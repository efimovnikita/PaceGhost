package com.paceghost.app.domain.engine

import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * 2D Kinematic Kalman Filter tracking position (latitude, longitude) and velocity (deg/s).
 *
 * Implements a constant velocity kinematic model with continuous white noise acceleration (CWNA)
 * process noise tuned for pedestrian walking dynamics.
 */
class KalmanLocationFilter(
    private val accelerationStdMetersPerSec2: Double = DEFAULT_ACCELERATION_STD
) {

    companion object {
        const val METERS_PER_DEG_LAT = 111320.0
        const val DEFAULT_ACCELERATION_STD = 1.0 // m/s^2 typical pedestrian acceleration noise
    }

    private var lat: Double = 0.0
    private var lon: Double = 0.0
    private var vLat: Double = 0.0 // degrees per second
    private var vLon: Double = 0.0 // degrees per second

    // Covariance matrix for Latitude [ [p00, p01], [p01, p11] ]
    private var p00Lat: Double = 0.0
    private var p01Lat: Double = 0.0
    private var p11Lat: Double = 0.0

    // Covariance matrix for Longitude [ [p00, p01], [p01, p11] ]
    private var p00Lon: Double = 0.0
    private var p01Lon: Double = 0.0
    private var p11Lon: Double = 0.0

    private var lastTimestampMs: Long = 0L
    var isInitialized: Boolean = false
        private set

    val filteredLat: Double
        get() = lat

    val filteredLon: Double
        get() = lon

    val filteredAccuracy: Float
        get() = if (isInitialized) {
            val metersPerDegLon = getMetersPerDegLon(lat)
            val latStdMeters = sqrt(p00Lat.coerceAtLeast(0.0)) * METERS_PER_DEG_LAT
            val lonStdMeters = sqrt(p00Lon.coerceAtLeast(0.0)) * metersPerDegLon
            sqrt((latStdMeters * latStdMeters + lonStdMeters * lonStdMeters) / 2.0).toFloat()
        } else 0f

    val filteredSpeed: Float
        get() = if (isInitialized) {
            val metersPerDegLon = getMetersPerDegLon(lat)
            val vLatMetersPerSec = vLat * METERS_PER_DEG_LAT
            val vLonMetersPerSec = vLon * metersPerDegLon
            sqrt(vLatMetersPerSec * vLatMetersPerSec + vLonMetersPerSec * vLonMetersPerSec).toFloat()
        } else 0f

    // Property aliases for convenience
    val latitude: Double get() = filteredLat
    val longitude: Double get() = filteredLon
    val accuracy: Float get() = filteredAccuracy
    val speed: Float get() = filteredSpeed

    /**
     * Projects current state forward by [dtSec] seconds using kinematic model.
     */
    fun predict(dtSec: Double) {
        if (!isInitialized || dtSec <= 0.0) return
        val dt = dtSec.coerceAtMost(30.0)

        // Kinematic state extrapolation: p = p + v * dt
        lat += vLat * dt
        lon += vLon * dt

        val metersPerDegLon = getMetersPerDegLon(lat)
        val sigmaALat = accelerationStdMetersPerSec2 / METERS_PER_DEG_LAT
        val sigmaALon = accelerationStdMetersPerSec2 / metersPerDegLon

        val dt2 = dt * dt
        val dt3 = dt2 * dt
        val dt4 = dt3 * dt

        // Process noise covariance Q for Lat
        val q00Lat = sigmaALat * sigmaALat * dt4 / 4.0
        val q01Lat = sigmaALat * sigmaALat * dt3 / 2.0
        val q11Lat = sigmaALat * sigmaALat * dt2

        // P_pred = F * P * F^T + Q
        p00Lat = p00Lat + 2.0 * dt * p01Lat + dt2 * p11Lat + q00Lat
        p01Lat = p01Lat + dt * p11Lat + q01Lat
        p11Lat = p11Lat + q11Lat

        // Process noise covariance Q for Lon
        val q00Lon = sigmaALon * sigmaALon * dt4 / 4.0
        val q01Lon = sigmaALon * sigmaALon * dt3 / 2.0
        val q11Lon = sigmaALon * sigmaALon * dt2

        p00Lon = p00Lon + 2.0 * dt * p01Lon + dt2 * p11Lon + q00Lon
        p01Lon = p01Lon + dt * p11Lon + q01Lon
        p11Lon = p11Lon + q11Lon

        if (lastTimestampMs > 0L) {
            lastTimestampMs += (dt * 1000.0).roundToLong()
        }
    }

    /**
     * Updates state with new raw GPS measurement.
     *
     * @param lat Raw latitude measurement.
     * @param lon Raw longitude measurement.
     * @param accuracyMeters GPS 1-sigma horizontal accuracy in meters.
     * @param timestampMs Fix timestamp in milliseconds.
     */
    fun update(lat: Double, lon: Double, accuracyMeters: Float, timestampMs: Long = 0L) {
        val safeAcc = accuracyMeters.coerceAtLeast(1.0f).toDouble()
        val metersPerDegLon = getMetersPerDegLon(lat)

        if (!isInitialized) {
            this.lat = lat
            this.lon = lon
            this.vLat = 0.0
            this.vLon = 0.0
            this.lastTimestampMs = timestampMs
            this.isInitialized = true

            val varLat = (safeAcc / METERS_PER_DEG_LAT).let { it * it }
            val varLon = (safeAcc / metersPerDegLon).let { it * it }

            p00Lat = varLat
            p01Lat = 0.0
            val initVelStdLat = 2.0 / METERS_PER_DEG_LAT
            p11Lat = initVelStdLat * initVelStdLat

            p00Lon = varLon
            p01Lon = 0.0
            val initVelStdLon = 2.0 / metersPerDegLon
            p11Lon = initVelStdLon * initVelStdLon
            return
        }

        // Auto-predict if timestamp elapsed and predict wasn't called manually
        if (lastTimestampMs > 0L && timestampMs > lastTimestampMs) {
            val dtSec = (timestampMs - lastTimestampMs) / 1000.0
            predict(dtSec)
        }
        if (timestampMs > 0L) {
            lastTimestampMs = timestampMs
        }

        // Lat measurement update
        val measStdLat = safeAcc / METERS_PER_DEG_LAT
        val rLat = measStdLat * measStdLat
        val yLat = lat - this.lat
        val sLat = p00Lat + rLat
        val k0Lat = p00Lat / sLat
        val k1Lat = p01Lat / sLat

        this.lat += k0Lat * yLat
        this.vLat += k1Lat * yLat

        p00Lat = (1.0 - k0Lat) * p00Lat
        p01Lat = (1.0 - k0Lat) * p01Lat
        p11Lat = (p11Lat - k1Lat * p01Lat).coerceAtLeast(1e-12)

        // Lon measurement update
        val currentMetersPerDegLon = getMetersPerDegLon(this.lat)
        val measStdLon = safeAcc / currentMetersPerDegLon
        val rLon = measStdLon * measStdLon
        val yLon = lon - this.lon
        val sLon = p00Lon + rLon
        val k0Lon = p00Lon / sLon
        val k1Lon = p01Lon / sLon

        this.lon += k0Lon * yLon
        this.vLon += k1Lon * yLon

        p00Lon = (1.0 - k0Lon) * p00Lon
        p01Lon = (1.0 - k0Lon) * p01Lon
        p11Lon = (p11Lon - k1Lon * p01Lon).coerceAtLeast(1e-12)
    }

    /**
     * Resets filter state and covariances to uninitialized.
     */
    fun reset() {
        lat = 0.0
        lon = 0.0
        vLat = 0.0
        vLon = 0.0
        p00Lat = 0.0
        p01Lat = 0.0
        p11Lat = 0.0
        p00Lon = 0.0
        p01Lon = 0.0
        p11Lon = 0.0
        lastTimestampMs = 0L
        isInitialized = false
    }

    private fun getMetersPerDegLon(latitude: Double): Double {
        val cosLat = cos(Math.toRadians(latitude)).coerceAtLeast(0.01)
        return METERS_PER_DEG_LAT * cosLat
    }
}
