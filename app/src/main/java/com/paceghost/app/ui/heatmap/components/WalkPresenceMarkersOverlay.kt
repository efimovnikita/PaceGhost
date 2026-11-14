package com.paceghost.app.ui.heatmap.components

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import com.paceghost.app.domain.model.WalkAnchor
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Draws Walk Presence Markers (glowing CyanGhost dots at each Walk Anchor) strictly below
 * the heatmap visibility cutoff. At zoom >= [CUTOFF_ZOOM] nothing is drawn (heatmap only).
 */
class WalkPresenceMarkersOverlay(
    private val density: Float
) : Overlay() {

    var anchors: List<WalkAnchor> = emptyList()

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
    private val reusePoint = Point()

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || !shouldShowMarkers(mapView.zoomLevelDouble) || anchors.isEmpty()) return
        val projection = mapView.projection
        val radius = DOT_RADIUS_DP * density
        for (anchor in anchors) {
            projection.toPixels(GeoPoint(anchor.centerLat, anchor.centerLon), reusePoint)
            val x = reusePoint.x.toFloat()
            val y = reusePoint.y.toFloat()
            canvas.drawCircle(x, y, radius * 1.8f, glowPaint)
            canvas.drawCircle(x, y, radius, corePaint)
        }
    }

    companion object {
        const val CUTOFF_ZOOM = 10.0
        private const val DOT_RADIUS_DP = 5f
        private const val CYAN_GHOST = 0xFF00E5FF.toInt()

        /** Strict handoff: markers only below zoom 10, heatmap only at zoom >= 10. */
        fun shouldShowMarkers(zoom: Double): Boolean = zoom < CUTOFF_ZOOM
    }
}
