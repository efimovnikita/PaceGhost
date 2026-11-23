package com.paceghost.app.domain.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats [LifetimeStats] metrics strictly in [Locale.US].
 */
object LifetimeStatsFormatter {

    private const val EM_DASH = "—"

    fun formatTotalDistance(distanceMeters: Double): String {
        return String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
    }

    fun formatTotalWalks(walkCount: Int): String {
        return walkCount.toString()
    }

    fun formatTotalTime(durationSeconds: Long): String {
        val safeSeconds = durationSeconds.coerceAtLeast(0L)
        val hours = safeSeconds / 3600L
        val minutes = (safeSeconds % 3600L) / 60L
        return if (hours > 0L) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatTotalSteps(totalSteps: Long): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(totalSteps)
    }

    fun formatAvgSpeed(speedKmh: Double?): String {
        return if (speedKmh != null) {
            String.format(Locale.US, "%.1f km/h", speedKmh)
        } else {
            EM_DASH
        }
    }

    fun formatLongestWalk(walkCount: Int, longestWalkMeters: Double): String {
        return if (walkCount > 0 && longestWalkMeters > 0.0) {
            String.format(Locale.US, "%.1f km", longestWalkMeters / 1000.0)
        } else {
            EM_DASH
        }
    }
}
