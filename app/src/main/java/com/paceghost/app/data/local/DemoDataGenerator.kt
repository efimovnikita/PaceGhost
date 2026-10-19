package com.paceghost.app.data.local

import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.model.TrackPoint
import kotlin.math.cos
import kotlin.math.sin

object DemoDataGenerator {
    const val DEMO_WALK_TITLE = "Victory Park Demo Track"
    const val CENTER_LAT = 55.7310
    const val CENTER_LON = 37.5050
    const val POINT_COUNT = 100
    const val SPEED_KMH = 5.0
    const val TARGET_STEPS = 1350

    /**
     * Generates the demo walk and 100 track points representing a ~1.2 km loop in Park Pobedy, Moscow.
     * Speed is 5.0 km/h, step count ~1350 steps.
     */
    fun createDemoWalkData(walkId: Long = 0, startTimeMs: Long = System.currentTimeMillis() - 864_000L): Pair<WalkEntity, List<TrackPointEntity>> {
        val radiusLat = 0.00175 // ~195 meters
        val radiusLon = 0.00310 // ~195 meters
        val speedMps = (SPEED_KMH * 1000.0) / 3600.0 // ~1.3889 m/s

        // 1. Generate 100 raw points along smooth elliptical loop
        val rawPoints = ArrayList<TrackPoint>(POINT_COUNT)
        for (i in 0 until POINT_COUNT) {
            val angle = (2.0 * Math.PI * i) / (POINT_COUNT - 1)
            val lat = CENTER_LAT + radiusLat * sin(angle)
            val lon = CENTER_LON + radiusLon * cos(angle)
            val alt = 155.0 + 4.5 * sin(angle * 2)
            rawPoints.add(
                TrackPoint(
                    lat = lat,
                    lon = lon,
                    altitude = alt,
                    speed = speedMps.toFloat()
                )
            )
        }

        // 2. Compute accurate cumulative distances
        val withDistances = GeoMath.computeCumulativeDistances(rawPoints)
        val totalDistance = withDistances.last().cumulativeDistance
        val totalDurationSeconds = (totalDistance / speedMps).toLong()
        val endTimeMs = startTimeMs + totalDurationSeconds * 1000L

        // Pace: seconds per km
        val avgPaceSecondsPerKm = if (totalDistance > 0) (totalDurationSeconds / (totalDistance / 1000.0)) else 720.0

        val walkEntity = WalkEntity(
            id = walkId,
            title = DEMO_WALK_TITLE,
            startTime = startTimeMs,
            endTime = endTimeMs,
            durationSeconds = totalDurationSeconds,
            distanceMeters = totalDistance,
            stepCount = TARGET_STEPS,
            averagePaceSecondsPerKm = avgPaceSecondsPerKm,
            isFavorite = true
        )

        // 3. Convert to TrackPointEntity with proportional timestamps and steps
        val pointEntities = withDistances.mapIndexed { index, pt ->
            val fraction = if (totalDistance > 0.0) pt.cumulativeDistance / totalDistance else (index.toDouble() / (POINT_COUNT - 1))
            val elapsedSec = (fraction * totalDurationSeconds).toLong()
            val pointTime = startTimeMs + elapsedSec * 1000L
            val stepsAtPoint = (fraction * TARGET_STEPS).toInt()

            TrackPointEntity(
                id = 0,
                walkId = walkId,
                latitude = pt.lat,
                longitude = pt.lon,
                altitude = pt.altitude,
                timestamp = pointTime,
                elapsedSeconds = elapsedSec,
                cumulativeDistance = pt.cumulativeDistance,
                speed = speedMps.toFloat(),
                stepCountAtPoint = stepsAtPoint
            )
        }

        return Pair(walkEntity, pointEntities)
    }
}
