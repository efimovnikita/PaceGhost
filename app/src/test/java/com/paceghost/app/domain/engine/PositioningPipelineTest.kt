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
            accuracy <= 30f -> GpsQuality.WEAK
            else -> GpsQuality.PDR_ACTIVE
        }

        assertEquals(GpsQuality.GOOD, determineQuality(5f))
        assertEquals(GpsQuality.GOOD, determineQuality(15f))
        assertEquals(GpsQuality.WEAK, determineQuality(15.1f))
        assertEquals(GpsQuality.WEAK, determineQuality(25f))
        assertEquals(GpsQuality.WEAK, determineQuality(30f))
        assertEquals(GpsQuality.PDR_ACTIVE, determineQuality(30.1f))
        assertEquals(GpsQuality.PDR_ACTIVE, determineQuality(45f))
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

    @Test
    fun `kalman filtered distance accumulation dampens raw GPS multipath noise`() {
        val baseLat = 55.750000
        val baseLon = 37.600000

        kalmanFilter.update(baseLat, baseLon, 10f, 1000L)

        // Raw noisy zig-zag points (+5m, -5m, +5m, -5m)
        val rawOffsetsMeters = doubleArrayOf(5.0, -5.0, 5.0, -5.0)
        var rawCumulativeDist = 0.0
        var filteredCumulativeDist = 0.0

        var prevRawLat = baseLat
        var prevFilteredLat = kalmanFilter.filteredLat

        for (i in rawOffsetsMeters.indices) {
            val offsetDeg = rawOffsetsMeters[i] / KalmanLocationFilter.METERS_PER_DEG_LAT
            val noisyLat = baseLat + offsetDeg
            val t = 2000L + i * 2000L

            rawCumulativeDist += GeoMath.haversineDistance(prevRawLat, baseLon, noisyLat, baseLon)
            prevRawLat = noisyLat

            kalmanFilter.update(noisyLat, baseLon, 12f, t)
            val currFilteredLat = kalmanFilter.filteredLat
            filteredCumulativeDist += GeoMath.haversineDistance(prevFilteredLat, baseLon, currFilteredLat, baseLon)
            prevFilteredLat = currFilteredLat
        }

        assertTrue(
            "Filtered distance ($filteredCumulativeDist) must be significantly lower than raw noisy distance ($rawCumulativeDist)",
            filteredCumulativeDist < rawCumulativeDist * 0.70
        )
    }

    @Test
    fun `underpass bridge mode maintains clean track points and validates kinematic gate on exit`() {
        // Entrance to underpass: real GPS point
        val entrancePoint = TrackPoint(
            lat = 55.64347,
            lon = 37.46928,
            cumulativeDistance = 3850.0,
            stepCount = 4700,
            timestamp = 100000L
        )
        val initialPoints = listOf(entrancePoint)

        // Ghost route which was far away on this distance in another area
        val farGhostRoute = listOf(
            TrackPoint(lat = 55.6546, lon = 37.4826, cumulativeDistance = 0.0, elapsedSeconds = 0),
            TrackPoint(lat = 55.6484, lon = 37.4649, cumulativeDistance = 3850.0, elapsedSeconds = 2200),
            TrackPoint(lat = 55.6535, lon = 37.4895, cumulativeDistance = 7100.0, elapsedSeconds = 3900)
        )
        val ghostEngine = GhostEngine(farGhostRoute)

        // Inside underpass: 52 steps taken, 42.6m accumulated
        val deltaSteps = 52
        val currentSteps = entrancePoint.stepCount + deltaSteps
        val addedDist = deltaSteps * 0.82
        val newDistance = entrancePoint.cumulativeDistance + addedDist

        // In Bridge Mode: ghost pacing is updated, but currentPoints remains untouched (no false injection)
        val ghostResult = ghostEngine.advanceGhostProgress(
            currentProgressMeters = newDistance,
            elapsedSeconds = 2230
        )
        assertFalse(ghostResult.shouldSpeakOffRoute)

        // Track points remain clean with entrancePoint as the last point
        val pointsDuringPdr = initialPoints
        assertEquals(1, pointsDuringPdr.size)
        assertEquals(entrancePoint, pointsDuringPdr.last())

        // Exit underpass on the opposite side of the avenue: real GPS fix
        val exitLat = 55.64322
        val exitLon = 37.46963
        val directCrossingDist = GeoMath.haversineDistance(
            entrancePoint.lat,
            entrancePoint.lon,
            exitLat,
            exitLon
        )

        // Physical underpass crossing width is ~35.4m
        assertTrue(directCrossingDist in 30.0..45.0)

        // Kinematic gate validates step-to-distance plausibility immediately on the first fix
        val stepsMadeDuringBlackout = currentSteps - pointsDuringPdr.last().stepCount
        assertEquals(52, stepsMadeDuringBlackout)
        assertTrue(
            "Underpass crossing must pass kinematic gate immediately on exit",
            pdrEngine.isKinematicallyPlausible(
                deltaDistMeters = directCrossingDist,
                deltaSteps = stepsMadeDuringBlackout,
                dtSec = 25.0
            )
        )
    }
}
