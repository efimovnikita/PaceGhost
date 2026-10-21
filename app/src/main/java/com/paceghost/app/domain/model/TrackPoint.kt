package com.paceghost.app.domain.model

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedSeconds: Long = 0,
    val cumulativeDistance: Double = 0.0,
    val speed: Float = 0f,
    val stepCount: Int = 0,
    val isPdr: Boolean = false,
    val accuracy: Float = 0f
)
