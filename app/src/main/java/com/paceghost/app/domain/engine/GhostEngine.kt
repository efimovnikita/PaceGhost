package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.GhostComparisonResult
import com.paceghost.app.domain.model.GhostStatus
import com.paceghost.app.domain.model.TrackPoint
import kotlin.math.max
import kotlin.math.min

class GhostEngine(
    ghostRoute: List<TrackPoint> = emptyList()
) {
    companion object {
        const val OFF_ROUTE_THRESHOLD_METERS = 60.0
        const val BEHIND_TIME_THRESHOLD_SECONDS = 30L
        const val AHEAD_TIME_THRESHOLD_SECONDS = -15L
        const val BEHIND_VIBRATION_COOLDOWN_SECONDS = 60L
    }

    private var route: List<TrackPoint> = prepareRoute(ghostRoute)
    private var lastMatchedSegmentIndex: Int = 0
    private var lastBehindVibrationElapsedSeconds: Long = -1000L
    private var previousStatus: GhostStatus = GhostStatus.NO_GHOST
    private var wasBehind: Boolean = false

    fun setGhostRoute(newRoute: List<TrackPoint>) {
        route = prepareRoute(newRoute)
        reset()
    }

    fun getGhostRoute(): List<TrackPoint> = route

    fun reset() {
        lastMatchedSegmentIndex = 0
        lastBehindVibrationElapsedSeconds = -1000L
        previousStatus = GhostStatus.NO_GHOST
        wasBehind = false
    }

    private fun prepareRoute(rawPoints: List<TrackPoint>): List<TrackPoint> {
        if (rawPoints.isEmpty()) return emptyList()
        // Ensure cumulative distances are populated
        val needsDistance = rawPoints.size > 1 && rawPoints.last().cumulativeDistance <= 0.0
        return if (needsDistance) {
            GeoMath.computeCumulativeDistances(rawPoints)
        } else {
            rawPoints
        }
    }

    fun compare(userPoint: TrackPoint): GhostComparisonResult {
        return compare(
            userLat = userPoint.lat,
            userLon = userPoint.lon,
            elapsedSeconds = userPoint.elapsedSeconds
        )
    }

    fun compare(userLat: Double, userLon: Double, elapsedSeconds: Long): GhostComparisonResult {
        if (route.isEmpty()) {
            return GhostComparisonResult.NO_GHOST
        }

        val totalGhostMeters = route.last().cumulativeDistance
        val userProbe = TrackPoint(lat = userLat, lon = userLon, elapsedSeconds = elapsedSeconds)

        // Interpolate ghost position at current elapsed seconds
        val ghostAtTime = interpolateGhostAtElapsed(route, elapsedSeconds)

        if (route.size == 1) {
            val dist = GeoMath.haversineDistance(userLat, userLon, route[0].lat, route[0].lon)
            val status = if (dist > OFF_ROUTE_THRESHOLD_METERS) GhostStatus.OFF_ROUTE else GhostStatus.ON_PACE
            return GhostComparisonResult(
                status = status,
                timeDeltaSeconds = 0L,
                distanceDeltaMeters = 0.0,
                currentProgressMeters = 0.0,
                totalGhostMeters = totalGhostMeters,
                ghostLat = route[0].lat,
                ghostLon = route[0].lon
            )
        }

        val numSegments = route.size - 1

        // 1. Search nearest segment in progress window first
        val windowStart = max(0, lastMatchedSegmentIndex - 10)
        val windowEnd = min(numSegments - 1, lastMatchedSegmentIndex + 40)

        var bestSegmentIndex = -1
        var minCrossTrackDist = Double.MAX_VALUE
        var bestProjectedPoint: TrackPoint? = null

        for (i in windowStart..windowEnd) {
            val (proj, crossDist) = GeoMath.projectPointOnSegment(userProbe, route[i], route[i + 1])
            if (crossDist < minCrossTrackDist) {
                minCrossTrackDist = crossDist
                bestSegmentIndex = i
                bestProjectedPoint = proj
            }
        }

        // If window search was beyond threshold, search the entire route
        if (minCrossTrackDist > OFF_ROUTE_THRESHOLD_METERS) {
            for (i in 0 until numSegments) {
                if (i in windowStart..windowEnd) continue
                val (proj, crossDist) = GeoMath.projectPointOnSegment(userProbe, route[i], route[i + 1])
                if (crossDist < minCrossTrackDist) {
                    minCrossTrackDist = crossDist
                    bestSegmentIndex = i
                    bestProjectedPoint = proj
                }
            }
        }

        val currentProgressMeters = bestProjectedPoint?.cumulativeDistance ?: 0.0

        // 2. Off route check
        if (minCrossTrackDist > OFF_ROUTE_THRESHOLD_METERS || bestProjectedPoint == null) {
            previousStatus = GhostStatus.OFF_ROUTE
            wasBehind = false
            return GhostComparisonResult(
                status = GhostStatus.OFF_ROUTE,
                timeDeltaSeconds = 0L,
                distanceDeltaMeters = 0.0,
                currentProgressMeters = currentProgressMeters,
                totalGhostMeters = totalGhostMeters,
                ghostLat = ghostAtTime.lat,
                ghostLon = ghostAtTime.lon,
                shouldVibrateBehind = false,
                shouldVibrateOvertake = false
            )
        }

        lastMatchedSegmentIndex = bestSegmentIndex

        // 3. Interpolate ghost elapsed seconds at current distance D_curr
        val ghostElapsedSeconds = bestProjectedPoint.elapsedSeconds

        // 4. Calculate timeDelta = elapsedSeconds - ghostElapsedSeconds
        val timeDeltaSeconds = elapsedSeconds - ghostElapsedSeconds

        // 5. Calculate distanceDelta = currentDistance - ghostDistanceAtElapsedSeconds
        val distanceDeltaMeters = currentProgressMeters - ghostAtTime.cumulativeDistance

        // 6. Determine status
        val status = when {
            timeDeltaSeconds > BEHIND_TIME_THRESHOLD_SECONDS -> GhostStatus.BEHIND
            timeDeltaSeconds < AHEAD_TIME_THRESHOLD_SECONDS -> GhostStatus.AHEAD
            else -> GhostStatus.ON_PACE
        }

        // 7. Trigger flags
        var shouldVibrateBehind = false
        var shouldVibrateOvertake = false

        if (status == GhostStatus.BEHIND) {
            if (lastBehindVibrationElapsedSeconds < 0L ||
                (elapsedSeconds - lastBehindVibrationElapsedSeconds >= BEHIND_VIBRATION_COOLDOWN_SECONDS)
            ) {
                shouldVibrateBehind = true
                lastBehindVibrationElapsedSeconds = elapsedSeconds
            }
            wasBehind = true
        } else if (status == GhostStatus.AHEAD) {
            if (wasBehind || previousStatus == GhostStatus.BEHIND) {
                shouldVibrateOvertake = true
            }
            wasBehind = false
        }

        previousStatus = status

        return GhostComparisonResult(
            status = status,
            timeDeltaSeconds = timeDeltaSeconds,
            distanceDeltaMeters = distanceDeltaMeters,
            currentProgressMeters = currentProgressMeters,
            totalGhostMeters = totalGhostMeters,
            ghostLat = ghostAtTime.lat,
            ghostLon = ghostAtTime.lon,
            shouldVibrateBehind = shouldVibrateBehind,
            shouldVibrateOvertake = shouldVibrateOvertake
        )
    }

    private fun interpolateGhostAtElapsed(route: List<TrackPoint>, elapsedSeconds: Long): TrackPoint {
        if (route.isEmpty()) return TrackPoint(0.0, 0.0)
        if (elapsedSeconds <= route.first().elapsedSeconds) return route.first()
        if (elapsedSeconds >= route.last().elapsedSeconds) return route.last()

        for (i in 0 until route.size - 1) {
            val p1 = route[i]
            val p2 = route[i + 1]
            if (elapsedSeconds in p1.elapsedSeconds..p2.elapsedSeconds) {
                val span = (p2.elapsedSeconds - p1.elapsedSeconds).toDouble()
                val fraction = if (span > 0) (elapsedSeconds - p1.elapsedSeconds) / span else 0.0
                return TrackPoint(
                    lat = p1.lat + fraction * (p2.lat - p1.lat),
                    lon = p1.lon + fraction * (p2.lon - p1.lon),
                    altitude = p1.altitude + fraction * (p2.altitude - p1.altitude),
                    timestamp = (p1.timestamp + fraction * (p2.timestamp - p1.timestamp)).toLong(),
                    elapsedSeconds = elapsedSeconds,
                    cumulativeDistance = p1.cumulativeDistance + fraction * (p2.cumulativeDistance - p1.cumulativeDistance),
                    speed = (p1.speed + fraction * (p2.speed - p1.speed)).toFloat()
                )
            }
        }

        return route.last()
    }
}
