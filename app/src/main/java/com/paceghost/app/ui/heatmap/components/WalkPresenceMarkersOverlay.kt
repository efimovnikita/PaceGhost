package com.paceghost.app.ui.heatmap.components

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.paceghost.app.domain.heatmap.PresenceCluster
import com.paceghost.app.domain.heatmap.PresenceClusterer
import com.paceghost.app.domain.model.WalkAnchor
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws Presence Clusters (glowing CyanGhost dots with a walk-count badge) strictly below
 * the heatmap visibility cutoff. At zoom >= cutoff nothing is drawn (heatmap only).
 * Clusters are recomputed via [recluster] (called after zoom/scroll settles), not per frame.
 */
class WalkPresenceMarkersOverlay(
    private val density: Float
) : Overlay() {

    var anchors: List<WalkAnchor> = emptyList()

    private var clusters: List<PresenceCluster> = emptyList()

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CYAN_GHOST
        alpha = 110
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(6f * density, BlurMaskFilter.Blur.NORMAL)
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CYAN_GHOST
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt()
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
            val radius = PresenceClusterer.markerRadiusDp(cluster.count) * density
            canvas.drawCircle(x, y, radius * 1.5f, glowPaint)
            canvas.drawCircle(x, y, radius, corePaint)
            textPaint.textSize = radius * 1.0f
            val baseline = y - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(PresenceClusterer.badgeText(cluster.count), x, baseline, textPaint)
        }
    }

    companion object {
        const val CUTOFF_ZOOM = PresenceClusterer.CUTOFF_ZOOM
        private const val CYAN_GHOST = 0xFF00E5FF.toInt()

        /** Strict handoff: markers only below zoom 10, heatmap only at zoom >= 10. */
        fun shouldShowMarkers(zoom: Double): Boolean = zoom < CUTOFF_ZOOM
    }
}
