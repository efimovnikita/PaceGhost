package com.paceghost.app.ui.preview.components

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.TextSecondary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class TrackPreviewMapController {
    internal var fitRouteAction: (() -> Unit)? = null

    fun fitRoute() {
        fitRouteAction?.invoke()
    }
}

@Composable
fun rememberTrackPreviewMapController(): TrackPreviewMapController {
    return remember { TrackPreviewMapController() }
}

@Composable
fun TrackPreviewMapView(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier,
    fitRouteTrigger: Int = 0,
    previewController: TrackPreviewMapController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density
    val paddingPx = (56 * density).toInt()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(16.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDetach()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val routePolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#00FF66")
            outlinePaint.strokeWidth = density * 7f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            mapView.overlays.add(this)
        }
    }

    val startMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = android.graphics.Color.parseColor("#00FF66"),
                strokeColor = android.graphics.Color.BLACK,
                sizePx = (24 * density).toInt(),
                strokeWidthPx = (2.5f * density).toInt()
            )
            title = "Start"
            mapView.overlays.add(this)
        }
    }

    val finishMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = android.graphics.Color.parseColor("#00E5FF"),
                strokeColor = android.graphics.Color.BLACK,
                sizePx = (24 * density).toInt(),
                strokeWidthPx = (2.5f * density).toInt()
            )
            title = "Finish"
            mapView.overlays.add(this)
        }
    }

    previewController?.fitRouteAction = {
        fitTrack(mapView, trackPoints, paddingPx = paddingPx, animated = true)
    }

    LaunchedEffect(trackPoints, fitRouteTrigger) {
        if (trackPoints.isNotEmpty()) {
            mapView.post {
                fitTrack(mapView, trackPoints, paddingPx = paddingPx, animated = fitRouteTrigger > 0)
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                if (trackPoints.isNotEmpty()) {
                    val geoPoints = trackPoints.map { GeoPoint(it.lat, it.lon) }

                    if (geoPoints.size >= 2) {
                        routePolyline.setPoints(geoPoints)
                        routePolyline.isEnabled = true
                    } else {
                        routePolyline.setPoints(emptyList())
                        routePolyline.isEnabled = false
                    }

                    startMarker.position = geoPoints.first()
                    startMarker.isEnabled = true

                    if (geoPoints.size >= 2) {
                        finishMarker.position = geoPoints.last()
                        finishMarker.isEnabled = true
                    } else {
                        finishMarker.isEnabled = false
                    }
                } else {
                    routePolyline.setPoints(emptyList())
                    routePolyline.isEnabled = false
                    startMarker.isEnabled = false
                    finishMarker.isEnabled = false
                }
                view.invalidate()
            }
        )

        if (trackPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "No GPS track points recorded for this walk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        } else if (trackPoints.size == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 76.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Single GPS point recorded",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun fitTrack(
    mapView: MapView,
    trackPoints: List<TrackPoint>,
    paddingPx: Int,
    animated: Boolean
) {
    if (trackPoints.isEmpty()) return

    val geoPoints = trackPoints.map { GeoPoint(it.lat, it.lon) }
    if (geoPoints.size == 1) {
        val single = geoPoints.first()
        mapView.controller.setCenter(single)
        mapView.controller.setZoom(17.0)
        return
    }

    val minLat = geoPoints.minOf { it.latitude }
    val maxLat = geoPoints.maxOf { it.latitude }
    val minLon = geoPoints.minOf { it.longitude }
    val maxLon = geoPoints.maxOf { it.longitude }

    val latDelta = maxLat - minLat
    val lonDelta = maxLon - minLon

    if (latDelta < 0.0001 && lonDelta < 0.0001) {
        mapView.controller.setCenter(GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0))
        mapView.controller.setZoom(17.0)
        return
    }

    // Offset southern boundary to leave room for bottom HUD card
    val southPadding = minLat - (latDelta * 0.32)
    val bbox = BoundingBox(maxLat, maxLon, southPadding, minLon)

    try {
        mapView.zoomToBoundingBox(bbox, animated, paddingPx)
    } catch (_: Exception) {
        mapView.controller.setCenter(GeoPoint((maxLat + minLat) / 2.0, (maxLon + minLon) / 2.0))
        mapView.controller.setZoom(16.0)
    }
}

private fun createMarkerDrawable(
    fillColor: Int,
    strokeColor: Int,
    sizePx: Int,
    strokeWidthPx: Int
): Drawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fillColor)
        setSize(sizePx, sizePx)
        setStroke(strokeWidthPx, strokeColor)
    }
}
