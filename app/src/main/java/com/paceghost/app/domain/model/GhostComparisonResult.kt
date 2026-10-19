package com.paceghost.app.domain.model

data class GhostComparisonResult(
    val status: GhostStatus,
    val timeDeltaSeconds: Long,
    val distanceDeltaMeters: Double,
    val currentProgressMeters: Double,
    val totalGhostMeters: Double,
    val ghostLat: Double?,
    val ghostLon: Double?,
    val shouldVibrateBehind: Boolean = false,
    val shouldVibrateOvertake: Boolean = false
) {
    companion object {
        val NO_GHOST = GhostComparisonResult(
            status = GhostStatus.NO_GHOST,
            timeDeltaSeconds = 0L,
            distanceDeltaMeters = 0.0,
            currentProgressMeters = 0.0,
            totalGhostMeters = 0.0,
            ghostLat = null,
            ghostLon = null,
            shouldVibrateBehind = false,
            shouldVibrateOvertake = false
        )
    }
}
