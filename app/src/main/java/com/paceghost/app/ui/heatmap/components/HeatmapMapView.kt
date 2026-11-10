package com.paceghost.app.ui.heatmap.components

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.domain.heatmap.HeatmapTileProvider
import com.paceghost.app.ui.theme.DarkBackground
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Controller allowing external Composable components (like floating action buttons)
 * to programmatically center or re-frame the heatmap.
 */
class HeatmapMapController {
    internal var recenterAction: (() -> Unit)? = null

    fun recenter() {
        recenterAction?.invoke()
    }
}

@Composable
fun rememberHeatmapMapController(): HeatmapMapController {
    return remember { HeatmapMapController() }
}

@Composable
fun HeatmapMapView(
    recentBounds: BoundingBox?,
    modifier: Modifier = Modifier,
    mapController: HeatmapMapController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density
    val paddingPx = (48 * density).toInt()

    val walkDao = remember { AppDatabase.getInstance(context).walkDao() }
    val heatmapTileProvider = remember {
        HeatmapTileProvider(
            context = context,
            walkDao = walkDao
        )
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setBackgroundColor(Color.BLACK)

            // Runtime Inverted Dark Theme Filter for OpenStreetMap Base Tiles
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f,  0f,  0f, 0f, 255f, // Invert Red
                 0f, -1f,  0f, 0f, 255f, // Invert Green
                 0f,  0f, -1f, 0f, 255f, // Invert Blue
                 0f,  0f,  0f, 1f,   0f  // Alpha
            ))
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(invertMatrix))
            overlayManager.tilesOverlay.loadingBackgroundColor = Color.BLACK
            overlayManager.tilesOverlay.loadingLineColor = Color.BLACK

            // Add Personal Heatmap tile overlay
            val heatmapOverlay = TilesOverlay(heatmapTileProvider, context).apply {
                loadingBackgroundColor = Color.TRANSPARENT
                loadingLineColor = Color.TRANSPARENT
            }
            overlays.add(heatmapOverlay)

            controller.setZoom(16.0)
        }
    }

    // Attach lifecycle observer to clean up background worker threads & MapView state
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDetach()
                    heatmapTileProvider.detach()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
            heatmapTileProvider.detach()
        }
    }

    fun centerOnRecentBounds(bbox: BoundingBox?, animated: Boolean) {
        if (bbox != null) {
            val latDelta = bbox.latNorth - bbox.latSouth
            val lonDelta = bbox.lonEast - bbox.lonWest
            if (latDelta < 0.0001 && lonDelta < 0.0001) {
                val center = GeoPoint((bbox.latNorth + bbox.latSouth) / 2.0, (bbox.lonEast + bbox.lonWest) / 2.0)
                if (animated) {
                    mapView.controller.animateTo(center, 16.5, 800L)
                } else {
                    mapView.controller.setCenter(center)
                    mapView.controller.setZoom(16.5)
                }
                return
            }
            try {
                mapView.zoomToBoundingBox(bbox, animated, paddingPx)
            } catch (_: Exception) {
                val center = GeoPoint((bbox.latNorth + bbox.latSouth) / 2.0, (bbox.lonEast + bbox.lonWest) / 2.0)
                mapView.controller.setCenter(center)
                mapView.controller.setZoom(15.0)
            }
        } else {
            // Default center if no recorded walks exist
            val defaultCenter = GeoPoint(55.751244, 37.618423) // Fallback default
            if (animated) {
                mapView.controller.animateTo(defaultCenter, 14.0, 800L)
            } else {
                mapView.controller.setCenter(defaultCenter)
                mapView.controller.setZoom(14.0)
            }
        }
    }

    LaunchedEffect(recentBounds) {
        centerOnRecentBounds(recentBounds, animated = false)
    }

    LaunchedEffect(mapController, recentBounds) {
        mapController?.recenterAction = {
            centerOnRecentBounds(recentBounds, animated = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
    }
}
