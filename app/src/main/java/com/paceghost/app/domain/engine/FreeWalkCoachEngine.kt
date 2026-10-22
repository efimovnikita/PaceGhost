package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.VoiceSettings

enum class FreeWalkFeedbackEvent {
    NONE,
    SPEED_UP,
    GOOD_PACE
}

/**
 * Real-time pace coach engine for Free Walk mode.
 *
 * Evaluates user's rolling speed over a 20-30 second window against the configured
 * target speed and tolerance corridor. Emits feedback events with debounce and cooldowns.
 */
class FreeWalkCoachEngine(
    var settings: VoiceSettings = VoiceSettings()
) {
    companion object {
        const val ROLLING_WINDOW_SECONDS = 25L
        const val DEBOUNCE_SECONDS = 3L
        const val WARMUP_SECONDS = 15L
        const val STATIONARY_SPEED_THRESHOLD_KMH = 1.5
    }

    private data class ProgressSample(val elapsedSeconds: Long, val distanceMeters: Double)

    private val samples = ArrayDeque<ProgressSample>()

    private var slowDurationSeconds = 0L
    private var fastDurationSeconds = 0L

    private var lastBehindAlertTime = -10000L
    private var lastAheadAlertTime = -10000L

    /**
     * Resets internal history and cooldown states.
     */
    fun reset() {
        samples.clear()
        slowDurationSeconds = 0L
        fastDurationSeconds = 0L
        lastBehindAlertTime = -10000L
        lastAheadAlertTime = -10000L
    }

    /**
     * Updates engine with latest progress.
     *
     * @param distanceMeters Current cumulative distance in meters
     * @param elapsedSeconds Elapsed active walk time in seconds
     * @param isPaused True if tracking is paused (manual or auto-pause)
     * @return [FreeWalkFeedbackEvent] to trigger TTS and haptic feedback
     */
    fun update(
        distanceMeters: Double,
        elapsedSeconds: Long,
        isPaused: Boolean
    ): FreeWalkFeedbackEvent {
        if (isPaused) {
            slowDurationSeconds = 0L
            fastDurationSeconds = 0L
            return FreeWalkFeedbackEvent.NONE
        }

        // Add current sample and prune old samples outside rolling window
        samples.addLast(ProgressSample(elapsedSeconds, distanceMeters))
        val windowStart = elapsedSeconds - ROLLING_WINDOW_SECONDS
        while (samples.isNotEmpty() && samples.first().elapsedSeconds < windowStart) {
            samples.removeFirst()
        }

        // Calculate rolling speed
        val rollingSpeedKmh = calculateRollingSpeedKmh()

        // Don't alert during warmup or when stationary (e.g. stopped at a traffic light)
        if (elapsedSeconds <= WARMUP_SECONDS || rollingSpeedKmh < STATIONARY_SPEED_THRESHOLD_KMH) {
            slowDurationSeconds = 0L
            fastDurationSeconds = 0L
            return FreeWalkFeedbackEvent.NONE
        }

        val targetSpeed = settings.targetSpeedKmh
        val tolerance = settings.toleranceKmh
        val lowerBound = targetSpeed - tolerance
        val upperBound = targetSpeed + tolerance

        return when {
            rollingSpeedKmh < lowerBound -> {
                fastDurationSeconds = 0L
                slowDurationSeconds++

                if (slowDurationSeconds >= DEBOUNCE_SECONDS &&
                    (elapsedSeconds - lastBehindAlertTime) >= settings.behindCooldownSeconds
                ) {
                    lastBehindAlertTime = elapsedSeconds
                    FreeWalkFeedbackEvent.SPEED_UP
                } else {
                    FreeWalkFeedbackEvent.NONE
                }
            }

            rollingSpeedKmh > upperBound -> {
                slowDurationSeconds = 0L
                fastDurationSeconds++

                if (settings.isAheadVoiceEnabled &&
                    fastDurationSeconds >= DEBOUNCE_SECONDS &&
                    (elapsedSeconds - lastAheadAlertTime) >= settings.aheadCooldownSeconds
                ) {
                    lastAheadAlertTime = elapsedSeconds
                    FreeWalkFeedbackEvent.GOOD_PACE
                } else {
                    FreeWalkFeedbackEvent.NONE
                }
            }

            else -> {
                // In the target corridor: silent
                slowDurationSeconds = 0L
                fastDurationSeconds = 0L
                FreeWalkFeedbackEvent.NONE
            }
        }
    }

    /**
     * Computes speed in km/h based on distance delta over elapsed time delta in the rolling window.
     */
    fun calculateRollingSpeedKmh(): Double {
        if (samples.size < 2) return 0.0
        val oldest = samples.first()
        val newest = samples.last()
        val dtSec = newest.elapsedSeconds - oldest.elapsedSeconds
        if (dtSec <= 0L) return 0.0
        val dDistMeters = (newest.distanceMeters - oldest.distanceMeters).coerceAtLeast(0.0)
        return (dDistMeters / dtSec) * 3.6
    }
}
