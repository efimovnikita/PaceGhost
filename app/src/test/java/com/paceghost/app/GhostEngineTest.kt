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
        assertFalse(result.shouldVibrateAheadReminder)
        assertTrue("Off route should trigger voice warning once", result.shouldSpeakOffRoute)
        assertTrue("Off route should trigger vibration warning once", result.shouldVibrateOffRoute)
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
        assertFalse("Reminder voice should not fire immediately upon overtake", r2.shouldSpeakAheadReminder)
        assertFalse("Reminder vibration should not fire immediately upon overtake", r2.shouldVibrateAheadReminder)

        // 3. User remains ahead at elapsed 250s (50s after overtake < 180s cooldown)
        val r3 = longEngine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 250)
        assertEquals(GhostStatus.AHEAD, r3.status)
        assertFalse("Subsequent ahead check should not trigger overtake vibration", r3.shouldVibrateOvertake)
        assertFalse("Subsequent ahead check should not trigger overtake voice", r3.shouldSpeakOvertake)
        assertFalse("Ahead reminder voice should not fire before 180s", r3.shouldSpeakAheadReminder)
        assertFalse("Ahead reminder vibration should not fire before 180s", r3.shouldVibrateAheadReminder)

        // 4. User remains ahead at elapsed 385s (185s after overtake >= 180s cooldown)
        val r4 = longEngine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 385)
        assertEquals(GhostStatus.AHEAD, r4.status)
        assertFalse(r4.shouldSpeakOvertake)
        assertFalse(r4.shouldVibrateOvertake)
        assertTrue("Ahead reminder voice SHOULD fire after 3 minutes", r4.shouldSpeakAheadReminder)
        assertTrue("Ahead reminder vibration SHOULD fire after 3 minutes", r4.shouldVibrateAheadReminder)
    }

    @Test
    fun `off-route warning triggers voice and vibration only once until re-entering track`() {
        // Step 1: User goes off route
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 100)
        assertEquals(GhostStatus.OFF_ROUTE, r1.status)
        assertTrue("First off-route should speak warning", r1.shouldSpeakOffRoute)
        assertTrue("First off-route should vibrate warning", r1.shouldVibrateOffRoute)

        // Step 2: User stays off route 10 seconds later
        val r2 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 110)
        assertEquals(GhostStatus.OFF_ROUTE, r2.status)
        assertFalse("Staying off-route should NOT repeat immediate voice warning", r2.shouldSpeakOffRoute)
        assertFalse("Staying off-route should NOT repeat immediate vibration warning", r2.shouldVibrateOffRoute)

        // Step 3: User returns to route
        val r3 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 120)
        assertEquals(GhostStatus.ON_PACE, r3.status)
        assertFalse(r3.shouldSpeakOffRoute)
        assertFalse(r3.shouldVibrateOffRoute)

        // Step 4: User goes off route again
        val r4 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 130)
        assertEquals(GhostStatus.OFF_ROUTE, r4.status)
        assertTrue("New off-route event after returning should trigger voice warning again", r4.shouldSpeakOffRoute)
        assertTrue("New off-route event after returning should trigger vibration warning again", r4.shouldVibrateOffRoute)
    }

    @Test
    fun `on pace maintains absolute silence`() {
        val r = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 100)
        assertEquals(GhostStatus.ON_PACE, r.status)
        assertFalse(r.shouldVibrateBehind)
        assertFalse(r.shouldVibrateOvertake)
        assertFalse(r.shouldVibrateAheadReminder)
        assertFalse(r.shouldVibrateOffRoute)
        assertFalse(r.shouldSpeakBehind)
        assertFalse(r.shouldSpeakOvertake)
        assertFalse(r.shouldSpeakAheadReminder)
        assertFalse(r.shouldSpeakOffRoute)
    }

    @Test
    fun `custom behind voice cooldown synchronizes both vibration and voice`() {
        // Set cooldown to 30 seconds
        ghostEngine.behindVoiceCooldownSeconds = 30L

        // 1. First trigger at 150s
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 150)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertTrue("First behind trigger should speak", r1.shouldSpeakBehind)
        assertTrue("First behind trigger should vibrate", r1.shouldVibrateBehind)

        // 2. 20s later (elapsed 170s < 30s cooldown) -> should NOT speak or vibrate
        val r2 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 170)
        assertFalse(r2.shouldSpeakBehind)
        assertFalse(r2.shouldVibrateBehind)

        // 3. 30s after first trigger (elapsed 180s >= 30s cooldown) -> SHOULD speak and vibrate
        val r3 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 180)
        assertTrue("Behind trigger after 30s should speak", r3.shouldSpeakBehind)
        assertTrue("Behind trigger after 30s should vibrate", r3.shouldVibrateBehind)
    }

    @Test
    fun `ahead voice disabled suppresses both voice and vibration when ahead`() {
        val pt0 = TrackPoint(lat = 55.7300, lon = 37.5000, elapsedSeconds = 0)
        val pt1 = TrackPoint(lat = 55.7400, lon = 37.5000, elapsedSeconds = 1000)
        val engine = GhostEngine(
            ghostRoute = GeoMath.computeCumulativeDistances(listOf(pt0, pt1)),
            isAheadVoiceEnabled = false
        )

        // Fall behind first
        engine.compare(userLat = 55.7300, userLon = 37.5000, elapsedSeconds = 50)

        // Sprint ahead (overtake event)
        val rOvertake = engine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 200)
        assertEquals(GhostStatus.AHEAD, rOvertake.status)
        assertFalse("Vibration on overtake should be suppressed when isAheadVoiceEnabled is false", rOvertake.shouldVibrateOvertake)
        assertFalse("Voice on overtake should be silenced when isAheadVoiceEnabled is false", rOvertake.shouldSpeakOvertake)
        assertFalse(rOvertake.shouldSpeakAheadReminder)
        assertFalse(rOvertake.shouldVibrateAheadReminder)

        // Long time passes while still ahead (> 180s)
        val rReminder = engine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 500)
        assertEquals(GhostStatus.AHEAD, rReminder.status)
        assertFalse("Ahead reminder voice should be silenced", rReminder.shouldSpeakAheadReminder)
        assertFalse("Ahead reminder vibration should be suppressed", rReminder.shouldVibrateAheadReminder)
    }

    @Test
    fun `custom ahead voice cooldown synchronizes both voice and vibration reminders`() {
        val pt0 = TrackPoint(lat = 55.7300, lon = 37.5000, elapsedSeconds = 0)
        val pt1 = TrackPoint(lat = 55.7400, lon = 37.5000, elapsedSeconds = 1000)
        val engine = GhostEngine(
            ghostRoute = GeoMath.computeCumulativeDistances(listOf(pt0, pt1)),
            aheadVoiceCooldownSeconds = 60L
        )

        // Fall behind first
        engine.compare(userLat = 55.7300, userLon = 37.5000, elapsedSeconds = 50)

        // Overtake at 200s
        val rOvertake = engine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 200)
        assertTrue(rOvertake.shouldSpeakOvertake)
        assertTrue(rOvertake.shouldVibrateOvertake)

        // Check at 240s (40s < 60s cooldown)
        val rEarly = engine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 240)
        assertFalse(rEarly.shouldSpeakAheadReminder)
        assertFalse(rEarly.shouldVibrateAheadReminder)

        // Check at 265s (65s >= 60s cooldown)
        val rDue = engine.compare(userLat = 55.7400, userLon = 37.5000, elapsedSeconds = 265)
        assertTrue("Reminder voice should fire after custom 60s cooldown", rDue.shouldSpeakAheadReminder)
        assertTrue("Reminder vibration should fire after custom 60s cooldown", rDue.shouldVibrateAheadReminder)
    }

    @Test
    fun `single point ghost route triggers off-route vibration and voice when far`() {
        val singlePointEngine = GhostEngine(listOf(TrackPoint(lat = 55.7300, lon = 37.5000, elapsedSeconds = 0)))
        val rOff = singlePointEngine.compare(userLat = 55.7310, userLon = 37.5020, elapsedSeconds = 10)
        assertEquals(GhostStatus.OFF_ROUTE, rOff.status)
        assertTrue(rOff.shouldSpeakOffRoute)
        assertTrue(rOff.shouldVibrateOffRoute)
    }

    @Test
    fun `reset clears alert timestamps and off-route flags`() {
        val r1 = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 150)
        assertEquals(GhostStatus.BEHIND, r1.status)
        assertTrue(r1.shouldVibrateBehind)
        assertTrue(r1.shouldSpeakBehind)

        ghostEngine.reset()

        // After reset, at same timestamp 150s, it should fire again because state was cleared
        val rAfterReset = ghostEngine.compare(userLat = 55.7310, userLon = 37.5000, elapsedSeconds = 150)
        assertEquals(GhostStatus.BEHIND, rAfterReset.status)
        assertTrue(rAfterReset.shouldVibrateBehind)
        assertTrue(rAfterReset.shouldSpeakBehind)
    }

    @Test
    fun `advanceGhostProgress in GPS-denied mode accurately computes pacing without false off-route`() {
        val totalDist = ghostRoute.last().cumulativeDistance
        val halfDist = totalDist / 2.0

        // User advances to half distance at 70s (ghost was at half distance at 100s) -> AHEAD
        val rAhead = ghostEngine.advanceGhostProgress(currentProgressMeters = halfDist, elapsedSeconds = 70)
        assertEquals(GhostStatus.AHEAD, rAhead.status)
        assertFalse("PDR mode should never trigger off-route", rAhead.shouldSpeakOffRoute)
        assertTrue("Distance delta should be positive", rAhead.distanceDeltaMeters > 0)

        // User advances to half distance at 145s -> BEHIND
        val rBehind = ghostEngine.advanceGhostProgress(currentProgressMeters = halfDist, elapsedSeconds = 145)
        assertEquals(GhostStatus.BEHIND, rBehind.status)
        assertFalse("PDR mode should never trigger off-route", rBehind.shouldSpeakOffRoute)
        assertTrue("Distance delta should be negative", rBehind.distanceDeltaMeters < 0)

        // Empty route returns NO_GHOST
        val emptyEngine = GhostEngine(emptyList())
        val rEmpty = emptyEngine.advanceGhostProgress(currentProgressMeters = 50.0, elapsedSeconds = 50)
        assertEquals(GhostStatus.NO_GHOST, rEmpty.status)
    }
}
