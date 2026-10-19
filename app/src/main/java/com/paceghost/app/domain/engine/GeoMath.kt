package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.TrackPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two points on the Earth in meters
     * using the Haversine formula.
     */
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val sinDeltaPhi2 = sin(deltaPhi / 2.0)
        val sinDeltaLambda2 = sin(deltaLambda / 2.0)

        val a = sinDeltaPhi2 * sinDeltaPhi2 +
                cos(phi1) * cos(phi2) * sinDeltaLambda2 * sinDeltaLambda2
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_METERS * c
    }

    fun haversineDistance(p1: TrackPoint, p2: TrackPoint): Double =
        haversineDistance(p1.lat, p1.lon, p2.lat, p2.lon)

    /**
     * Projects a point onto a track segment defined by [segStart] and [segEnd].
     * Clamps the projection within the segment boundaries [0, 1].
     *
     * @return Pair of projected [TrackPoint] (with interpolated attributes including cumulativeDistance
     *         and elapsedSeconds) and the perpendicular cross-track distance in meters.
     */
    fun projectPointOnSegment(
        point: TrackPoint,
        segStart: TrackPoint,
        segEnd: TrackPoint
    ): Pair<TrackPoint, Double> {
        val phiMid = Math.toRadians((segStart.lat + segEnd.lat) / 2.0)
        val mPerLat = Math.toRadians(1.0) * EARTH_RADIUS_METERS
        val mPerLon = Math.toRadians(1.0) * EARTH_RADIUS_METERS * cos(phiMid)

        val dx = (segEnd.lon - segStart.lon) * mPerLon
        val dy = (segEnd.lat - segStart.lat) * mPerLat

        val segLenSq = dx * dx + dy * dy
        val t = if (segLenSq < 1e-6) {
            0.0
        } else {
            val wx = (point.lon - segStart.lon) * mPerLon
            val wy = (point.lat - segStart.lat) * mPerLat
            val rawT = (wx * dx + wy * dy) / segLenSq
            rawT.coerceIn(0.0, 1.0)
        }

        val projLat = segStart.lat + t * (segEnd.lat - segStart.lat)
        val projLon = segStart.lon + t * (segEnd.lon - segStart.lon)
        val projAlt = segStart.altitude + t * (segEnd.altitude - segStart.altitude)
        val projTimestamp = (segStart.timestamp + t * (segEnd.timestamp - segStart.timestamp)).toLong()
        val projElapsed = (segStart.elapsedSeconds + t * (segEnd.elapsedSeconds - segStart.elapsedSeconds)).toLong()
        val projDist = segStart.cumulativeDistance + t * (segEnd.cumulativeDistance - segStart.cumulativeDistance)
        val projSpeed = (segStart.speed + t * (segEnd.speed - segStart.speed)).toFloat()
        val projSteps = (segStart.stepCount + t * (segEnd.stepCount - segStart.stepCount)).toInt()

        val projectedPoint = TrackPoint(
            lat = projLat,
            lon = projLon,
            altitude = projAlt,
            timestamp = projTimestamp,
            elapsedSeconds = projElapsed,
            cumulativeDistance = projDist,
            speed = projSpeed,
            stepCount = projSteps
        )

        val crossTrackDistance = haversineDistance(point.lat, point.lon, projLat, projLon)
        return Pair(projectedPoint, crossTrackDistance)
    }

    /**
     * Computes and returns points with sequential cumulative distance in meters along the path.
     */
    fun computeCumulativeDistances(points: List<TrackPoint>): List<TrackPoint> {
        if (points.isEmpty()) return emptyList()

        val result = ArrayList<TrackPoint>(points.size)
        var cumulative = 0.0
        result.add(points[0].copy(cumulativeDistance = 0.0))

        for (i in 1 until points.size) {
            val dist = haversineDistance(points[i - 1], points[i])
            cumulative += dist
            result.add(points[i].copy(cumulativeDistance = cumulative))
        }

        return result
    }
}
