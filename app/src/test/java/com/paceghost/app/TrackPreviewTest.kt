package com.paceghost.app

import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.local.entity.averageSpeedKmh
import com.paceghost.app.domain.engine.PaceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPreviewTest {

    @Test
    fun testAverageSpeedKmhCalculation() {
        val walk = WalkEntity(
            id = 1L,
            title = "Morning Walk",
            startTime = 1000L,
            endTime = 4600L,
            durationSeconds = 3600L, // 1 hour
            distanceMeters = 5000.0,  // 5 km
            stepCount = 6000,
            averagePaceSecondsPerKm = 720.0
        )

        assertEquals(5.0, walk.averageSpeedKmh, 0.01)
    }

    @Test
    fun testAverageSpeedKmhZeroDistanceOrDurationFallback() {
        val walkZeroDuration = WalkEntity(
            id = 2L,
            title = "Zero Duration Walk",
            startTime = 1000L,
            endTime = 1000L,
            durationSeconds = 0L,
            distanceMeters = 0.0,
            stepCount = 0,
            averagePaceSecondsPerKm = 600.0 // 10 min/km -> 6 km/h
        )

        assertEquals(6.0, walkZeroDuration.averageSpeedKmh, 0.01)

        val walkAllZero = WalkEntity(
            id = 3L,
            title = "All Zero Walk",
            startTime = 0L,
            endTime = 0L,
            durationSeconds = 0L,
            distanceMeters = 0.0,
            stepCount = 0,
            averagePaceSecondsPerKm = 0.0
        )

        assertEquals(0.0, walkAllZero.averageSpeedKmh, 0.01)
    }

    @Test
    fun testPaceCalculatorFormatters() {
        assertEquals("05:30", PaceCalculator.formatPaceMinSec(330.0))
        assertEquals("5.00 km", PaceCalculator.formatDistance(5000.0))
        assertEquals("850 m", PaceCalculator.formatDistance(850.0))
        assertEquals("01:00:00", PaceCalculator.formatDuration(3600L))
        assertEquals("05:00", PaceCalculator.formatDuration(300L))
    }
}
