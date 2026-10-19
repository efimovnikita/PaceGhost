package com.paceghost.app

import com.paceghost.app.domain.engine.AutoPauseDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoPauseDetectorTest {

    private lateinit var detector: AutoPauseDetector

    @Before
    fun setUp() {
        detector = AutoPauseDetector(speedThresholdKmh = 0.5f, durationThresholdSeconds = 7L)
    }

    @Test
    fun `speed below threshold for less than or equal to 7s does not auto-pause`() {
        // Speed is 0.2 km/h starting at t = 10s
        assertFalse(detector.update(currentSpeedKmh = 0.2f, elapsedSeconds = 10L))
        assertFalse(detector.update(currentSpeedKmh = 0.3f, elapsedSeconds = 13L))
        assertFalse(detector.update(currentSpeedKmh = 0.1f, elapsedSeconds = 17L)) // 7s elapsed
    }

    @Test
    fun `speed below threshold for more than 7s triggers auto-pause`() {
        detector.update(currentSpeedKmh = 0.2f, elapsedSeconds = 10L)
        // At 18s: 8s elapsed (> 7s)
        assertTrue(detector.update(currentSpeedKmh = 0.2f, elapsedSeconds = 18L))
        assertTrue(detector.isAutoPaused())
    }

    @Test
    fun `speed rising back above threshold resumes tracking`() {
        detector.update(currentSpeedKmh = 0.2f, elapsedSeconds = 10L)
        assertTrue(detector.update(currentSpeedKmh = 0.2f, elapsedSeconds = 20L))

        // Speed increases to 3.5 km/h
        assertFalse(detector.update(currentSpeedKmh = 3.5f, elapsedSeconds = 21L))
        assertFalse(detector.isAutoPaused())
    }

    @Test
    fun `reset clears auto-pause state`() {
        detector.update(currentSpeedKmh = 0.1f, elapsedSeconds = 10L)
        detector.update(currentSpeedKmh = 0.1f, elapsedSeconds = 20L)
        assertTrue(detector.isAutoPaused())

        detector.reset()
        assertFalse(detector.isAutoPaused())
    }
}
