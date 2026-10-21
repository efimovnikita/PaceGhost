package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PedestrianDeadReckoningEngineTest {

    private lateinit var pdrEngine: PedestrianDeadReckoningEngine

    @Before
    fun setUp() {
        pdrEngine = PedestrianDeadReckoningEngine()
    }

    @Test
    fun `default stride length is 75 cm`() {
        assertEquals(0.75, pdrEngine.currentStride, 0.001)
    }

    @Test
    fun `stride calibration calculates and updates current stride`() {
        // 80 meters over 100 steps = 0.80m per step
        val stride = pdrEngine.calibrateStride(80.0, 100)
        assertEquals(0.80, stride, 0.001)
        assertEquals(0.80, pdrEngine.currentStride, 0.001)
    }

    @Test
    fun `stride calibration clamps to minimum 0_50m and maximum 1_10m`() {
        // Lower bound clamp (30m / 100 steps = 0.30m -> should clamp to 0.50m)
        val clampedLow = pdrEngine.calibrateStride(30.0, 100)
        assertEquals(PedestrianDeadReckoningEngine.MIN_STRIDE_METERS, clampedLow, 0.001)
        assertEquals(0.50, pdrEngine.currentStride, 0.001)

        // Upper bound clamp (150m / 100 steps = 1.50m -> should clamp to 1.10m)
        val clampedHigh = pdrEngine.calibrateStride(150.0, 100)
        assertEquals(PedestrianDeadReckoningEngine.MAX_STRIDE_METERS, clampedHigh, 0.001)
        assertEquals(1.10, pdrEngine.currentStride, 0.001)
    }

    @Test
    fun `updateCalibration ignores unreliable GPS reception`() {
        val initialStride = pdrEngine.currentStride

        // Unreliable GPS fix (> 20m) should NOT alter calibration
        pdrEngine.updateCalibration(deltaDistMeters = 100.0, deltaSteps = 100, accuracyMeters = 35.0f)
        assertEquals(initialStride, pdrEngine.currentStride, 0.001)
        assertEquals(0, pdrEngine.accumulatedReliableSteps)

        // Reliable GPS fix (<= 20m) should update calibration
        pdrEngine.updateCalibration(deltaDistMeters = 85.0, deltaSteps = 100, accuracyMeters = 8.0f)
        assertEquals(0.85, pdrEngine.currentStride, 0.001)
        assertEquals(100, pdrEngine.accumulatedReliableSteps)
    }

    @Test
    fun `resetCalibration and reset alias restore default stride`() {
        pdrEngine.calibrateStride(100.0, 100)
        assertEquals(1.00, pdrEngine.currentStride, 0.001)
        assertEquals(1.00, pdrEngine.currentStrideLengthMeters, 0.001)

        pdrEngine.reset()
        assertEquals(0.75, pdrEngine.currentStride, 0.001)
        assertEquals(0.75, pdrEngine.currentStrideLengthMeters, 0.001)
        assertEquals(0.0, pdrEngine.accumulatedReliableDistance, 0.001)
        assertEquals(0, pdrEngine.accumulatedReliableSteps)
    }

    @Test
    fun `onGpsDistanceProgress updates calibration and currentStrideLengthMeters`() {
        val updated = pdrEngine.onGpsDistanceProgress(deltaDistMeters = 90.0, deltaSteps = 100)
        assertEquals(0.90, updated, 0.001)
        assertEquals(0.90, pdrEngine.currentStrideLengthMeters, 0.001)
    }

    @Test
    fun `dead reckoning produces expected forward coordinates along heading 0 North`() {
        val startLat = 55.750000
        val startLon = 37.600000
        val steps = 100
        val stride = 0.75 // Expected distance: 75.0 meters

        val (northLat, northLon) = pdrEngine.propagate(startLat, startLon, steps, 0f, stride)

        val traveledDist = GeoMath.haversineDistance(startLat, startLon, northLat, northLon)
        assertEquals(75.0, traveledDist, 0.5)
        assertTrue("Heading North should increase latitude", northLat > startLat)
        assertEquals(startLon, northLon, 0.0001)
    }

    @Test
    fun `dead reckoning produces expected forward coordinates along heading 90 East`() {
        val startLat = 55.750000
        val startLon = 37.600000
        val steps = 100
        val stride = 0.75

        val (eastLat, eastLon) = pdrEngine.propagate(startLat, startLon, steps, 90f, stride)

        val traveledDist = GeoMath.haversineDistance(startLat, startLon, eastLat, eastLon)
        assertEquals(75.0, traveledDist, 0.5)
        assertTrue("Heading East should increase longitude", eastLon > startLon)
        assertEquals(startLat, eastLat, 0.0001)
    }

    @Test
    fun `dead reckoning propagates along South and West bearings`() {
        val startLat = 55.750000
        val startLon = 37.600000

        // South = 180 degrees
        val (southLat, _) = pdrEngine.propagate(startLat, startLon, 50, 180f, 1.0)
        assertTrue("Heading South should decrease latitude", southLat < startLat)

        // West = 270 degrees
        val (_, westLon) = pdrEngine.propagate(startLat, startLon, 50, 270f, 1.0)
        assertTrue("Heading West should decrease longitude", westLon < startLon)
    }

    @Test
    fun `propagatePoint creates new TrackPoint with isPdr flag set to true`() {
        val initialPoint = TrackPoint(
            lat = 55.7500,
            lon = 37.6000,
            cumulativeDistance = 100.0,
            stepCount = 120,
            timestamp = 1000L
        )

        val pdrPoint = pdrEngine.propagatePoint(
            lastPoint = initialPoint,
            deltaSteps = 10,
            headingAzimuthDeg = 0f,
            timestampMs = 6000L,
            strideLength = 0.8
        )

        assertTrue(pdrPoint.isPdr)
        assertEquals(130, pdrPoint.stepCount)
        assertEquals(108.0, pdrPoint.cumulativeDistance, 0.01)
        assertEquals(6000L, pdrPoint.timestamp)
        assertTrue(pdrPoint.speed > 0f)
    }

    @Test
    fun `kinematic gate correctly rejects 150m jumps when steps == 0 or 2, and accepts normal pedestrian movements`() {
        // Rejects 150m jump with 0 steps (max allowed: 25m)
        assertFalse(
            "150m jump with 0 steps must be rejected",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 150.0, deltaSteps = 0)
        )

        // Rejects 150m jump with 2 steps (max allowed: 2 * 1.25 + 25 = 27.5m)
        assertFalse(
            "150m jump with 2 steps must be rejected",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 150.0, deltaSteps = 2)
        )

        // Accepts GPS jitter / small drift within buffer (15m with 0 steps)
        assertTrue(
            "15m jitter with 0 steps should be accepted within buffer",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 15.0, deltaSteps = 0)
        )

        // Accepts normal pedestrian movement (8m with 10 steps)
        assertTrue(
            "8m movement with 10 steps should be accepted",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 8.0, deltaSteps = 10)
        )

        // Accepts brisk walking (35m with 25 steps; max allowed = 25 * 1.25 + 25 = 56.25m)
        assertTrue(
            "35m movement with 25 steps should be accepted",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 35.0, deltaSteps = 25)
        )

        // Rejects teleportation with modest step count (90m with 10 steps; max allowed = 37.5m)
        assertFalse(
            "90m jump with 10 steps must be rejected",
            pdrEngine.isKinematicallyPlausible(deltaDistMeters = 90.0, deltaSteps = 10)
        )
    }
}
