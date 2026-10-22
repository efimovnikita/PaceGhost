package com.paceghost.app.domain.model

import com.paceghost.app.domain.engine.PaceCalculator
import java.util.Locale

data class VoiceSettings(
    val behindCooldownSeconds: Long = DEFAULT_BEHIND_COOLDOWN_SECONDS,
    val isAheadVoiceEnabled: Boolean = DEFAULT_AHEAD_VOICE_ENABLED,
    val aheadCooldownSeconds: Long = DEFAULT_AHEAD_COOLDOWN_SECONDS,
    val targetSpeedKmh: Double = DEFAULT_TARGET_SPEED_KMH,
    val toleranceKmh: Double = DEFAULT_TOLERANCE_KMH
) {
    companion object {
        const val DEFAULT_BEHIND_COOLDOWN_SECONDS = 60L
        const val MIN_BEHIND_COOLDOWN_SECONDS = 15L
        const val MAX_BEHIND_COOLDOWN_SECONDS = 300L
        const val BEHIND_COOLDOWN_STEP_SECONDS = 15L

        const val DEFAULT_AHEAD_VOICE_ENABLED = true

        const val DEFAULT_AHEAD_COOLDOWN_SECONDS = 180L
        const val MIN_AHEAD_COOLDOWN_SECONDS = 30L
        const val MAX_AHEAD_COOLDOWN_SECONDS = 600L
        const val AHEAD_COOLDOWN_STEP_SECONDS = 30L

        const val DEFAULT_TARGET_SPEED_KMH = 5.0
        const val MIN_TARGET_SPEED_KMH = 3.0
        const val MAX_TARGET_SPEED_KMH = 8.0
        const val TARGET_SPEED_STEP_KMH = 0.1

        const val DEFAULT_TOLERANCE_KMH = 0.30
        const val MIN_TOLERANCE_KMH = 0.10
        const val MAX_TOLERANCE_KMH = 1.00
        const val TOLERANCE_STEP_KMH = 0.05

        fun formatSeconds(seconds: Long): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return when {
                minutes > 0 && remainingSeconds > 0 -> "$minutes min $remainingSeconds sec"
                minutes > 0 -> "$minutes min"
                else -> "$remainingSeconds sec"
            }
        }

        fun formatTargetSpeed(speedKmh: Double): String {
            val paceSec = PaceCalculator.speedKmhToPaceSeconds(speedKmh)
            val paceStr = PaceCalculator.formatPaceMinSec(paceSec)
            return String.format(Locale.US, "%.1f km/h • %s/km", speedKmh, paceStr)
        }

        fun formatToleranceBadge(toleranceKmh: Double): String {
            return String.format(Locale.US, "±%.2f km/h", toleranceKmh)
        }

        fun formatToleranceRange(targetSpeedKmh: Double, toleranceKmh: Double): String {
            val lower = (targetSpeedKmh - toleranceKmh).coerceAtLeast(0.0)
            val upper = targetSpeedKmh + toleranceKmh
            return String.format(Locale.US, "Silent corridor: %.2f – %.2f km/h", lower, upper)
        }

        fun formatTolerance(targetSpeedKmh: Double, toleranceKmh: Double): String {
            val lower = (targetSpeedKmh - toleranceKmh).coerceAtLeast(0.0)
            val upper = targetSpeedKmh + toleranceKmh
            return String.format(Locale.US, "±%.2f km/h [%.2f – %.2f km/h]", toleranceKmh, lower, upper)
        }
    }
}
