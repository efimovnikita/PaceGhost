package com.paceghost.app.domain.model

/**
 * Cumulative all-time walking summary aggregated across all Qualifying Walks.
 */
data class LifetimeStats(
    val walkCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val totalSteps: Long = 0L,
    val longestWalkMeters: Double = 0.0
) {
    val hasQualifyingWalks: Boolean
        get() = walkCount > 0

    val weightedAverageSpeedKmh: Double?
        get() = if (walkCount > 0 && totalDurationSeconds > 0L && totalDistanceMeters > 0.0) {
            (totalDistanceMeters / 1000.0) / (totalDurationSeconds / 3600.0)
        } else {
            null
        }

    companion object {
        const val MIN_DURATION_SECONDS: Long = 60L
        const val MIN_DISTANCE_METERS: Double = 50.0
    }
}
