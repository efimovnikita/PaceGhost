package com.paceghost.app.ui.heatmap.components

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.domain.heatmap.PresenceCluster
import com.paceghost.app.domain.model.WalkAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalkPresenceMarkersOverlayTest {

    @Test
    fun `markers shown only strictly below zoom 10`() {
        assertTrue(WalkPresenceMarkersOverlay.shouldShowMarkers(3.0))
        assertTrue(WalkPresenceMarkersOverlay.shouldShowMarkers(9.99))
        assertFalse(WalkPresenceMarkersOverlay.shouldShowMarkers(10.0))
        assertFalse(WalkPresenceMarkersOverlay.shouldShowMarkers(16.0))
    }

    @Test
    fun `hitTestCluster returns cluster when tap is within marker hit radius`() {
        val cluster = PresenceCluster(
            centerLat = 55.75,
            centerLon = 37.62,
            count = 1,
            walkIds = listOf(1L),
            minLat = 55.74,
            maxLat = 55.76,
            minLon = 37.61,
            maxLon = 37.63
        )
        val density = 2f
        val hit = WalkPresenceMarkersOverlay.hitTestCluster(
            clusters = listOf(cluster),
            tapX = 210f,
            tapY = 305f,
            density = density,
            projectToScreen = { 200f to 300f }
        )
        assertEquals(cluster, hit)
    }

    @Test
    fun `hitTestCluster returns null when tap is outside all markers`() {
        val cluster = PresenceCluster(
            centerLat = 55.75,
            centerLon = 37.62,
            count = 1,
            walkIds = listOf(1L),
            minLat = 55.74,
            maxLat = 55.76,
            minLon = 37.61,
            maxLon = 37.63
        )
        val hit = WalkPresenceMarkersOverlay.hitTestCluster(
            clusters = listOf(cluster),
            tapX = 400f,
            tapY = 500f,
            density = 2f,
            projectToScreen = { 200f to 300f }
        )
        assertNull(hit)
    }

    @Test
    fun `onSingleTapConfirmed triggers drill-down on cluster tap and ignores miss or high zoom`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = MapView(context)
        mapView.layout(0, 0, 1080, 1920)
        mapView.controller.setZoom(3.0)
        mapView.controller.setCenter(GeoPoint(55.75, 37.62))

        var tappedCluster: PresenceCluster? = null
        val overlay = WalkPresenceMarkersOverlay(density = 2f) { cluster ->
            tappedCluster = cluster
        }
        overlay.anchors = listOf(
            WalkAnchor(1L, 55.75, 37.62, 55.749, 55.751, 37.619, 37.621)
        )
        overlay.recluster(3.0)

        val centerPt = mapView.projection.toPixels(GeoPoint(55.75, 37.62), null)

        // Tap outside marker -> returns false (does not intercept panning)
        val missEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, centerPt.x + 300f, centerPt.y + 300f, 0)
        assertFalse(overlay.onSingleTapConfirmed(missEvent, mapView))
        assertNull(tappedCluster)
        missEvent.recycle()

        // Tap on marker -> returns true and invokes drill-down
        val hitEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, centerPt.x.toFloat(), centerPt.y.toFloat(), 0)
        assertTrue(overlay.onSingleTapConfirmed(hitEvent, mapView))
        assertNotNull(tappedCluster)
        assertEquals(listOf(1L), tappedCluster?.walkIds)

        // At zoom >= 10 -> does not intercept
        tappedCluster = null
        mapView.controller.setZoom(10.0)
        assertFalse(overlay.onSingleTapConfirmed(hitEvent, mapView))
        assertNull(tappedCluster)
        hitEvent.recycle()
        mapView.onDetach()
    }
}
