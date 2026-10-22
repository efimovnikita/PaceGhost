package com.paceghost.app

import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.local.entity.calculateMedianSpeedKmh
import com.paceghost.app.domain.model.VoiceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WalkEntityTest {

    private fun createWalk(durationSeconds: Long, distanceMeters: Double): WalkEntity {
        return WalkEntity(
            title = "Test Walk",
            startTime = 1000L,
            endTime = 1000L + durationSeconds * 1000L,
            durationSeconds = durationSeconds,
            distanceMeters = distanceMeters,
            stepCount = 1000,
            averagePaceSecondsPerKm = if (distanceMeters > 0) durationSeconds / (distanceMeters / 1000.0) else 0.0
        )
    }

    @Test
    fun `calculateMedianSpeedKmh returns default when list is empty`() {
        val emptyList = emptyList<WalkEntity>()
        val median = emptyList.calculateMedianSpeedKmh()
        assertEquals(VoiceSettings.DEFAULT_TARGET_SPEED_KMH, median, 0.001)
    }

    @Test
    fun `calculateMedianSpeedKmh ignores walks shorter than 60s or 50m`() {
        val walks = listOf(
            createWalk(durationSeconds = 10, distanceMeters = 15.0),
            createWalk(durationSeconds = 50, distanceMeters = 40.0)
        )
        val median = walks.calculateMedianSpeedKmh()
        assertEquals(VoiceSettings.DEFAULT_TARGET_SPEED_KMH, median, 0.001)
    }

    @Test
    fun `calculateMedianSpeedKmh calculates correct median for odd number of walks`() {
        // Walk 1: 3.6 km/h (1000m in 1000s)
        // Walk 2: 5.4 km/h (1500m in 1000s)
        // Walk 3: 7.2 km/h (2000m in 1000s)
        val walks = listOf(
            createWalk(1000, 1000.0), // 3.6
            createWalk(1000, 2000.0), // 7.2
            createWalk(1000, 1500.0)  // 5.4
        )
        val median = walks.calculateMedianSpeedKmh()
        assertEquals(5.4, median, 0.001)
    }

    @Test
    fun `calculateMedianSpeedKmh calculates correct median for even number of walks`() {
        // Walk 1: 4.0 km/h
        // Walk 2: 6.0 km/h
        val walks = listOf(
            createWalk(3600, 4000.0), // 4.0
            createWalk(3600, 6000.0)  // 6.0
        )
        val median = walks.calculateMedianSpeedKmh()
        assertEquals(5.0, median, 0.001)
    }
}
