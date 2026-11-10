package com.paceghost.app.domain.heatmap

import android.graphics.Bitmap
import android.graphics.Paint
import com.paceghost.app.data.local.entity.TrackPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapTileRasterizerTest {

    private val rasterizer = HeatmapTileRasterizer()

    private fun createPoint(
        walkId: Long,
        id: Long,
        lat: Double,
        lon: Double
    ): TrackPointEntity = TrackPointEntity(
        id = id,
        walkId = walkId,
        latitude = lat,
        longitude = lon,
        altitude = 0.0,
        timestamp = 1000L + id * 1000,
        elapsedSeconds = id,
        cumulativeDistance = id * 10.0,
        speed = 1.4f,
        stepCountAtPoint = id.toInt()
    )

    @Test
    fun renderTile_emptyPoints_returnsNullWithoutAllocation() {
        val result = rasterizer.renderTile(
            points = emptyList(),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )
        assertNull("Empty points list must return null without allocating Bitmap", result)
    }

    @Test
    fun renderTile_singlePoint_returnsNullWithoutAllocation() {
        val singlePoint = createPoint(walkId = 1L, id = 1L, lat = 55.75, lon = 37.61)
        val result = rasterizer.renderTile(
            points = listOf(singlePoint),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )
        assertNull("Single point cannot form a line segment and should return null", result)
    }

    @Test
    fun renderTile_validSegment_allocates256x256Argb8888Bitmap() {
        val bounds = WebMercatorProjection.tileToBounds(zoom = 14, tileX = 9912, tileY = 5122)
        val midLon = (bounds.minLon + bounds.maxLon) / 2.0

        val p1 = createPoint(walkId = 1L, id = 1L, lat = bounds.maxLat - 0.001, lon = midLon)
        val p2 = createPoint(walkId = 1L, id = 2L, lat = bounds.minLat + 0.001, lon = midLon)

        val bitmap = rasterizer.renderTile(
            points = listOf(p1, p2),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )

        assertNotNull("Bitmap must be generated for valid segments", bitmap)
        bitmap!!
        assertEquals(256, bitmap.width)
        assertEquals(256, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun renderSegments_preGrouped_rendersBitmap() {
        val segment = listOf(
            Pair(55.752, 37.620),
            Pair(55.755, 37.625)
        )
        val bitmap = rasterizer.renderSegments(
            segments = listOf(segment),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)

        val emptyBitmap = rasterizer.renderSegments(emptyList(), 14, 9912, 5122)
        assertNull(emptyBitmap)
    }

    @Test
    fun renderTilePng_outputsValidPngBytes() {
        val bounds = WebMercatorProjection.tileToBounds(zoom = 14, tileX = 9912, tileY = 5122)
        val midLon = (bounds.minLon + bounds.maxLon) / 2.0
        val p1 = createPoint(walkId = 1L, id = 1L, lat = bounds.maxLat - 0.001, lon = midLon)
        val p2 = createPoint(walkId = 1L, id = 2L, lat = bounds.minLat + 0.001, lon = midLon)

        val pngBytes = rasterizer.renderTilePng(
            points = listOf(p1, p2),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )
        assertNotNull(pngBytes)
        pngBytes!!
        // PNG magic number: 0x89, 'P', 'N', 'G'
        assertEquals(0x89.toByte(), pngBytes[0])
        assertEquals('P'.code.toByte(), pngBytes[1])
        assertEquals('N'.code.toByte(), pngBytes[2])
        assertEquals('G'.code.toByte(), pngBytes[3])

        // Empty points returns null
        val emptyPng = rasterizer.renderTilePng(
            points = emptyList(),
            zoom = 14,
            tileX = 9912,
            tileY = 5122
        )
        assertNull(emptyPng)
    }

    @Test
    fun emptyTransparentPng_isValidPng() {
        val emptyTile = HeatmapTileRasterizer.EMPTY_TRANSPARENT_PNG
        assertTrue(emptyTile.isNotEmpty())
        assertEquals(0x89.toByte(), emptyTile[0])
        assertEquals('P'.code.toByte(), emptyTile[1])
        assertEquals('N'.code.toByte(), emptyTile[2])
        assertEquals('G'.code.toByte(), emptyTile[3])
    }

    @Test
    fun paintConfiguration_matchesSoftDensitySpecs() {
        val auraPaint = rasterizer.createAuraPaint()
        val corePaint = rasterizer.createCorePaint()

        assertEquals(56f, auraPaint.strokeWidth, 0.01f)
        assertEquals(HeatmapTileRasterizer.AURA_COLOR_DEFAULT, auraPaint.color)
        assertEquals(Paint.Cap.ROUND, auraPaint.strokeCap)
        assertEquals(Paint.Join.ROUND, auraPaint.strokeJoin)
        assertEquals(Paint.Style.STROKE, auraPaint.style)
        assertNotNull(auraPaint.xfermode)

        assertEquals(12f, corePaint.strokeWidth, 0.01f)
        assertEquals(HeatmapTileRasterizer.CORE_COLOR_DEFAULT, corePaint.color)
        assertEquals(Paint.Cap.ROUND, corePaint.strokeCap)
        assertEquals(Paint.Join.ROUND, corePaint.strokeJoin)
        assertEquals(Paint.Style.STROKE, corePaint.style)
        assertNotNull(corePaint.xfermode)

        // Verify multi-tier soft density diffusion tiers
        assertEquals(5, rasterizer.densityTiers.size)
        assertEquals(56f, rasterizer.densityTiers[0].strokeWidth, 0.01f)
        assertEquals(4f, rasterizer.densityTiers.last().strokeWidth, 0.01f)
    }

    @Test
    fun extractSegments_groupsByWalkAndSplitsOnGaps() {
        val p1 = createPoint(walkId = 1L, id = 1L, lat = 55.750, lon = 37.610)
        val p2 = createPoint(walkId = 1L, id = 2L, lat = 55.751, lon = 37.611)

        // Point with large ID jump (gap in track)
        val p3 = createPoint(walkId = 1L, id = 10L, lat = 55.752, lon = 37.612)
        val p4 = createPoint(walkId = 1L, id = 11L, lat = 55.753, lon = 37.613)

        // Point from different walk
        val p5 = createPoint(walkId = 2L, id = 1L, lat = 55.750, lon = 37.610)
        val p6 = createPoint(walkId = 2L, id = 2L, lat = 55.751, lon = 37.611)

        val segments = rasterizer.extractSegments(listOf(p1, p2, p3, p4, p5, p6))
        assertEquals(3, segments.size)

        // Segment 1: walk 1, points 1 and 2
        assertEquals(listOf(p1, p2), segments[0])

        // Segment 2: walk 1, points 3 and 4
        assertEquals(listOf(p3, p4), segments[1])

        // Segment 3: walk 2, points 5 and 6
        assertEquals(listOf(p5, p6), segments[2])
    }

    @Test
    fun additiveBlending_multipleRoutesSaturateToWhiteHotNeon() {
        val aura = HeatmapTileRasterizer.AURA_COLOR_DEFAULT // 0x3300E5FF
        val core = HeatmapTileRasterizer.CORE_COLOR_DEFAULT // 0x9939FF14

        // Single route passes through (aura + core)
        val singleRouteColor = HeatmapTileRasterizer.blendAdditive(listOf(aura, core))
        val alpha1 = (singleRouteColor ushr 24) and 0xFF
        val r1 = (singleRouteColor ushr 16) and 0xFF
        val g1 = (singleRouteColor ushr 8) and 0xFF
        val b1 = singleRouteColor and 0xFF

        assertTrue(alpha1 > 0)
        assertTrue(g1 > 0)
        assertTrue(b1 > 0)

        // Multiple overlapping routes (e.g. 5 passes)
        val multiPassColors = mutableListOf<Int>()
        repeat(5) {
            multiPassColors.add(aura)
            multiPassColors.add(core)
        }
        val whiteHotNeon = HeatmapTileRasterizer.blendAdditive(multiPassColors)
        val alphaMulti = (whiteHotNeon ushr 24) and 0xFF
        val rMulti = (whiteHotNeon ushr 16) and 0xFF
        val gMulti = (whiteHotNeon ushr 8) and 0xFF
        val bMulti = whiteHotNeon and 0xFF

        // Alpha and Green saturate to 255 (maximum opacity and intense luminescence)
        assertEquals(255, alphaMulti)
        assertEquals(255, gMulti)

        // Red and Blue intensify additively, pushing towards pure white (255, 255, 255)
        assertTrue("Multi-route red ($rMulti) must be > single route ($r1)", rMulti > r1)
        assertEquals(255, bMulti)
    }

    @Test
    fun getDensityTiersForZoom_zoom16_returnsUnscaledDefaultTiers() {
        val tiers = rasterizer.getDensityTiersForZoom(16)
        assertEquals(5, tiers.size)
        assertEquals(56f, tiers.first().strokeWidth, 0.001f)
        assertEquals(36f, tiers[1].strokeWidth, 0.001f)
        assertEquals(22f, tiers[2].strokeWidth, 0.001f)
        assertEquals(12f, tiers[3].strokeWidth, 0.001f)
        assertEquals(4f, tiers.last().strokeWidth, 0.001f)
    }

    @Test
    fun getDensityTiersForZoom_zoom18_clampsScaleToOne() {
        val tiers = rasterizer.getDensityTiersForZoom(18)
        assertEquals(5, tiers.size)
        // Zoom >= 16 must clamp scale factor k = 1.0, not blooming beyond 56f
        assertEquals(56f, tiers.first().strokeWidth, 0.001f)
        assertEquals(4f, tiers.last().strokeWidth, 0.001f)
    }

    @Test
    fun getDensityTiersForZoom_zoom12_scalesExponentially() {
        val tiers = rasterizer.getDensityTiersForZoom(12)
        assertEquals(5, tiers.size)
        // k = 2^((12 - 16) * 0.6) = 2^(-2.4) ~= 0.18946457
        // Aura = 56 * 0.18946457 ~= 10.610f
        // Core = max(4 * 0.18946457, 2.0f) = 2.0f
        assertEquals(10.610f, tiers.first().strokeWidth, 0.05f)
        assertEquals(2.0f, tiers.last().strokeWidth, 0.001f)
        assertTrue(tiers[0].strokeWidth > tiers[1].strokeWidth)
        assertTrue(tiers[1].strokeWidth > tiers[2].strokeWidth)
        assertTrue(tiers[2].strokeWidth > tiers[3].strokeWidth)
        assertTrue(tiers[3].strokeWidth > tiers[4].strokeWidth)
    }

    @Test
    fun getDensityTiersForZoom_zoom10_clampsCoreFloorToTwoAndAuraAboveMin() {
        val tiers = rasterizer.getDensityTiersForZoom(10)
        assertEquals(5, tiers.size)
        // k = 2^((10 - 16) * 0.6) = 2^(-3.6) ~= 0.082469275
        // Aura = max(56 * 0.082469275, 4.5f) = max(4.618f, 4.5f) ~= 4.618f
        // Core = max(4 * 0.082469275, 2.0f) = 2.0f (floor hit)
        assertEquals(4.618f, tiers.first().strokeWidth, 0.05f)
        assertTrue("Aura must be >= 4.5f", tiers.first().strokeWidth >= 4.5f)
        assertEquals(2.0f, tiers.last().strokeWidth, 0.001f)
        // Verify monotonic decreasing order
        for (i in 0 until tiers.size - 1) {
            assertTrue("Tier $i (${tiers[i].strokeWidth}) must be >= Tier ${i+1} (${tiers[i+1].strokeWidth})",
                tiers[i].strokeWidth >= tiers[i + 1].strokeWidth)
        }
    }

    @Test
    fun getDensityTiersForZoom_zoom6_enforcesFloorsOnAllTiers() {
        val tiers = rasterizer.getDensityTiersForZoom(6)
        assertEquals(5, tiers.size)
        // Deep zoom out: aura should hit floor 4.5f and core hits 2.0f
        assertEquals(HeatmapTileRasterizer.MIN_AURA_STROKE_WIDTH, tiers.first().strokeWidth, 0.001f)
        assertEquals(HeatmapTileRasterizer.MIN_CORE_STROKE_WIDTH, tiers.last().strokeWidth, 0.001f)
    }

    @Test
    fun renderTile_zoom10_rendersSuccessfully() {
        val bounds = WebMercatorProjection.tileToBounds(zoom = 10, tileX = 619, tileY = 320)
        val midLon = (bounds.minLon + bounds.maxLon) / 2.0
        val p1 = createPoint(walkId = 1L, id = 1L, lat = bounds.maxLat - 0.01, lon = midLon)
        val p2 = createPoint(walkId = 1L, id = 2L, lat = bounds.minLat + 0.01, lon = midLon)

        val bitmap = rasterizer.renderTile(
            points = listOf(p1, p2),
            zoom = 10,
            tileX = 619,
            tileY = 320
        )
        assertNotNull(bitmap)
        assertEquals(256, bitmap!!.width)
        assertEquals(256, bitmap.height)
    }
}
