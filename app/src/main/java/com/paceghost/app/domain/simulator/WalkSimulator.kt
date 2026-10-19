package com.paceghost.app.domain.simulator

import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.model.TrackPoint
import kotlin.math.cos
import kotlin.math.sin

class WalkSimulator(
    referenceTrack: List<TrackPoint> = defaultParkPobedyLoop()
) {
    companion object {
        const val DEFAULT_SPEED_KMH = 5.0
        const val STEPS_PER_MINUTE_AT_5KMH = 115.0

        /**
         * Generates a realistic ~1.2 km walking loop in Park Pobedy, Moscow (55.731, 37.505).
         */
        fun defaultParkPobedyLoop(): List<TrackPoint> {
            val centerLat = 55.7310
            val centerLon = 37.5050
            val radiusLat = 0.0018 // ~200 meters north/south
            val radiusLon = 0.0032 // ~200 meters east/west
            val pointCount = 100
            val speedKmh = 5.0
            val speedMps = (speedKmh * 1000.0) / 3600.0
            val stepsPerSec = (STEPS_PER_MINUTE_AT_5KMH / 60.0)

            val rawPoints = ArrayList<TrackPoint>(pointCount)
            for (i in 0 until pointCount) {
                val angle = (2.0 * Math.PI * i) / (pointCount - 1)
                // Add gentle natural curvature
                val lat = centerLat + radiusLat * sin(angle)
                val lon = centerLon + radiusLon * cos(angle)
                rawPoints.add(
                    TrackPoint(
                        lat = lat,
                        lon = lon,
                        altitude = 155.0 + 5.0 * sin(angle * 2),
                        speed = speedMps.toFloat()
                    )
                )
            }

            val withDistances = GeoMath.computeCumulativeDistances(rawPoints)
            val result = ArrayList<TrackPoint>(pointCount)
            for (pt in withDistances) {
                val elapsedSec = (pt.cumulativeDistance / speedMps).toLong()
                val steps = (elapsedSec * stepsPerSec).toInt()
                result.add(
                    pt.copy(
                        elapsedSeconds = elapsedSec,
                        stepCount = steps
                    )
                )
            }
            return result
        }
    }

    private val track: List<TrackPoint> = if (referenceTrack.isNotEmpty() && referenceTrack.last().cumulativeDistance > 0.0) {
        referenceTrack
    } else {
        GeoMath.computeCumulativeDistances(referenceTrack.ifEmpty { defaultParkPobedyLoop() })
    }

    private var speedKmh: Double = DEFAULT_SPEED_KMH
    private var isOffRoute: Boolean = false
    private var currentDistanceMeters: Double = 0.0
    private var fractionalElapsedSeconds: Double = 0.0
    private var currentStepCount: Int = 0
    private var fractionalSteps: Double = 0.0
    private var startTimestampMs: Long = System.currentTimeMillis()

    fun setSpeedKmh(speed: Double) {
        speedKmh = if (speed < 0.0) 0.0 else speed
    }

    fun getSpeedKmh(): Double = speedKmh

    fun setOffRoute(offRoute: Boolean) {
        isOffRoute = offRoute
    }

    fun isOffRoute(): Boolean = isOffRoute

    fun getReferenceTrack(): List<TrackPoint> = track

    fun reset(newStartTimestamp: Long = System.currentTimeMillis()) {
        speedKmh = DEFAULT_SPEED_KMH
        isOffRoute = false
        currentDistanceMeters = 0.0
        fractionalElapsedSeconds = 0.0
        currentStepCount = 0
        fractionalSteps = 0.0
        startTimestampMs = newStartTimestamp
    }

    /**
     * Advances simulation by [deltaSeconds] and returns the generated [TrackPoint].
     */
    fun tick(deltaSeconds: Double): TrackPoint {
        fractionalElapsedSeconds += deltaSeconds
        val elapsedSeconds = fractionalElapsedSeconds.toLong()

        // 1. Advance distance
        val speedMps = (speedKmh * 1000.0) / 3600.0
        val deltaDistance = speedMps * deltaSeconds
        currentDistanceMeters += deltaDistance

        // 2. Advance steps based on speed (~115 steps/min at 5 km/h)
        val stepsPerSec = if (speedKmh > 0.0) {
            (speedKmh / 5.0) * (STEPS_PER_MINUTE_AT_5KMH / 60.0)
        } else {
            0.0
        }
        fractionalSteps += stepsPerSec * deltaSeconds
        val addedSteps = fractionalSteps.toInt()
        currentStepCount += addedSteps
        fractionalSteps -= addedSteps

        // 3. Determine location along reference track
        val totalTrackMeters = if (track.isNotEmpty()) track.last().cumulativeDistance else 0.0
        val effectiveDistance = if (totalTrackMeters > 0.0) {
            currentDistanceMeters % totalTrackMeters
        } else {
            0.0
        }

        var (lat, lon, altitude) = interpolateCoordinatesAtDistance(effectiveDistance)

        // 4. If off-route simulated, add ~111m offset (0.001 deg lat)
        if (isOffRoute) {
            lat += 0.0010
            lon += 0.0005
        }

        val timestamp = startTimestampMs + (fractionalElapsedSeconds * 1000.0).toLong()

        return TrackPoint(
            lat = lat,
            lon = lon,
            altitude = altitude,
            timestamp = timestamp,
            elapsedSeconds = elapsedSeconds,
            cumulativeDistance = currentDistanceMeters,
            speed = speedMps.toFloat(),
            stepCount = currentStepCount
        )
    }

    private fun interpolateCoordinatesAtDistance(distanceMeters: Double): Triple<Double, Double, Double> {
        if (track.isEmpty()) return Triple(0.0, 0.0, 0.0)
        if (distanceMeters <= track.first().cumulativeDistance) {
            return Triple(track.first().lat, track.first().lon, track.first().altitude)
        }
        if (distanceMeters >= track.last().cumulativeDistance) {
            return Triple(track.last().lat, track.last().lon, track.last().altitude)
        }

        for (i in 0 until track.size - 1) {
            val p1 = track[i]
            val p2 = track[i + 1]
            if (distanceMeters in p1.cumulativeDistance..p2.cumulativeDistance) {
                val segLen = p2.cumulativeDistance - p1.cumulativeDistance
                val fraction = if (segLen > 0.0) (distanceMeters - p1.cumulativeDistance) / segLen else 0.0
                val lat = p1.lat + fraction * (p2.lat - p1.lat)
                val lon = p1.lon + fraction * (p2.lon - p1.lon)
                val alt = p1.altitude + fraction * (p2.altitude - p1.altitude)
                return Triple(lat, lon, alt)
            }
        }

        return Triple(track.last().lat, track.last().lon, track.last().altitude)
    }
}
