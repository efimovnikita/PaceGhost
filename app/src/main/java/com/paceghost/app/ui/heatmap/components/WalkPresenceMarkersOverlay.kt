package com.paceghost.app.ui.heatmap.components

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import com.paceghost.app.domain.heatmap.PresenceCluster
import com.paceghost.app.domain.heatmap.PresenceClusterer
import com.paceghost.app.domain.model.WalkAnchor
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot

/**
 * Computed Two-Pass Glow visual specification for a single [PresenceCluster] marker.
 */
data class MarkerVisualSpec(
    val badgeText: String,
    val coreRadiusPx: Float,
    val innerAuraRadiusPx: Float,
    val outerAuraRadiusPx: Float,
    val coreColor: Int,
    val innerAuraColor: Int,
    val outerAuraColor: Int,
    val textColor: Int,
    val textSizePx: Float
)

/**
 * Draws Presence Clusters (glowing CyanGhost dots with a walk-count badge) strictly below
 * the heatmap visibility cutoff. At zoom >= cutoff nothing is drawn (heatmap only).
 * Clusters are recomputed via [recluster] (called after zoom/scroll settles), not per frame.
 */
class WalkPresenceMarkersOverlay(
    private val density: Float,
    private val onClusterTap: ((PresenceCluster) -> Unit)? = null
) : Overlay() {

    var anchors: List<WalkAnchor> = emptyList()

    private var clusters: List<PresenceCluster> = emptyList()

    private val outerAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OUTER_AURA_COLOR
        style = Paint.Style.FILL
    }
    private val innerAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = INNER_AURA_COLOR
        style = Paint.Style.FILL
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CYAN_GHOST
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT_COLOR
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val reusePoint = Point()

    fun recluster(zoom: Double) {
        clusters = PresenceClusterer.cluster(anchors, zoom, PresenceClusterer.CELL_SIZE_DP * density)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !shouldShowMarkers(mapView.zoomLevelDouble) || clusters.isEmpty()) return
        val projection = mapView.projection
        for (cluster in clusters) {
            projection.toPixels(GeoPoint(cluster.centerLat, cluster.centerLon), reusePoint)
            val x = reusePoint.x.toFloat()
            val y = reusePoint.y.toFloat()
            val spec = computeMarkerVisualSpec(cluster.count, density)
            // Pass 1: Two-tier semi-transparent CyanGhost aura
            canvas.drawCircle(x, y, spec.outerAuraRadiusPx, outerAuraPaint)
            canvas.drawCircle(x, y, spec.innerAuraRadiusPx, innerAuraPaint)
            // Pass 2: Saturated CyanGhost core + centered bold count badge
            canvas.drawCircle(x, y, spec.coreRadiusPx, corePaint)
            textPaint.textSize = spec.textSizePx
            val baseline = y - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(spec.badgeText, x, baseline, textPaint)
        }
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        if (!shouldShowMarkers(mapView.zoomLevelDouble) || clusters.isEmpty()) return false
        val projection = mapView.projection
        val hit = hitTestCluster(
            clusters = clusters,
            tapX = e.x,
            tapY = e.y,
            density = density,
            projectToScreen = { cluster ->
                projection.toPixels(GeoPoint(cluster.centerLat, cluster.centerLon), reusePoint)
                reusePoint.x.toFloat() to reusePoint.y.toFloat()
            }
        ) ?: return false
        if (onClusterTap != null) {
            onClusterTap.invoke(hit)
        } else {
            val target = PresenceClusterer.drillDownTarget(
                cluster = hit,
                viewportWidthPx = mapView.width.takeIf { it > 0 } ?: 1080,
                viewportHeightPx = mapView.height.takeIf { it > 0 } ?: 1920,
                paddingPx = (48f * density).toInt()
            )
            mapView.controller.animateTo(
                GeoPoint(target.centerLat, target.centerLon),
                target.targetZoom,
                800L
            )
        }
        return true
    }

    companion object {
        const val CUTOFF_ZOOM = PresenceClusterer.CUTOFF_ZOOM
        const val CYAN_GHOST = 0xFF00E5FF.toInt()
        const val INNER_AURA_COLOR = 0x5500E5FF.toInt()
        const val OUTER_AURA_COLOR = 0x2600E5FF.toInt()
        const val TEXT_COLOR = 0xFF000000.toInt()
        private const val OUTER_AURA_MULTIPLIER = 2.0f
        private const val INNER_AURA_MULTIPLIER = 1.4f
        private const val HIT_RADIUS_MULTIPLIER = 1.5f
        private const val MIN_HIT_RADIUS_DP = 24f

        /** Strict handoff: markers only below zoom 10, heatmap only at zoom >= 10. */
        fun shouldShowMarkers(zoom: Double): Boolean = zoom < CUTOFF_ZOOM

        /**
         * Computes Two-Pass Glow radii, colors, and adaptive text sizing so count badges
         * ("1".."9", "10".."99", "99+") stay crisp and centered inside the core across all sizes.
         */
        fun computeMarkerVisualSpec(count: Int, density: Float): MarkerVisualSpec {
            val badge = PresenceClusterer.badgeText(count)
            val coreRadiusPx = PresenceClusterer.markerRadiusDp(count) * density
            val textScale = when {
                badge.length >= 3 -> 0.78f
                badge.length == 2 -> 0.90f
                else -> 1.05f
            }
            return MarkerVisualSpec(
                badgeText = badge,
                coreRadiusPx = coreRadiusPx,
                innerAuraRadiusPx = coreRadiusPx * INNER_AURA_MULTIPLIER,
                outerAuraRadiusPx = coreRadiusPx * OUTER_AURA_MULTIPLIER,
                coreColor = CYAN_GHOST,
                innerAuraColor = INNER_AURA_COLOR,
                outerAuraColor = OUTER_AURA_COLOR,
                textColor = TEXT_COLOR,
                textSizePx = coreRadiusPx * textScale
            )
        }

        /**
         * Returns the closest [PresenceCluster] whose marker touch target contains ([tapX], [tapY]),
         * or `null` if the tap misses all markers so normal map panning/gestures are not intercepted.
         */
        fun hitTestCluster(
            clusters: List<PresenceCluster>,
            tapX: Float,
            tapY: Float,
            density: Float,
            projectToScreen: (PresenceCluster) -> Pair<Float, Float>
        ): PresenceCluster? {
            var bestCluster: PresenceCluster? = null
            var bestDist = Float.MAX_VALUE
            for (cluster in clusters) {
                val (cx, cy) = projectToScreen(cluster)
                val visualGlowRadius = PresenceClusterer.markerRadiusDp(cluster.count) * HIT_RADIUS_MULTIPLIER * density
                val hitRadius = maxOf(visualGlowRadius, MIN_HIT_RADIUS_DP * density)
                val dist = hypot(tapX - cx, tapY - cy)
                if (dist <= hitRadius && dist < bestDist) {
                    bestDist = dist
                    bestCluster = cluster
                }
            }
            return bestCluster
        }
    }
}
