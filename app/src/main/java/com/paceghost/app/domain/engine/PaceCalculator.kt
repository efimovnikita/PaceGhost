package com.paceghost.app.domain.engine

import java.util.Locale

object PaceCalculator {

    /**
     * Calculates pace in seconds per kilometer based on distance in meters and elapsed duration in seconds.
     * Returns 0.0 if distance is non-positive or duration is 0.
     */
    fun calculatePaceSecondsPerKm(distanceMeters: Double, durationSeconds: Long): Double {
        if (distanceMeters <= 0.0 || durationSeconds <= 0L) return 0.0
        val km = distanceMeters / 1000.0
        return durationSeconds / km
    }

    /**
     * Converts speed in km/h to pace in seconds per km.
     */
    fun speedKmhToPaceSeconds(speedKmh: Double): Double {
        if (speedKmh <= 0.0) return 0.0
        return 3600.0 / speedKmh
    }

    /**
     * Converts pace in seconds per km to speed in km/h.
     */
    fun paceSecondsToSpeedKmh(paceSecondsPerKm: Double): Double {
        if (paceSecondsPerKm <= 0.0) return 0.0
        return 3600.0 / paceSecondsPerKm
    }

    /**
     * Formats pace as "mm:ss / km" (e.g. "05:30 / km").
     * Returns "--:-- / km" if pace is 0 or invalid.
     */
    fun formatPace(paceSecondsPerKm: Double): String {
        if (paceSecondsPerKm <= 0.0 || paceSecondsPerKm.isInfinite() || paceSecondsPerKm.isNaN() || paceSecondsPerKm > 3600.0) {
            return "--:-- / km"
        }
        val totalSeconds = paceSecondsPerKm.toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d / km", minutes, seconds)
    }

    /**
     * Formats pace as "mm:ss" without the "/ km" suffix.
     */
    fun formatPaceMinSec(paceSecondsPerKm: Double): String {
        if (paceSecondsPerKm <= 0.0 || paceSecondsPerKm.isInfinite() || paceSecondsPerKm.isNaN() || paceSecondsPerKm > 3600.0) {
            return "--:--"
        }
        val totalSeconds = paceSecondsPerKm.toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    /**
     * Formats speed in km/h (e.g. "5.2 km/h").
     */
    fun formatSpeed(speedKmh: Double): String {
        val safeSpeed = if (speedKmh.isNaN() || speedKmh.isInfinite() || speedKmh < 0.0) 0.0 else speedKmh
        return String.format(Locale.US, "%.1f km/h", safeSpeed)
    }

    /**
     * Formats duration in seconds as "mm:ss" or "hh:mm:ss".
     */
    fun formatDuration(durationSeconds: Long): String {
        val safe = if (durationSeconds < 0L) 0L else durationSeconds
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats distance in meters as either meters ("850 m") or kilometers ("1.25 km").
     */
    fun formatDistance(distanceMeters: Double): String {
        val safe = if (distanceMeters < 0.0 || distanceMeters.isNaN()) 0.0 else distanceMeters
        return if (safe >= 1000.0) {
            String.format(Locale.US, "%.2f km", safe / 1000.0)
        } else {
            String.format(Locale.US, "%.0f m", safe)
        }
    }
}
