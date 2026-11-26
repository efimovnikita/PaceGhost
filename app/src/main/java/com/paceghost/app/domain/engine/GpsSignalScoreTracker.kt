package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.GpsQuality
import com.paceghost.app.domain.model.GpsSignalSummary
import com.paceghost.app.domain.model.GpsSignalTier
import kotlin.math.roundToInt

/**
 * Tracks per-second GPS signal quality during active walking time (excluding pauses)
 * and computes a weighted session GPS Signal Score (0-100%).
 */
class GpsSignalScoreTracker {

    private var goodSeconds: Int = 0
    private var weakSeconds: Int = 0
    private var pdrSeconds: Int = 0

    fun recordActiveTick(
        currentQuality: GpsQuality,
        hasValidFix: Boolean,
        timeSinceLastFixMs: Long,
        isDemoMode: Boolean
    ): GpsSignalSummary {
        val effectiveQuality = when {
            isDemoMode -> currentQuality
            !hasValidFix || timeSinceLastFixMs > STALE_GPS_THRESHOLD_MS -> GpsQuality.PDR_ACTIVE
            else -> currentQuality
        }

        when (effectiveQuality) {
            GpsQuality.GOOD -> goodSeconds++
            GpsQuality.WEAK -> weakSeconds++
            GpsQuality.PDR_ACTIVE -> pdrSeconds++
        }

        return currentSummary()
    }

    fun currentSummary(): GpsSignalSummary {
        val total = goodSeconds + weakSeconds + pdrSeconds
        if (total == 0) {
            return GpsSignalSummary(
                scorePercent = null,
                tier = GpsSignalTier.UNAVAILABLE,
                totalActiveSeconds = 0
            )
        }

        val weightedSum = goodSeconds * WEIGHT_GOOD + weakSeconds * WEIGHT_WEAK + pdrSeconds * WEIGHT_PDR
        val scorePercent = ((weightedSum / total) * 100.0).roundToInt().coerceIn(0, 100)
        val tier = when {
            scorePercent >= GOOD_THRESHOLD -> GpsSignalTier.GOOD
            scorePercent >= FAIR_THRESHOLD -> GpsSignalTier.FAIR
            else -> GpsSignalTier.POOR
        }

        return GpsSignalSummary(
            scorePercent = scorePercent,
            tier = tier,
            totalActiveSeconds = total
        )
    }

    fun reset() {
        goodSeconds = 0
        weakSeconds = 0
        pdrSeconds = 0
    }

    companion object {
        const val STALE_GPS_THRESHOLD_MS = 4500L
        private const val WEIGHT_GOOD = 1.0
        private const val WEIGHT_WEAK = 0.5
        private const val WEIGHT_PDR = 0.0
        private const val GOOD_THRESHOLD = 80
        private const val FAIR_THRESHOLD = 50
    }
}
