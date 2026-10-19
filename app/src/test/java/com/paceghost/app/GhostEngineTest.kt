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
        assertTrue("Off route should trigger voice warning once", result.shouldSpeakOffRoute)
    }

    @Test
    fun `anti-spam cooldown prevents repeated behind vibration and voice within 60s`() {
        // First BEHIND comparison at elapsed 150s
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 150)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertTrue("First behind trigger should vibrate", r1.shouldVibrateBehind)
        assertTrue("First behind trigger should speak", r1.shouldSpeakBehind)

        // Second comparison at elapsed 170s (20s later < 60s cooldown)
        val r2 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 170)
        assertEquals(GhostStatus.BEHIND, r2.status)
        assertFalse("Second behind trigger within 60s should NOT vibrate", r2.shouldVibrateBehind)
        assertFalse("Second behind trigger within 60s should NOT speak", r2.shouldSpeakBehind)

        // Third comparison at elapsed 235s (65s later >= 60s cooldown)
        val r3 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 235)
        assertEquals(GhostStatus.BEHIND, r3.status)
        assertTrue("Third behind trigger after 60s cooldown SHOULD vibrate", r3.shouldVibrateBehind)
        assertTrue("Third behind trigger after 60s cooldown SHOULD speak", r3.shouldSpeakBehind)
    }

    @Test
    fun `transition from BEHIND to AHEAD triggers overtake vibration and voice, followed by 3min reminder`() {
        // Track where ghost takes 1000s from p0 to p1
        val pt0 = TrackPoint(lat = 55.7300, lon = 37.5000, elapsedSeconds = 0)
        val pt1 = TrackPoint(lat = 55.7400, lon = 37.5000, elapsedSeconds = 1000)
        val longEngine = GhostEngine(GeoMath.computeCumulativeDistances(listOf(pt0, pt1)))

        // 1. User falls behind at start (elapsed 50s at start position: timeDelta = +50s > 30s)
        val r1 = longEngine.compare(userLat = 55.7300, userLon = 37.5000, elapsedSeconds = 50)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertFalse(r1.shouldVibrateOvertake)
        assertFalse(r1.shouldSpeakOvertake)

        // 2. User sprints ahead to pt1 at elapsed 200s (ghost is scheduled at 1000s -> timeDelta = -800s)
        val r2 = longEngine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 200)
        assertEquals(GhostStatus.AHEAD, r2.status)
        assertTrue("Overtake should trigger vibration", r2.shouldVibrateOvertake)
        assertTrue("Overtake should trigger voice", r2.shouldSpeakOvertake)
        assertFalse("Reminder should not fire immediately upon overtake", r2.shouldSpeakAheadReminder)

        // 3. User remains ahead at elapsed 250s (50s after overtake < 180s cooldown)
        val r3 = longEngine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 250)
        assertEquals(GhostStatus.AHEAD, r3.status)
        assertFalse("Subsequent ahead check should not trigger overtake vibration", r3.shouldVibrateOvertake)
        assertFalse("Subsequent ahead check should not trigger overtake voice", r3.shouldSpeakOvertake)
        assertFalse("Ahead reminder should not fire before 180s", r3.shouldSpeakAheadReminder)

        // 4. User remains ahead at elapsed 385s (185s after overtake >= 180s cooldown)
        val r4 = longEngine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 385)
        assertEquals(GhostStatus.AHEAD, r4.status)
        assertFalse(r4.shouldSpeakOvertake)
        assertTrue("Ahead reminder SHOULD fire after 3 minutes", r4.shouldSpeakAheadReminder)
    }

    @Test
    fun `off-route voice warning triggers only once until re-entering track`() {
        // Step 1: User goes off route
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 100)
        assertEquals(GhostStatus.OFF_ROUTE, r1.status)
        assertTrue("First off-route should speak warning", r1.shouldSpeakOffRoute)

        // Step 2: User stays off route 10 seconds later
        val r2 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 110)
        assertEquals(GhostStatus.OFF_ROUTE, r2.status)
        assertFalse("Staying off-route should NOT repeat immediate warning", r2.shouldSpeakOffRoute)

        // Step 3: User returns to route
        val r3 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 120)
        assertEquals(GhostStatus.ON_PACE, r3.status)
        assertFalse(r3.shouldSpeakOffRoute)

        // Step 4: User goes off route again
        val r4 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 130)
        assertEquals(GhostStatus.OFF_ROUTE, r4.status)
        assertTrue("New off-route event after returning should trigger warning again", r4.shouldSpeakOffRoute)
    }

    @Test
    fun `on pace maintains absolute silence`() {
        val r = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 100)
        assertEquals(GhostStatus.ON_PACE, r.status)
        assertFalse(r.shouldVibrateBehind)
        assertFalse(r.shouldVibrateOvertake)
        assertFalse(r.shouldSpeakBehind)
        assertFalse(r.shouldSpeakOvertake)
        assertFalse(r.shouldSpeakAheadReminder)
        assertFalse(r.shouldSpeakOffRoute)
    }
}
