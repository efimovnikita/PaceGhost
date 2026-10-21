package com.paceghost.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class KalmanLocationFilterTest {

    private lateinit var filter: KalmanLocationFilter

    @Before
    fun setUp() {
        filter = KalmanLocationFilter()
    }

    @Test
    fun `initial update sets state directly`() {
        assertFalse(filter.isInitialized)

        filter.update(55.7500, 37.6000, 5f, 1000L)

        assertTrue(filter.isInitialized)
        assertEquals(55.7500, filter.filteredLat, 0.000001)
        assertEquals(37.6000, filter.filteredLon, 0.000001)
        assertEquals(0f, filter.filteredSpeed, 0.001f)
        assertTrue(filter.filteredAccuracy > 0f)
    }

    @Test
    fun `reset clears all state and flags`() {
        filter.update(55.7500, 37.6000, 5f, 1000L)
        assertTrue(filter.isInitialized)

        filter.reset()

        assertFalse(filter.isInitialized)
        assertEquals(0.0, filter.filteredLat, 0.0)
        assertEquals(0.0, filter.filteredLon, 0.0)
        assertEquals(0f, filter.filteredSpeed, 0f)
    }

    @Test
    fun `filtering smooths noisy measurements`() {
        val baseLat = 55.750000
        val baseLon = 37.600000
        val offsetsMeters = doubleArrayOf(
            0.0, 8.0, -7.0, 9.0, -8.0, 6.0, -9.0, 7.0, -6.0, 8.0,
            -7.0, 5.0, -6.0, 7.0, -5.0, 6.0, -4.0, 5.0, -5.0, 4.0
        )

        val rawErrors = mutableListOf<Double>()
        val filteredErrors = mutableListOf<Double>()

        for (i in offsetsMeters.indices) {
            val offsetDeg = offsetsMeters[i] / KalmanLocationFilter.METERS_PER_DEG_LAT
            val noisyLat = baseLat + offsetDeg
            val t = 1000L + i * 1000L

            filter.update(noisyLat, baseLon, 10f, t)

            rawErrors.add(abs(noisyLat - baseLat))
            filteredErrors.add(abs(filter.filteredLat - baseLat))
        }

        val rawMeanError = rawErrors.average()
        val filteredMeanError = filteredErrors.drop(3).average() // Skip first 3 convergence steps

        assertTrue(
            "Filtered mean error ($filteredMeanError) must be significantly lower than raw mean error ($rawMeanError)",
            filteredMeanError < rawMeanError * 0.65
        )
    }

    @Test
    fun `low accuracy measurement has lower weight than high accuracy measurement`() {
        val filterLowAcc = KalmanLocationFilter()
        val filterHighAcc = KalmanLocationFilter()

        val startLat = 55.750000
        val startLon = 37.600000
        val t0 = 1000L

        // Initialize both at the exact same location
        filterLowAcc.update(startLat, startLon, 5f, t0)
        filterHighAcc.update(startLat, startLon, 5f, t0)

        // Measurement jumps ~111m north
        val jumpLat = 55.751000
        val t1 = 2000L

        filterLowAcc.update(jumpLat, startLon, accuracyMeters = 80f, timestampMs = t1)
        filterHighAcc.update(jumpLat, startLon, accuracyMeters = 5f, timestampMs = t1)

        val shiftLowAcc = abs(filterLowAcc.filteredLat - startLat)
        val shiftHighAcc = abs(filterHighAcc.filteredLat - startLat)

        assertTrue(
            "High accuracy measurement should shift position much more than low accuracy measurement (shiftHigh=$shiftHighAcc, shiftLow=$shiftLowAcc)",
            shiftHighAcc > shiftLowAcc * 2.0
        )
    }

    @Test
    fun `filter tracks motion smoothly without sudden freeze`() {
        val startLat = 55.750000
        val startLon = 37.600000
        val speedMps = 1.4 // ~5 km/h walking speed
        val stepDegPerSec = speedMps / KalmanLocationFilter.METERS_PER_DEG_LAT

        filter.update(startLat, startLon, 5f, 1000L)

        var previousLat = filter.filteredLat
        for (sec in 1..10) {
            val t = 1000L + sec * 1000L
            val trueLat = startLat + sec * stepDegPerSec
            filter.update(trueLat, startLon, 5f, t)

            // Ensure motion progresses steadily
            assertTrue("Filter latitude should steadily advance northward", filter.filteredLat > previousLat)
            previousLat = filter.filteredLat

            // Filter should not freeze (speed must remain positive and reasonable)
            if (sec >= 4) {
                val speed = filter.filteredSpeed
                assertTrue("Speed should be tracked smoothly (~1.4 m/s), was $speed", speed in 0.8f..2.0f)
            }
        }
    }
}
