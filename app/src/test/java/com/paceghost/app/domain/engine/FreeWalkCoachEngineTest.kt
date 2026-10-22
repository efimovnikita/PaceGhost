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
        var event = FreeWalkFeedbackEvent.NONE
        for (sec in 1L..14L) {
            val dist = sec * 0.55
            event = engine.update(distanceMeters = dist, elapsedSeconds = sec, isPaused = false)
            assertEquals("Should be NONE during warmup at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }
    }

    @Test
    fun `stationary speed below threshold produces no alerts`() {
        // Speed near zero (< 1.5 km/h)
        for (sec in 1L..25L) {
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
        for (sec in 1L..40L) {
            val dist = sec * 1.3888
            val event = engine.update(distanceMeters = dist, elapsedSeconds = sec, isPaused = false)
            if (event != FreeWalkFeedbackEvent.NONE) lastEvent = event
        }
        assertEquals(FreeWalkFeedbackEvent.NONE, lastEvent)
    }

    @Test
    fun `consistently slow walk triggers SPEED_UP right after warmup and debounce`() {
        // Walk slowly from start at 3.5 km/h (0.972 m/s).
        // Warmup ends at sec 15.
        for (sec in 1L..15L) {
            val event = engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
            assertEquals("Warmup should be silent at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 16: slow 1s
        var event = engine.update(distanceMeters = 16 * 0.972, elapsedSeconds = 16L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)

        // sec 17: slow 2s
        event = engine.update(distanceMeters = 17 * 0.972, elapsedSeconds = 17L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)

        // sec 18: slow 3s (debounce satisfied!) -> SPEED_UP
        event = engine.update(distanceMeters = 18 * 0.972, elapsedSeconds = 18L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.SPEED_UP, event)

        // sec 19..47: in cooldown (30s)
        for (sec in 19L..47L) {
            event = engine.update(distanceMeters = sec * 0.972, elapsedSeconds = sec, isPaused = false)
            assertEquals("Should be in cooldown at sec=$sec", FreeWalkFeedbackEvent.NONE, event)
        }

        // sec 48 (18 + 30): cooldown expires -> SPEED_UP again
        event = engine.update(distanceMeters = 48 * 0.972, elapsedSeconds = 48L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.SPEED_UP, event)
    }

    @Test
    fun `consistently fast walk triggers GOOD_PACE right after warmup and debounce`() {
        // Walk fast at 6.5 km/h (1.805 m/s)
        for (sec in 1L..15L) {
            engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
        }

        // sec 16: fast 1s
        var event = engine.update(distanceMeters = 16 * 1.805, elapsedSeconds = 16L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)

        // sec 17: fast 2s
        event = engine.update(distanceMeters = 17 * 1.805, elapsedSeconds = 17L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)

        // sec 18: fast 3s (debounce satisfied!) -> GOOD_PACE
        event = engine.update(distanceMeters = 18 * 1.805, elapsedSeconds = 18L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.GOOD_PACE, event)

        // Cooldown is 60s -> next at 18 + 60 = 78
        for (sec in 19L..77L) {
            event = engine.update(distanceMeters = sec * 1.805, elapsedSeconds = sec, isPaused = false)
            assertEquals(FreeWalkFeedbackEvent.NONE, event)
        }

        event = engine.update(distanceMeters = 78 * 1.805, elapsedSeconds = 78L, isPaused = false)
        assertEquals(FreeWalkFeedbackEvent.GOOD_PACE, event)
    }

    @Test
    fun `transition from target speed to slow speed triggers SPEED_UP after rolling window drops`() {
        var distance = 0.0
        // Walk at target speed (5.0 km/h = 1.3888 m/s) for 25 seconds
        for (sec in 1L..25L) {
            distance += 1.3888
            engine.update(distanceMeters = distance, elapsedSeconds = sec, isPaused = false)
        }

        // Now drop to 3.0 km/h = 0.8333 m/s
        var triggeredSec = -1L
        for (sec in 26L..50L) {
            distance += 0.8333
            val event = engine.update(distanceMeters = distance, elapsedSeconds = sec, isPaused = false)
            if (event == FreeWalkFeedbackEvent.SPEED_UP) {
                triggeredSec = sec
                break
            }
        }

        // Should trigger within a few seconds after the rolling window crosses the lower threshold
        assert(triggeredSec in 29L..36L) { "Expected SPEED_UP to trigger between sec 29 and 36, but got $triggeredSec" }
    }

    @Test
    fun `paused state resets debounce and suppresses alerts`() {
        for (sec in 1L..15L) {
            engine.update(distanceMeters = sec * 1.3888, elapsedSeconds = sec, isPaused = false)
        }
        val event = engine.update(distanceMeters = 15 * 1.3888 + 5.0, elapsedSeconds = 16L, isPaused = true)
        assertEquals(FreeWalkFeedbackEvent.NONE, event)
    }
}
