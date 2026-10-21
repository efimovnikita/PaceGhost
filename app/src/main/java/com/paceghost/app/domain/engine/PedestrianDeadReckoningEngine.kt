package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.TrackPoint

/**
 * Pedestrian Dead Reckoning (PDR) engine.
 *
 * Provides:
 * 1. Dynamic stride length calibration during reliable GPS reception.
 * 2. Dead reckoning propagation using step counts, heading azimuth, and geodetic forward projection.
 * 3. Kinematic gating to reject GPS teleportation and spoofing anomalies.
 */
class PedestrianDeadReckoningEngine(
    initialStrideMeters: Double = DEFAULT_STRIDE_METERS
) {

    companion object {
        const val DEFAULT_STRIDE_METERS = 0.75
        const val MIN_STRIDE_METERS = 0.50
        const val MAX_STRIDE_METERS = 1.10
        const val RELIABLE_ACCURACY_THRESHOLD_METERS = 20.0f
        const val MAX_STRIDE_SPEED_METERS_PER_STEP = 1.25
        const val KINEMATIC_BUFFER_METERS = 25.0
    }

    var currentStride: Double = initialStrideMeters.coerceIn(MIN_STRIDE_METERS, MAX_STRIDE_METERS)
        private set

    val currentStrideLengthMeters: Double
        get() = currentStride

    var accumulatedReliableDistance: Double = 0.0
        private set

    var accumulatedReliableSteps: Int = 0
        private set

    /**
     * Updates stride calibration with accumulated reliable GPS movements.
     * Only updates when [accuracyMeters] is within reliable threshold (<= 20m).
     *
     * @return Clamped stride length in meters.
     */
    fun updateCalibration(deltaDistMeters: Double, deltaSteps: Int, accuracyMeters: Float): Double {
        if (accuracyMeters <= RELIABLE_ACCURACY_THRESHOLD_METERS && deltaSteps > 0 && deltaDistMeters > 0.0) {
            accumulatedReliableDistance += deltaDistMeters
            accumulatedReliableSteps += deltaSteps
            val calculatedStride = accumulatedReliableDistance / accumulatedReliableSteps
            currentStride = calculatedStride.coerceIn(MIN_STRIDE_METERS, MAX_STRIDE_METERS)
        }
        return currentStride
    }

    /**
     * Direct stride calibration calculation clamped between [MIN_STRIDE_METERS] and [MAX_STRIDE_METERS].
     */
    fun calibrateStride(deltaDistMeters: Double, deltaSteps: Int): Double {
        if (deltaSteps <= 0 || deltaDistMeters <= 0.0) return currentStride
        val computed = deltaDistMeters / deltaSteps
        currentStride = computed.coerceIn(MIN_STRIDE_METERS, MAX_STRIDE_METERS)
        return currentStride
    }

    /**
     * Updates stride calibration using reliable GPS progress (accuracy <= 20m).
     */
    fun onGpsDistanceProgress(deltaDistMeters: Double, deltaSteps: Int): Double {
        return updateCalibration(deltaDistMeters, deltaSteps, RELIABLE_ACCURACY_THRESHOLD_METERS)
    }

    /**
     * Resets calibration state back to defaults.
     */
    fun resetCalibration(strideMeters: Double = DEFAULT_STRIDE_METERS) {
        accumulatedReliableDistance = 0.0
        accumulatedReliableSteps = 0
        currentStride = strideMeters.coerceIn(MIN_STRIDE_METERS, MAX_STRIDE_METERS)
    }

    /**
     * Alias for [resetCalibration].
     */
    fun reset(strideMeters: Double = DEFAULT_STRIDE_METERS) {
        resetCalibration(strideMeters)
    }

    /**
     * Propagates starting position ([lat], [lon]) forward by [stepCount] steps
     * in the direction of [headingAzimuthDeg] using forward geodetic projection.
     *
     * @return Pair of updated (latitude, longitude) coordinates.
     */
    fun propagate(
        lat: Double,
        lon: Double,
        stepCount: Int,
        headingAzimuthDeg: Float,
        strideLength: Double = currentStride
    ): Pair<Double, Double> {
        if (stepCount <= 0) return Pair(lat, lon)
        val distanceMeters = stepCount * strideLength
        return GeoMath.forwardProjection(lat, lon, distanceMeters, headingAzimuthDeg.toDouble())
    }

    /**
     * Propagates a [TrackPoint] by [deltaSteps] in the direction of [headingAzimuthDeg],
     * marking the resulting point with isPdr = true.
     */
    fun propagatePoint(
        lastPoint: TrackPoint,
        deltaSteps: Int,
        headingAzimuthDeg: Float,
        timestampMs: Long = System.currentTimeMillis(),
        strideLength: Double = currentStride
    ): TrackPoint {
        val (newLat, newLon) = propagate(
            lastPoint.lat,
            lastPoint.lon,
            deltaSteps,
            headingAzimuthDeg,
            strideLength
        )
        val deltaDist = deltaSteps.coerceAtLeast(0) * strideLength
        val dtSec = if (timestampMs > lastPoint.timestamp) {
            (timestampMs - lastPoint.timestamp) / 1000L
        } else 1L
        val speed = if (dtSec > 0) (deltaDist / dtSec).toFloat() else 0f

        return TrackPoint(
            lat = newLat,
            lon = newLon,
            altitude = lastPoint.altitude,
            timestamp = timestampMs,
            elapsedSeconds = lastPoint.elapsedSeconds + dtSec,
            cumulativeDistance = lastPoint.cumulativeDistance + deltaDist,
            speed = speed,
            stepCount = lastPoint.stepCount + deltaSteps.coerceAtLeast(0),
            isPdr = true,
            accuracy = lastPoint.accuracy
        )
    }

    /**
     * Kinematic gating check: determines whether a GPS jump is physically plausible for human walking.
     *
     * Checks whether [deltaDistMeters] <= (deltaSteps * 1.25 + 25.0).
     * If false, it's flagged as an outlier/spoofing jump.
     */
    fun isKinematicallyPlausible(
        deltaDistMeters: Double,
        deltaSteps: Int,
        dtSec: Double = 0.0
    ): Boolean {
        val maxAllowed = deltaSteps.coerceAtLeast(0) * MAX_STRIDE_SPEED_METERS_PER_STEP + KINEMATIC_BUFFER_METERS
        return deltaDistMeters <= maxAllowed
    }
}
