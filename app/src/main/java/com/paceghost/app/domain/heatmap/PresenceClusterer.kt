package com.paceghost.app.domain.heatmap

import com.paceghost.app.domain.model.WalkAnchor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

/** Presence Cluster: walks grouped by a screen-space grid cell. */
data class PresenceCluster(
    val centerLat: Double,
    val centerLon: Double,
    val count: Int,
    val walkIds: List<Long>,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

/** Target bounds and clamped zoom level for a Cluster Drill-Down. */
data class DrillDownTarget(
    val centerLat: Double,
    val centerLon: Double,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double,
    val targetZoom: Double
)

/** Pure-JVM clustering of Walk Anchors into Presence Clusters below the heatmap cutoff. */
object PresenceClusterer {

    /** Single source of truth shared with the heatmap tile provider. */
    const val CUTOFF_ZOOM: Double = HeatmapTileProvider.MIN_HEATMAP_ZOOM.toDouble()
    const val MAX_DRILL_DOWN_ZOOM: Double = 16.0
    const val CELL_SIZE_DP = 64f
    const val MIN_RADIUS_DP = 8f
    const val MAX_RADIUS_DP = 14f
    private const val RADIUS_COUNT_CAP = 100.0
    private const val BADGE_CAP = 99
    private const val DEFAULT_VIEWPORT_WIDTH_PX = 1080
    private const val DEFAULT_VIEWPORT_HEIGHT_PX = 1920
    private const val DEFAULT_PADDING_PX = 96

    fun cluster(anchors: List<WalkAnchor>, zoom: Double, cellSizePx: Float): List<PresenceCluster> {
        if (anchors.isEmpty() || zoom >= CUTOFF_ZOOM || cellSizePx <= 0f) return emptyList()
        val world = WebMercatorProjection.DEFAULT_TILE_SIZE * 2.0.pow(zoom)
        val cells = LinkedHashMap<Pair<Long, Long>, MutableList<WalkAnchor>>()
        for (a in anchors) {
            val (x, y) = project(a.centerLat, a.centerLon, world)
            val key = floor(x / cellSizePx).toLong() to floor(y / cellSizePx).toLong()
            cells.getOrPut(key) { mutableListOf() }.add(a)
        }
        return cells.values.map { group ->
            PresenceCluster(
                centerLat = group.map { it.centerLat }.average(),
                centerLon = group.map { it.centerLon }.average(),
                count = group.size,
                walkIds = group.map { it.walkId },
                minLat = group.minOf { it.minLat },
                maxLat = group.maxOf { it.maxLat },
                minLon = group.minOf { it.minLon },
                maxLon = group.maxOf { it.maxLon }
            )
        }
    }

    /** `8 + 6 * ln(count)/ln(cap)`, clamped to [8, 14] dp. */
    fun markerRadiusDp(count: Int): Float {
        if (count <= 1) return MIN_RADIUS_DP
        val r = MIN_RADIUS_DP + (MAX_RADIUS_DP - MIN_RADIUS_DP) * (ln(count.toDouble()) / ln(RADIUS_COUNT_CAP)).toFloat()
        return r.coerceIn(MIN_RADIUS_DP, MAX_RADIUS_DP)
    }

    fun badgeText(count: Int): String = if (count > BADGE_CAP) "$BADGE_CAP+" else count.toString()

    /**
     * Computes the drill-down target for [cluster]: the union of its walks' bounds and the
     * Web Mercator zoom that fits those bounds inside the padded viewport, clamped to [10.0, 16.0].
     */
    fun drillDownTarget(
        cluster: PresenceCluster,
        viewportWidthPx: Int = DEFAULT_VIEWPORT_WIDTH_PX,
        viewportHeightPx: Int = DEFAULT_VIEWPORT_HEIGHT_PX,
        paddingPx: Int = DEFAULT_PADDING_PX
    ): DrillDownTarget {
        val availW = max(1, viewportWidthPx - 2 * paddingPx).toDouble()
        val availH = max(1, viewportHeightPx - 2 * paddingPx).toDouble()
        val tileSize = WebMercatorProjection.DEFAULT_TILE_SIZE.toDouble()
        val (x1, y1) = project(cluster.maxLat, cluster.minLon, tileSize)
        val (x2, y2) = project(cluster.minLat, cluster.maxLon, tileSize)
        val spanX = abs(x2 - x1)
        val spanY = abs(y2 - y1)
        val rawZoom = if (spanX < 1e-6 && spanY < 1e-6) {
            MAX_DRILL_DOWN_ZOOM
        } else {
            val zoomX = if (spanX > 1e-9) ln(availW / spanX) / ln(2.0) else MAX_DRILL_DOWN_ZOOM
            val zoomY = if (spanY > 1e-9) ln(availH / spanY) / ln(2.0) else MAX_DRILL_DOWN_ZOOM
            min(zoomX, zoomY)
        }
        return DrillDownTarget(
            centerLat = (cluster.minLat + cluster.maxLat) / 2.0,
            centerLon = (cluster.minLon + cluster.maxLon) / 2.0,
            minLat = cluster.minLat,
            maxLat = cluster.maxLat,
            minLon = cluster.minLon,
            maxLon = cluster.maxLon,
            targetZoom = rawZoom.coerceIn(CUTOFF_ZOOM, MAX_DRILL_DOWN_ZOOM)
        )
    }

    private fun project(lat: Double, lon: Double, world: Double): Pair<Double, Double> {
        val cl = lat.coerceIn(WebMercatorProjection.MIN_LATITUDE, WebMercatorProjection.MAX_LATITUDE)
        val clon = lon.coerceIn(-180.0, 180.0)
        val x = (clon + 180.0) / 360.0 * world
        val y = (1.0 - ln(tan(PI / 4.0 + Math.toRadians(cl) / 2.0)) / PI) / 2.0 * world
        return x to y
    }
}
