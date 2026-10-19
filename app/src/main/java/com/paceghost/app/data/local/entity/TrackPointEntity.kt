package com.paceghost.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import com.paceghost.app.domain.model.TrackPoint

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = WalkEntity::class,
            parentColumns = ["id"],
            childColumns = ["walkId"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["walkId"])
    ]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val walkId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val timestamp: Long,
    val elapsedSeconds: Long,
    val cumulativeDistance: Double,
    val speed: Float,
    val stepCountAtPoint: Int
) {
    fun toDomain(): TrackPoint = TrackPoint(
        lat = latitude,
        lon = longitude,
        altitude = altitude,
        timestamp = timestamp,
        elapsedSeconds = elapsedSeconds,
        cumulativeDistance = cumulativeDistance,
        speed = speed,
        stepCount = stepCountAtPoint
    )

    companion object {
        fun fromDomain(point: TrackPoint, walkId: Long, id: Long = 0): TrackPointEntity =
            TrackPointEntity(
                id = id,
                walkId = walkId,
                latitude = point.lat,
                longitude = point.lon,
                altitude = point.altitude,
                timestamp = point.timestamp,
                elapsedSeconds = point.elapsedSeconds,
                cumulativeDistance = point.cumulativeDistance,
                speed = point.speed,
                stepCountAtPoint = point.stepCount
            )
    }
}
