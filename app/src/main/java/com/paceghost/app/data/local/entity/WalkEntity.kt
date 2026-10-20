package com.paceghost.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paceghost.app.domain.engine.PaceCalculator

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

