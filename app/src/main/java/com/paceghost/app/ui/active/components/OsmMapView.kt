package com.paceghost.app.ui.active.components

import android.graphics.Color
import android.graphics.DashPathEffect
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
import com.paceghost.app.domain.model.GpsQuality
import com.paceghost.app.domain.model.TrackPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    userPoints: List<TrackPoint>,
    ghostPoints: List<TrackPoint>,
    ghostLat: Double? = null,
    ghostLon: Double? = null,
    gpsQuality: GpsQuality = GpsQuality.GOOD,
    accuracyMeters: Float = 0f,
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
            outlinePaint.color = Color.argb(160, 0, 229, 255) // Cyan with alpha
            outlinePaint.strokeWidth = density * 6f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            mapView.overlays.add(this)
        }
    }

    // Overlay folder containing user track segments (GPS solid and PDR dashed)
    val userTrackFolder = remember {
        FolderOverlay().apply {
            mapView.overlays.add(this)
        }
    }
    val userPolylines = remember { mutableListOf<Polyline>() }

    // User accuracy circle / halo underneath the user pin
    val userAccuracyCircle = remember {
        Polygon(mapView).apply {
            fillPaint.color = Color.argb(30, 0, 229, 255)
            fillPaint.style = Paint.Style.FILL
            outlinePaint.color = Color.argb(100, 0, 229, 255)
            outlinePaint.strokeWidth = density * 1.5f
            outlinePaint.style = Paint.Style.STROKE
            infoWindow = null
            setOnClickListener { _, _, _ -> false }
            isEnabled = false
            mapView.overlays.add(this)
        }
    }

    val ghostMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = Color.argb(220, 0, 229, 255),
                strokeColor = Color.WHITE,
                sizePx = (24 * density).toInt(),
                strokeWidthPx = (2 * density).toInt()
            )
            title = "Ghost"
            mapView.overlays.add(this)
        }
    }

    val userMarker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerDrawable(
                fillColor = Color.parseColor("#39FF14"),
                strokeColor = Color.BLACK,
                sizePx = (26 * density).toInt(),
                strokeWidthPx = (3 * density).toInt()
            )
            title = "You"
            mapView.overlays.add(this)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            // 1. Update Ghost Track
            if (ghostPoints.isNotEmpty()) {
                val gPoints = ghostPoints.map { GeoPoint(it.lat, it.lon) }
                ghostPolyline.setPoints(gPoints)
                ghostPolyline.isEnabled = true
            } else {
                ghostPolyline.setPoints(emptyList())
                ghostPolyline.isEnabled = false
            }

            // 2. Update User Track with PDR Segment Visual Distinction
            if (userPoints.size >= 2) {
                // Partition user points into continuous segments:
                // GPS segments: solid neon green (#39FF14)
                // PDR segments: cyan dashed (#00E5FF)
                // Adjacent segments share the boundary point for seamless connection without gaps
                val segments = mutableListOf<Pair<Boolean, MutableList<GeoPoint>>>()
                var currentIsPdr = userPoints[1].isPdr
                var currentSegmentPoints = mutableListOf(
                    GeoPoint(userPoints[0].lat, userPoints[0].lon),
                    GeoPoint(userPoints[1].lat, userPoints[1].lon)
                )
                segments.add(currentIsPdr to currentSegmentPoints)

                for (i in 2 until userPoints.size) {
                    val pt = userPoints[i]
                    val geo = GeoPoint(pt.lat, pt.lon)
                    if (pt.isPdr == currentIsPdr) {
                        currentSegmentPoints.add(geo)
                    } else {
                        currentIsPdr = pt.isPdr
                        currentSegmentPoints = mutableListOf(currentSegmentPoints.last(), geo)
                        segments.add(currentIsPdr to currentSegmentPoints)
                    }
                }

                // Adjust userPolylines pool size to match segment count
                while (userPolylines.size < segments.size) {
                    val poly = Polyline(view)
                    userPolylines.add(poly)
                    userTrackFolder.add(poly)
                }
                while (userPolylines.size > segments.size) {
                    val poly = userPolylines.removeAt(userPolylines.size - 1)
                    userTrackFolder.remove(poly)
                }

                // Update styling and points for each segment
                for (i in segments.indices) {
                    val (isPdr, pts) = segments[i]
                    val poly = userPolylines[i]
                    poly.outlinePaint.strokeCap = Paint.Cap.ROUND
                    poly.outlinePaint.strokeJoin = Paint.Join.ROUND
                    if (isPdr) {
                        poly.outlinePaint.color = Color.argb(220, 0, 229, 255)
                        poly.outlinePaint.strokeWidth = density * 6f
                        poly.outlinePaint.pathEffect = DashPathEffect(
                            floatArrayOf(density * 7f, density * 5f),
                            0f
                        )
                    } else {
                        poly.outlinePaint.color = Color.parseColor("#39FF14")
                        poly.outlinePaint.strokeWidth = density * 8f
                        poly.outlinePaint.pathEffect = null
                    }
                    poly.setPoints(pts)
                    poly.isEnabled = true
                }
            } else {
                for (poly in userPolylines) {
                    poly.setPoints(emptyList())
                    poly.isEnabled = false
                }
            }

            // 3. Update User Marker and Accuracy Halo
            if (userPoints.isNotEmpty()) {
                val lastPoint = userPoints.last()
                val userGeo = GeoPoint(lastPoint.lat, lastPoint.lon)
                userMarker.position = userGeo
                userMarker.isEnabled = true

                // Accuracy circle / halo underneath the user pin
                if (accuracyMeters > 0f) {
                    val clampedRadius = accuracyMeters.toDouble().coerceIn(4.0, 150.0)
                    userAccuracyCircle.setPoints(Polygon.pointsAsCircle(userGeo, clampedRadius))
                    val (fillColor, strokeColor) = when (gpsQuality) {
                        GpsQuality.GOOD -> Color.argb(30, 57, 255, 20) to Color.argb(100, 57, 255, 20)
                        GpsQuality.WEAK -> Color.argb(30, 255, 215, 0) to Color.argb(100, 255, 215, 0)
                        GpsQuality.PDR_ACTIVE -> Color.argb(30, 0, 229, 255) to Color.argb(100, 0, 229, 255)
                    }
                    userAccuracyCircle.fillPaint.color = fillColor
                    userAccuracyCircle.outlinePaint.color = strokeColor
                    userAccuracyCircle.isEnabled = true
                } else {
                    userAccuracyCircle.setPoints(emptyList())
                    userAccuracyCircle.isEnabled = false
                }

                if (autoCenter) {
                    view.controller.animateTo(userGeo)
                }
            } else {
                userMarker.isEnabled = false
                userAccuracyCircle.setPoints(emptyList())
                userAccuracyCircle.isEnabled = false

                // If ghost has points, center on ghost start initially
                if (ghostPoints.isNotEmpty() && autoCenter) {
                    val first = ghostPoints.first()
                    view.controller.setCenter(GeoPoint(first.lat, first.lon))
                }
            }

            // 4. Update Ghost Marker
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
