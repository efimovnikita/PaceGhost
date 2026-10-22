package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.VoiceSettings
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FreeWalkCoachEngineTest {

    private lateinit var engine: FreeWalkCoachEngine

    @Before
    fun setUp() {
        engine = FreeWalkCoachEngine(
            settings = VoiceSettings(
                targetSpeedKmh = 5.0,
                toleranceKmh = 0.3,
                behindCooldownSeconds = 30L,
                aheadCooldownSeconds = 60L,
                isAheadVoiceEnabled = true
            )
        )
    }

    @Test
    fun `warmup period produces no alerts`() {
        // Target is 5.0 km/h (1.388 m/s). Walk very slowly at 2.0 km/h (0.55 m/s)
        for (sec in 1L..35L) {
            val dist = sec * 0.55
            val event = engine.update(distanceMeters = dist, elapsedSeconds = sec, isPaused = false)
            assertEquals("Should be NONE during warmup at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }
    }

    @Test
    fun `stationary speed below threshold produces no alerts`() {
        // Speed near zero (< 1.5 km/h)
        for (sec in 1L..50L) {
            val dist = sec * 0.1 // 0.36 km/h
            val event = engine.update(distanceMeters = dist, elapsedSeconds = sec, isPaused = false)
            assertEquals("Should be NONE when stopped/stationary", FreeWalkFeedbackEvent.NONE, event)
        }
    }

    @Test
    fun `walking inside tolerance corridor produces no alerts`() {
        // Target 5.0 km/h, tolerance 0.3 km/h -> range [4.7, 5.3].
        // Walk at 5.0 km/h = 1.3888 m/s
        var lastEvent = FreeWalkFeedbackEvent.NONE
        for (sec in 1L..60L) {
            val dist = sec * 1.3888
            val event = engine.update(distanceMeters = dist, elapsedSeconds = sec, isPaused = false)
            if (event != FreeWalkFeedbackEvent.NONE) lastEvent = event
        }
        assertEquals(FreeWalkFeedbackEvent.NONE, lastEvent)
    }

    @Test
    fun `consistently slow walk triggers SPEED_UP right after warmup and debounce`() {
        // Walk slowly from start at 3.5 km/h (0.972 m/s).
        // Warmup is 35s.
        for (sec in 1L..35L) {
            val event = engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
            assertEquals("Warmup should be silent at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 36..39: slow 1..4s (debounce is 5s)
        for (sec in 36L..39L) {
            val event = engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
            assertEquals("Debounce period should be silent at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 40: slow 5s (debounce satisfied!) -> SPEED_UP
        var event = engine.update(distanceMeters = 40 * 0.972, elapsedSeconds = 40L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.SPEED_UP, event)

        // sec 41..69: in cooldown (30s from sec 40 -> next at sec 70)
        for (sec in 41L..69L) {
            event = engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
            assertEquals("Should be in cooldown at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 70 (40 + 30): cooldown expires -> SPEED_UP again
        event = engine.update(distanceMeters = 70 * 0.972, elapsedSeconds = 70L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.SPEED_UP, event)
    }

    @Test
    fun `consistently fast walk triggers GOOD_PACE right after warmup and debounce`() {
        // Walk fast at 6.5 km/h (1.805 m/s)
        for (sec in 1L..35L) {
            engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
        }

        // sec 36..39: debounce
        for (sec in 36L..39L) {
            val event = engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
            assertEquals(FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 40: fast 5s -> GOOD_PACE
        var event = engine.update(distanceMeters = 40 * 1.805, elapsedSeconds = 40L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.GOOD_PACE, event)

        // Cooldown is 60s -> next at 40 + 60 = 100
        for (sec in 41L..99L) {
            event = engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
            assertEquals(FreeWalkFeedbackEvent.NONE, event)
        }

        event = engine.update(distanceMeters = 100 * 1.805, elapsedSeconds = 100L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.GOOD_PACE, event)
    }

    @Test
    fun `anti-ping-pong blocks opposite praise immediately after speed up warning`() {
        // 1. Warmup at slow speed (3.5 km/h) -> triggers SPEED_UP at sec 40
        for (sec in 1L..40L) {
            engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
        }

        // 2. User immediately sprints to 7.0 km/h (1.944 m/s) to catch up
        var currentDist = 40 * 0.972
        var goodPaceFired = false
        // Ahead cooldown is 60s. For the next 55 seconds (up to sec 95), GOOD_PACE must NOT fire!
        for (sec in 41L..95L) {
            currentDist += 1.944
            val event = engine.update(distanceMeters = currentDist, elapsedSeconds = sec, isPaused = false)
            if (event == FreeWalkFeedbackEvent.GOOD_PACE) {
                goodPaceFired = true
                break
            }
        }

        assertEquals("GOOD_PACE must be blocked by aheadCooldown after SPEED_UP to prevent ping-pong", false, goodPaceFired)
    }

    @Test
    fun `anti-ping-pong blocks speed up warning immediately after good pace praise`() {
        // 1. Fast speed triggers GOOD_PACE at sec 40
        for (sec in 1L..40L) {
            engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
        }

        // 2. User slows down to 3.5 km/h. Behind cooldown is 30s.
        var currentDist = 40 * 1.805
        var speedUpFired = false
        // For the next 25 seconds (up to sec 65), SPEED_UP must NOT fire!
        for (sec in 41L..65L) {
            currentDist += 0.972
            val event = engine.update(distanceMeters = currentDist, elapsedSeconds = sec, isPaused = false)
            if (event == FreeWalkFeedbackEvent.SPEED_UP) {
                speedUpFired = true
                break
            }
        }

        assertEquals("SPEED_UP must be blocked by behindCooldown after GOOD_PACE to prevent ping-pong", false, speedUpFired)
    }

    @Test
    fun `paused state resets debounce and suppresses alerts`() {
        for (sec in 1L..35L) {
            engine.update(distanceMeters = sec * 1.3888, elapsedSeconds = sec, isPaused = false)
        }
        val event = engine.update(distanceMeters = 35 * 1.3888 + 5.0, elapsedSeconds = 36L, isPaused = true)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)
    }
}
