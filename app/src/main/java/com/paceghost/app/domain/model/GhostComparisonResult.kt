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
    val shouldVibrateOvertake: Boolean = false,
    val shouldVibrateAheadReminder: Boolean = false,
    val shouldVibrateOffRoute: Boolean = false,
    val shouldSpeakBehind: Boolean = false,
    val shouldSpeakOvertake: Boolean = false,
    val shouldSpeakAheadReminder: Boolean = false,
    val shouldSpeakOffRoute: Boolean = false
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
            shouldVibrateOvertake = false,
            shouldVibrateAheadReminder = false,
            shouldVibrateOffRoute = false,
            shouldSpeakBehind = false,
            shouldSpeakOvertake = false,
            shouldSpeakAheadReminder = false,
            shouldSpeakOffRoute = false
        )
    }
}
