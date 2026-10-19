package com.paceghost.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
