package com.paceghost.app.domain.heatmap

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.domain.engine.GeoMath
import java.io.ByteArrayOutputStream

/**
 * Two-pass glowing tile rasterizer producing transparent PNG tiles in the Cyber Neon Ramp spectrum.
 *
 * Pass 1: Wide low-alpha aura (CyanGhost tinted).
 * Pass 2: Thin saturated core (NeonGreenRunner tinted).
 * Blending: [PorterDuff.Mode.ADD] additive blending so overlapping routes intensify into white-hot neon.
 */
class HeatmapTileRasterizer(
    val auraWidthPx: Float = AURA_WIDTH_DEFAULT,
    val coreWidthPx: Float = CORE_WIDTH_DEFAULT,
    val auraColor: Int = AURA_COLOR_DEFAULT,
    val coreColor: Int = CORE_COLOR_DEFAULT
) {

    /**
     * Renders route segments for Slippy Map tile `(zoom, tileX, tileY)` into an [ARGB_8888] [Bitmap].
     *
     * Returns `null` if [points] is empty or no valid multi-point segments can be formed,
     * avoiding any memory allocations for empty bounding boxes.
     */
    fun renderTile(
        points: List<TrackPointEntity>,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int = WebMercatorProjection.DEFAULT_TILE_SIZE
    ): Bitmap? {
        if (points.size < 2) return null

        val segments = extractSegments(points)
        if (segments.isEmpty()) return null

        return renderSegmentsInternal(segments, zoom, tileX, tileY, tileSize)
    }

    /**
     * Renders pre-grouped route segments into an [ARGB_8888] [Bitmap].
     * Returns `null` if [segments] is empty.
     */
    fun renderSegments(
        segments: List<List<Pair<Double, Double>>>,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int = WebMercatorProjection.DEFAULT_TILE_SIZE
    ): Bitmap? {
        val validSegments = segments.filter { it.size >= 2 }
        if (validSegments.isEmpty()) return null

        val bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paths = validSegments.map { segment ->
            Path().apply {
                val first = segment.first()
                val (x0, y0) = WebMercatorProjection.projectToTilePixel(
                    lat = first.first,
                    lon = first.second,
                    zoom = zoom,
                    tileX = tileX,
                    tileY = tileY,
                    tileSize = tileSize
                )
                moveTo(x0, y0)
                for (i in 1 until segment.size) {
                    val pt = segment[i]
                    val (x, y) = WebMercatorProjection.projectToTilePixel(
                        lat = pt.first,
                        lon = pt.second,
                        zoom = zoom,
                        tileX = tileX,
                        tileY = tileY,
                        tileSize = tileSize
                    )
                    lineTo(x, y)
                }
            }
        }

        drawTwoPassGlow(canvas, paths)
        return bitmap
    }

    /**
     * Renders route segments for tile `(zoom, tileX, tileY)` and compresses into PNG bytes.
     * Returns `null` if [points] is empty or has no drawable segments.
     */
    fun renderTilePng(
        points: List<TrackPointEntity>,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int = WebMercatorProjection.DEFAULT_TILE_SIZE
    ): ByteArray? {
        val bitmap = renderTile(points, zoom, tileX, tileY, tileSize) ?: return null
        val stream = ByteArrayOutputStream()
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    internal fun createAuraPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = auraWidthPx
        color = auraColor
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    internal fun createCorePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = coreWidthPx
        color = coreColor
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }

    internal fun drawTwoPassGlow(canvas: Canvas, paths: List<Path>) {
        val auraPaint = createAuraPaint()
        val corePaint = createCorePaint()

        // Pass 1: Wide aura ambient glow
        for (path in paths) {
            canvas.drawPath(path, auraPaint)
        }

        // Pass 2: Sharp saturated neon core
        for (path in paths) {
            canvas.drawPath(path, corePaint)
        }
    }

    internal fun extractSegments(points: List<TrackPointEntity>): List<List<TrackPointEntity>> {
        val segments = mutableListOf<List<TrackPointEntity>>()
        var currentSegment = mutableListOf<TrackPointEntity>()

        for (i in points.indices) {
            val point = points[i]
            if (currentSegment.isEmpty()) {
                currentSegment.add(point)
                continue
            }

            val prev = currentSegment.last()
            val isSameWalk = point.walkId == prev.walkId
            val distance = GeoMath.haversineDistance(prev.latitude, prev.longitude, point.latitude, point.longitude)
            val isConsecutive = if (prev.id > 0L && point.id > 0L) {
                point.id == prev.id + 1
            } else {
                distance < MAX_SEGMENT_GAP_METERS
            }
            val isContinuous = isSameWalk && isConsecutive

            if (isContinuous) {
                currentSegment.add(point)
            } else {
                if (currentSegment.size >= 2) {
                    segments.add(currentSegment)
                }
                currentSegment = mutableListOf(point)
            }
        }

        if (currentSegment.size >= 2) {
            segments.add(currentSegment)
        }

        return segments
    }

    private fun renderSegmentsInternal(
        segments: List<List<TrackPointEntity>>,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paths = segments.map { segment ->
            Path().apply {
                val first = segment.first()
                val (x0, y0) = WebMercatorProjection.projectToTilePixel(
                    lat = first.latitude,
                    lon = first.longitude,
                    zoom = zoom,
                    tileX = tileX,
                    tileY = tileY,
                    tileSize = tileSize
                )
                moveTo(x0, y0)
                for (i in 1 until segment.size) {
                    val pt = segment[i]
                    val (x, y) = WebMercatorProjection.projectToTilePixel(
                        lat = pt.latitude,
                        lon = pt.longitude,
                        zoom = zoom,
                        tileX = tileX,
                        tileY = tileY,
                        tileSize = tileSize
                    )
                    lineTo(x, y)
                }
            }
        }

        drawTwoPassGlow(canvas, paths)
        return bitmap
    }

    companion object {
        const val AURA_WIDTH_DEFAULT = 8f
        const val CORE_WIDTH_DEFAULT = 2f

        // CyanGhost (#00E5FF) at 20% alpha -> 0x3300E5FF
        const val AURA_COLOR_DEFAULT = 0x3300E5FF.toInt()

        // NeonGreenRunner (#39FF14) at 60% alpha -> 0x9939FF14
        const val CORE_COLOR_DEFAULT = 0x9939FF14.toInt()

        // Maximum distance in meters between consecutive points before splitting into separate polylines
        const val MAX_SEGMENT_GAP_METERS = 500.0

        /**
         * Simulates additive color blending of multiple overlay colors (ARGB) to model
         * how multiple overlapping routes blend into white-hot neon.
         */
        fun blendAdditive(colors: List<Int>): Int {
            var totalA = 0
            var totalR = 0
            var totalG = 0
            var totalB = 0
            for (c in colors) {
                totalA += (c ushr 24) and 0xFF
                totalR += (c ushr 16) and 0xFF
                totalG += (c ushr 8) and 0xFF
                totalB += c and 0xFF
            }
            val a = totalA.coerceAtMost(255)
            val r = totalR.coerceAtMost(255)
            val g = totalG.coerceAtMost(255)
            val b = totalB.coerceAtMost(255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        /**
         * Pre-encoded 1x1 transparent PNG byte array for immediate use without memory allocation.
         */
        val EMPTY_TRANSPARENT_PNG: ByteArray = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4e.toByte(), 0x47.toByte(), 0x0d.toByte(), 0x0a.toByte(), 0x1a.toByte(), 0x0a.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0d.toByte(), 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1f.toByte(), 0x15.toByte(), 0xc4.toByte(),
            0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0a.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
            0x54.toByte(), 0x78.toByte(), 0x9c.toByte(), 0x63.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0x0d.toByte(), 0x0a.toByte(), 0x2d.toByte(), 0xb4.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(), 0x4e.toByte(), 0x44.toByte(), 0xae.toByte(),
            0x42.toByte(), 0x60.toByte(), 0x82.toByte()
        )
    }
}
