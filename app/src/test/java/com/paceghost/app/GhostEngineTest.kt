package com.paceghost.app

import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.engine.GhostEngine
import com.paceghost.app.domain.model.GhostStatus
import com.paceghost.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GhostEngineTest {

    private lateinit var ghostRoute: List<TrackPoint>
    private lateinit var ghostEngine: GhostEngine

    @Before
    fun setUp() {
        // Create reference track: 3 points, 100 seconds apart
        val p0 = TrackPoint(lat = 55.7300, lon = 37.5000, elapsedSeconds = 0)
        val p1 = TrackPoint(lat = 55.7310, lon = 37.5000, elapsedSeconds = 100)
        val p2 = TrackPoint(lat = 55.7320, lon = 37.5000, elapsedSeconds = 200)

        ghostRoute = GeoMath.computeCumulativeDistances(listOf(p0, p1, p2)).mapIndexed { index, pt ->
            pt.copy(elapsedSeconds = (index * 100).toLong())
        }
        ghostEngine = GhostEngine(ghostRoute)
    }

    @Test
    fun `user faster than ghost triggers AHEAD status`() {
        // Point 1 is at 55.7310, 37.5000. Ghost was there at 100s.
        // User reaches Point 1 at 70s -> timeDelta = -30s (< -15s threshold)
        val result = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 70)

        assertEquals(GhostStatus.AHEAD, result.status)
        assertTrue("timeDelta should be < -15s, was ${result.timeDeltaSeconds}", result.timeDeltaSeconds < -15L)
        assertTrue("distanceDelta should be positive, was ${result.distanceDeltaMeters}", result.distanceDeltaMeters > 0)
    }

    @Test
    fun `user slower than ghost triggers BEHIND status`() {
        // Point 1 reached at 145s -> timeDelta = +45s (> 30s threshold)
        val result = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 145)

        assertEquals(GhostStatus.BEHIND, result.status)
        assertTrue("timeDelta should be > 30s, was ${result.timeDeltaSeconds}", result.timeDeltaSeconds > 30L)
        assertTrue("distanceDelta should be negative, was ${result.distanceDeltaMeters}", result.distanceDeltaMeters < 0)
    }

    @Test
    fun `user within thresholds is ON_PACE`() {
        // Point 1 reached at 105s -> timeDelta = +5s (between -15s and 30s)
        val result = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 105)

        assertEquals(GhostStatus.ON_PACE, result.status)
        assertEquals(5L, result.timeDeltaSeconds)
    }

    @Test
    fun `user further than 60m from track is OFF_ROUTE`() {
        // lon 37.5020 is ~125 meters away from 37.5000 at this latitude
        val result = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 100)

        assertEquals(GhostStatus.OFF_ROUTE, result.status)
        assertFalse(result.shouldVibrateBehind)
        assertFalse(result.shouldVibrateOvertake)
    }

    @Test
    fun `anti-spam cooldown prevents repeated behind vibration within 60s`() {
        // First BEHIND comparison at elapsed 100s
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 150)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertTrue("First behind trigger should vibrate", r1.shouldVibrateBehind)

        // Second comparison at elapsed 120s (20s later < 60s cooldown)
        val r2 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 170)
        assertEquals(GhostStatus.BEHIND, r2.status)
        assertFalse("Second behind trigger within 60s should NOT vibrate", r2.shouldVibrateBehind)

        // Third comparison at elapsed 215s (65s later >= 60s cooldown)
        val r3 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 235)
        assertEquals(GhostStatus.BEHIND, r3.status)
        assertTrue("Third behind trigger after 60s cooldown SHOULD vibrate", r3.shouldVibrateBehind)
    }

    @Test
    fun `transition from BEHIND to AHEAD triggers overtake vibration`() {
        // 1. User falls behind
        val r1 = ghostEngine.compare(userLat = 55.7305, userLon = 37.5000, elapsedSeconds = 100)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertFalse(r1.shouldVibrateOvertake)

        // 2. User sprints ahead to end of track ahead of ghost schedule
        val r2 = ghostEngine.compare(userLat = 55.7320, userLon = 37.5000, elapsedSeconds = 160)
        assertEquals(GhostStatus.AHEAD, r2.status)
        assertTrue("Overtake should trigger vibration", r2.shouldVibrateOvertake)

        // 3. User remains ahead on next check - should NOT vibrate again
        val r3 = ghostEngine.compare(userLat = 55.7320, userLon = 37.5000, elapsedSeconds = 165)
        assertEquals(GhostStatus.AHEAD, r3.status)
        assertFalse("Subsequent ahead check should not trigger overtake vibration", r3.shouldVibrateOvertake)
    }
}
