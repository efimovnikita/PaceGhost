package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.GpsQuality
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.domain.model.WalkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PositioningPipelineTest {

    private lateinit var kalmanFilter: KalmanLocationFilter
    private lateinit var pdrEngine: PedestrianDeadReckoningEngine

    @Before
    fun setUp() {
        kalmanFilter = KalmanLocationFilter()
        pdrEngine = PedestrianDeadReckoningEngine()
    }

    @Test
    fun `walkState has backward-compatible defaults for gpsQuality and accuracy`() {
        val state = WalkState()
        assertEquals(GpsQuality.GOOD, state.gpsQuality)
        assertEquals(0f, state.currentAccuracyMeters, 0.001f)
    }

    @Test
    fun `gpsQuality categorization matches accuracy thresholds`() {
        fun determineQuality(accuracy: Float): GpsQuality = when {
            accuracy <= 15f -> GpsQuality.GOOD
            accuracy <= 45f -> GpsQuality.WEAK
            else -> GpsQuality.PDR_ACTIVE
        }

        assertEquals(GpsQuality.GOOD, determineQuality(5f))
        assertEquals(GpsQuality.GOOD, determineQuality(15f))
        assertEquals(GpsQuality.WEAK, determineQuality(15.1f))
        assertEquals(GpsQuality.WEAK, determineQuality(30f))
        assertEquals(GpsQuality.WEAK, determineQuality(45f))
        assertEquals(GpsQuality.PDR_ACTIVE, determineQuality(45.1f))
        assertEquals(GpsQuality.PDR_ACTIVE, determineQuality(80f))
    }

    @Test
    fun `pdr propagation creates valid trackpoint with pdr flag and accuracy`() {
        val lastPoint = TrackPoint(
            lat = 55.7500,
            lon = 37.6000,
            cumulativeDistance = 500.0,
            stepCount = 600,
            timestamp = 10000L
        )

        val deltaSteps = 10
        val heading = 90f // East
        val now = 15000L
        val addedDist = deltaSteps * pdrEngine.currentStrideLengthMeters
        val newDistance = lastPoint.cumulativeDistance + addedDist

        val propagated = pdrEngine.propagatePoint(
            lastPoint = lastPoint,
            deltaSteps = deltaSteps,
            headingAzimuthDeg = heading,
            timestampMs = now
        ).copy(
            cumulativeDistance = newDistance,
            stepCount = lastPoint.stepCount + deltaSteps,
            isPdr = true,
            accuracy = 25f
        )

        assertTrue(propagated.isPdr)
        assertEquals(25f, propagated.accuracy, 0.01f)
        assertEquals(610, propagated.stepCount)
        assertEquals(507.5, propagated.cumulativeDistance, 0.01)
        assertTrue(propagated.lon > lastPoint.lon)
    }

    @Test
    fun `kinematic check accepts valid human steps and rejects rogue teleport`() {
        // Valid: 8m in 10 steps
        assertTrue(pdrEngine.isKinematicallyPlausible(deltaDistMeters = 8.0, deltaSteps = 10, dtSec = 5.0))

        // Rogue jump: 200m in 0 steps
        assertFalse(pdrEngine.isKinematicallyPlausible(deltaDistMeters = 200.0, deltaSteps = 0, dtSec = 1.0))

        // Rogue jump: 100m in 2 steps
        assertFalse(pdrEngine.isKinematicallyPlausible(deltaDistMeters = 100.0, deltaSteps = 2, dtSec = 1.0))
    }

    @Test
    fun `kalman filter updates position and provides filtered coordinates`() {
        kalmanFilter.update(55.7500, 37.6000, 10f, 1000L)
        assertTrue(kalmanFilter.isInitialized)
        assertEquals(55.7500, kalmanFilter.filteredLat, 0.0001)
        assertEquals(37.6000, kalmanFilter.filteredLon, 0.0001)

        kalmanFilter.update(55.7501, 37.6000, 10f, 2000L)
        assertTrue(kalmanFilter.filteredLat > 55.7500)
        assertTrue(kalmanFilter.filteredAccuracy > 0f)
    }

    @Test
    fun `stride calibration updates when gps accuracy is reliable`() {
        val initialStride = pdrEngine.currentStrideLengthMeters
        assertEquals(0.75, initialStride, 0.001)

        // 80 meters over 100 steps with reliable accuracy
        pdrEngine.onGpsDistanceProgress(deltaDistMeters = 80.0, deltaSteps = 100)
        assertEquals(0.80, pdrEngine.currentStrideLengthMeters, 0.001)

        // Reset restores initial stride
        pdrEngine.reset()
        assertEquals(0.75, pdrEngine.currentStrideLengthMeters, 0.001)
    }
}
