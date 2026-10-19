package com.paceghost.app.domain.model

data class WalkState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val isAutoPaused: Boolean = false,
    val isDemoMode: Boolean = false,
    val isVoiceGuidanceEnabled: Boolean = true,
    val elapsedSeconds: Long = 0L,
    val distanceMeters: Double = 0.0,
    val stepCount: Int = 0,
    val currentPaceSecondsPerKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val ghostComparison: GhostComparisonResult = GhostComparisonResult.NO_GHOST,
    val currentPoints: List<TrackPoint> = emptyList(),
    val ghostPoints: List<TrackPoint> = emptyList(),
    val recentVibrationMessage: String? = null,
    val recentVoiceMessage: String? = null
)
