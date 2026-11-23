package com.paceghost.app.ui.common

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class MapViewDefaultsTest {

    @Test
    fun `applyPaceGhostDefaults hides built-in zoom controls even after activate`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = MapView(context)
        mapView.layout(0, 0, 1080, 1920)

        mapView.applyPaceGhostDefaults()
        mapView.zoomController.activate()

        val tapEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 540f, 1800f, 0)
        try {
            assertFalse(mapView.zoomController.isTouched(tapEvent))
        } finally {
            tapEvent.recycle()
        }
    }

    @Test
    fun `applyPaceGhostDefaults configures tile source, dpi scaling, and disables repetition`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = MapView(context)

        mapView.applyPaceGhostDefaults()

        assertEquals(TileSourceFactory.MAPNIK, mapView.tileProvider.tileSource)
        assertTrue(mapView.isTilesScaledToDpi)
        assertFalse(mapView.isHorizontalMapRepetitionEnabled)
        assertFalse(mapView.isVerticalMapRepetitionEnabled)
    }

    @Test
    fun `applyPaceGhostDefaults keeps multi-touch enabled and programmatic zoom working`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = MapView(context)
        mapView.layout(0, 0, 1080, 1920)

        mapView.applyPaceGhostDefaults()
        mapView.controller.setZoom(14.0)
        val initialZoom = mapView.zoomLevelDouble

        assertTrue(mapView.controller.zoomIn())
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
        val zoomedIn = mapView.zoomLevelDouble
        assertTrue("Expected zoomIn() to increase zoom level", zoomedIn > initialZoom)

        assertTrue(mapView.controller.zoomOut())
        ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
        val zoomedOut = mapView.zoomLevelDouble
        assertTrue("Expected zoomOut() to decrease zoom level", zoomedOut < zoomedIn)
    }

    @Test
    fun `applyPaceGhostDefaults enables multi-touch pointer handling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val unconfigured = MapView(context).apply { layout(0, 0, 1080, 1920) }
        val configured = MapView(context).apply {
            layout(0, 0, 1080, 1920)
            applyPaceGhostDefaults()
        }

        val downTime = 1000L
        val p0Props = arrayOf(MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER })
        val p0Coords = arrayOf(MotionEvent.PointerCoords().apply { x = 500f; y = 960f })
        val bothProps = arrayOf(
            MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_FINGER },
            MotionEvent.PointerProperties().apply { id = 1; toolType = MotionEvent.TOOL_TYPE_FINGER }
        )
        val bothCoords = arrayOf(
            MotionEvent.PointerCoords().apply { x = 500f; y = 960f },
            MotionEvent.PointerCoords().apply { x = 580f; y = 960f }
        )

        val down1 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1, p0Props, p0Coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        val ptrDown1 = MotionEvent.obtain(
            downTime, downTime + 16,
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            2, bothProps, bothCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )
        val down2 = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 1, p0Props, p0Coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        val ptrDown2 = MotionEvent.obtain(
            downTime, downTime + 16,
            MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            2, bothProps, bothCoords, 0, 0, 1f, 1f, 0, 0, 0, 0
        )

        try {
            unconfigured.dispatchTouchEvent(down1)
            assertFalse("Unconfigured MapView should not handle multi-touch", unconfigured.dispatchTouchEvent(ptrDown1))

            configured.dispatchTouchEvent(down2)
            assertTrue("Configured MapView should handle multi-touch", configured.dispatchTouchEvent(ptrDown2))
        } finally {
            down1.recycle()
            ptrDown1.recycle()
            down2.recycle()
            ptrDown2.recycle()
        }
    }
}
