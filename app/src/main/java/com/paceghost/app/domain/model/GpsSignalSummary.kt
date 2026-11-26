package com.paceghost.app.domain.model

import java.util.Locale

enum class GpsSignalTier {
    GOOD,
    FAIR,
    POOR,
    UNAVAILABLE
}

/**
 * Session summary of GPS signal quality across active walking time.
 */
data class GpsSignalSummary(
    val scorePercent: Int? = null,
    val tier: GpsSignalTier = GpsSignalTier.UNAVAILABLE,
    val totalActiveSeconds: Int = 0
) {
    fun formatDisplay(): String = when (tier) {
        GpsSignalTier.GOOD -> String.format(Locale.US, "Good · %d%%", scorePercent ?: 0)
        GpsSignalTier.FAIR -> String.format(Locale.US, "Fair · %d%%", scorePercent ?: 0)
        GpsSignalTier.POOR -> String.format(Locale.US, "Poor · %d%%", scorePercent ?: 0)
        GpsSignalTier.UNAVAILABLE -> "N/A"
    }
}
