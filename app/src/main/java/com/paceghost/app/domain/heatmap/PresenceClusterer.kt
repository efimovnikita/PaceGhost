package com.paceghost.app.domain.heatmap

import com.paceghost.app.domain.model.WalkAnchor
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
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

/** Pure-JVM clustering of Walk Anchors into Presence Clusters below the heatmap cutoff. */
object PresenceClusterer {

    /** Single source of truth shared with the heatmap tile provider. */
    const val CUTOFF_ZOOM: Double = HeatmapTileProvider.MIN_HEATMAP_ZOOM.toDouble()
    const val CELL_SIZE_DP = 64f
    const val MIN_RADIUS_DP = 8f
    const val MAX_RADIUS_DP = 14f
    private const val RADIUS_COUNT_CAP = 100.0
    private const val BADGE_CAP = 99

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

    private fun project(lat: Double, lon: Double, world: Double): Pair<Double, Double> {
        val cl = lat.coerceIn(WebMercatorProjection.MIN_LATITUDE, WebMercatorProjection.MAX_LATITUDE)
        val clon = lon.coerceIn(-180.0, 180.0)
        val x = (clon + 180.0) / 360.0 * world
        val y = (1.0 - ln(tan(PI / 4.0 + Math.toRadians(cl) / 2.0)) / PI) / 2.0 * world
        return x to y
    }
}
