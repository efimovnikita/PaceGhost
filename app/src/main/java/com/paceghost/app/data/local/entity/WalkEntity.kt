package com.paceghost.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.domain.model.VoiceSettings

@Entity(tableName = "walks")
data class WalkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val stepCount: Int,
    val averagePaceSecondsPerKm: Double,
    val isFavorite: Boolean = false
)

val WalkEntity.averageSpeedKmh: Double
    get() = if (durationSeconds > 0L && distanceMeters > 0.0) {
        (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
    } else {
        PaceCalculator.paceSecondsToSpeedKmh(averagePaceSecondsPerKm)
    }

/**
 * Computes the median walking speed in km/h from recent valid walks.
 * Filters out walks shorter than [minDurationSeconds] or [minDistanceMeters].
 * Returns [VoiceSettings.DEFAULT_TARGET_SPEED_KMH] if no valid history exists.
 */
fun List<WalkEntity>.calculateMedianSpeedKmh(
    minDurationSeconds: Long = 60L,
    minDistanceMeters: Double = 50.0,
    limit: Int = 5
): Double {
    val validWalks = this
        .filter { it.durationSeconds >= minDurationSeconds && it.distanceMeters >= minDistanceMeters }
        .take(limit)
    if (validWalks.isEmpty()) return VoiceSettings.DEFAULT_TARGET_SPEED_KMH

    val speeds = validWalks.map { it.averageSpeedKmh }.sorted()
    val mid = speeds.size / 2
    val median = if (speeds.size % 2 == 1) {
        speeds[mid]
    } else {
        (speeds[mid - 1] + speeds[mid]) / 2.0
    }
    // Round to 1 decimal place (e.g. 5.2 km/h)
    val rounded = Math.round(median * 10.0) / 10.0
    return rounded.coerceIn(VoiceSettings.MIN_TARGET_SPEED_KMH, VoiceSettings.MAX_TARGET_SPEED_KMH)
}
