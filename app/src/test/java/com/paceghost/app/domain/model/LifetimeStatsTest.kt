package com.paceghost.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifetimeStatsTest {

    @Test
    fun `default LifetimeStats has no qualifying walks and null weighted average speed`() {
        val stats = LifetimeStats()
        assertFalse(stats.hasQualifyingWalks)
        assertNull(stats.weightedAverageSpeedKmh)
    }

    @Test
    fun `weightedAverageSpeedKmh returns null when duration or distance is zero`() {
        val zeroDuration = LifetimeStats(
            walkCount = 1,
            totalDistanceMeters = 1000.0,
            totalDurationSeconds = 0L
        )
        val zeroDistance = LifetimeStats(
            walkCount = 1,
            totalDistanceMeters = 0.0,
            totalDurationSeconds = 600L
        )
        assertNull(zeroDuration.weightedAverageSpeedKmh)
        assertNull(zeroDistance.weightedAverageSpeedKmh)
    }

    @Test
    fun `weightedAverageSpeedKmh computes totalDistance over totalDuration`() {
        // 10.0 km in 2.0 hours = 5.0 km/h
        val stats = LifetimeStats(
            walkCount = 2,
            totalDistanceMeters = 10_000.0,
            totalDurationSeconds = 7_200L,
            totalSteps = 12_000L,
            longestWalkMeters = 8_000.0
        )
        assertTrue(stats.hasQualifyingWalks)
        assertEquals(5.0, stats.weightedAverageSpeedKmh!!, 1e-6)
    }

    @Test
    fun `formatTotalDistance formats km with one decimal in Locale US`() {
        assertEquals("0.0 km", LifetimeStatsFormatter.formatTotalDistance(0.0))
        assertEquals("142.8 km", LifetimeStatsFormatter.formatTotalDistance(142_840.0))
    }

    @Test
    fun `formatTotalWalks formats integer count`() {
        assertEquals("0", LifetimeStatsFormatter.formatTotalWalks(0))
        assertEquals("47", LifetimeStatsFormatter.formatTotalWalks(47))
    }

    @Test
    fun `formatTotalTime formats minutes only when under one hour and hours plus minutes otherwise`() {
        assertEquals("0m", LifetimeStatsFormatter.formatTotalTime(0L))
        assertEquals("35m", LifetimeStatsFormatter.formatTotalTime(2_100L))
        assertEquals("42h 15m", LifetimeStatsFormatter.formatTotalTime(152_100L))
    }

    @Test
    fun `formatTotalSteps formats with US thousands separator`() {
        assertEquals("0", LifetimeStatsFormatter.formatTotalSteps(0L))
        assertEquals("184,320", LifetimeStatsFormatter.formatTotalSteps(184_320L))
    }

    @Test
    fun `formatAvgSpeed formats one decimal km per hour or em dash when null`() {
        assertEquals("—", LifetimeStatsFormatter.formatAvgSpeed(null))
        assertEquals("5.2 km/h", LifetimeStatsFormatter.formatAvgSpeed(5.24))
    }

    @Test
    fun `formatLongestWalk formats km with one decimal or em dash when no qualifying walks`() {
        assertEquals("—", LifetimeStatsFormatter.formatLongestWalk(walkCount = 0, longestWalkMeters = 0.0))
        assertEquals("—", LifetimeStatsFormatter.formatLongestWalk(walkCount = 1, longestWalkMeters = 0.0))
        assertEquals("12.4 km", LifetimeStatsFormatter.formatLongestWalk(walkCount = 3, longestWalkMeters = 12_400.0))
    }
}
