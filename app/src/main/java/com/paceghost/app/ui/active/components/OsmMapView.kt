package com.paceghost.app.ui.active.components

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paceghost.app.domain.model.TrackPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userPoints: List<TrackPoint>,
    ghostPoints: List<TrackPoint>,
    ghostLat: Double? = null,
    ghostLon: Double? = null,
    autoCenter: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current.density

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(17.5)
        }
    }

    // Manage MapView lifecycle
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

    val ghostPolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.argb(160, 0, 229, 255) // Cyan with alpha
            outlinePaint.strokeWidth = density * 6f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            mapView.overlays.add(this)
        }
    }

    val userPolyline = remember {
        Polyline(mapView).apply {
            outlinePaint.color = android.graphics.Color.parseColor("#39FF14") // Bright neon green
            outlinePaint.strokeWidth = density * 8f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            mapView.overlays.add(this)
        }
    }

    val ghostMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = android.graphics.Color.argb(220, 0, 229, 255),
                strokeColor = android.graphics.Color.WHITE,
                sizePx = (24 * density).toInt(),
                strokeWidthPx = (2 * density).toInt()
            )
            title = "Призрак"
            mapView.overlays.add(this)
        }
    }

    val userMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = android.graphics.Color.parseColor("#39FF14"),
                strokeColor = android.graphics.Color.BLACK,
                sizePx = (26 * density).toInt(),
                strokeWidthPx = (3 * density).toInt()
            )
            title = "Вы"
            mapView.overlays.add(this)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // Update Ghost Track
            if (ghostPoints.isNotEmpty()) {
                val gPoints = ghostPoints.map { GeoPoint(it.lat, it.lon) }
                ghostPolyline.setPoints(gPoints)
                ghostPolyline.isEnabled = true
            } else {
                ghostPolyline.setPoints(emptyList())
                ghostPolyline.isEnabled = false
            }

            // Update User Track
            if (userPoints.isNotEmpty()) {
                val uPoints = userPoints.map { GeoPoint(it.lat, it.lon) }
                userPolyline.setPoints(uPoints)
                userPolyline.isEnabled = true

                val lastPoint = userPoints.last()
                val userGeo = GeoPoint(lastPoint.lat, lastPoint.lon)
                userMarker.position = userGeo
                userMarker.isEnabled = true

                if (autoCenter) {
                    view.controller.animateTo(userGeo)
                }
            } else {
                userPolyline.setPoints(emptyList())
                userPolyline.isEnabled = false
                userMarker.isEnabled = false

                // If ghost has points, center on ghost start initially
                if (ghostPoints.isNotEmpty() && autoCenter) {
                    val first = ghostPoints.first()
                    view.controller.setCenter(GeoPoint(first.lat, first.lon))
                }
            }

            // Update Ghost Marker
            if (ghostLat != null && ghostLon != null) {
                ghostMarker.position = GeoPoint(ghostLat, ghostLon)
                ghostMarker.isEnabled = true
            } else {
                ghostMarker.isEnabled = false
            }

            view.invalidate()
        }
    )
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
