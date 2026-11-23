package com.paceghost.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.model.LifetimeStats
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalk(walk: WalkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM walks ORDER BY startTime DESC")
    fun getAllWalks(): Flow<List<WalkEntity>>

    @Query("SELECT * FROM walks WHERE id = :id")
    suspend fun getWalkById(id: Long): WalkEntity?

    @Query("SELECT * FROM track_points WHERE walkId = :walkId ORDER BY id ASC")
    suspend fun getPointsForWalk(walkId: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon ORDER BY walkId, id ASC")
    suspend fun getPointsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<TrackPointEntity>

    @Query("DELETE FROM walks WHERE id = :id")
    suspend fun deleteWalk(id: Long)

    @Query(
        """
        SELECT walkId,
               (MIN(latitude) + MAX(latitude)) / 2.0 AS centerLat,
               (MIN(longitude) + MAX(longitude)) / 2.0 AS centerLon,
               MIN(latitude) AS minLat, MAX(latitude) AS maxLat,
               MIN(longitude) AS minLon, MAX(longitude) AS maxLon
        FROM track_points
        GROUP BY walkId
        HAVING COUNT(*) >= 2
        """
    )
    suspend fun getWalkAnchors(): List<com.paceghost.app.domain.model.WalkAnchor>

    @Update
    suspend fun updateWalk(walk: WalkEntity)

    @Query("UPDATE walks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM walks")
    suspend fun getWalkCount(): Int

    @Query(
        """
        SELECT COUNT(*) AS walkCount,
               COALESCE(SUM(distanceMeters), 0.0) AS totalDistanceMeters,
               COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds,
               COALESCE(SUM(stepCount), 0) AS totalSteps,
               COALESCE(MAX(distanceMeters), 0.0) AS longestWalkMeters
        FROM walks
        WHERE durationSeconds >= :minDurationSeconds
          AND distanceMeters >= :minDistanceMeters
        """
    )
    suspend fun getLifetimeStats(
        minDurationSeconds: Long = LifetimeStats.MIN_DURATION_SECONDS,
        minDistanceMeters: Double = LifetimeStats.MIN_DISTANCE_METERS
    ): LifetimeStats

    @Query("DELETE FROM walks")
    suspend fun clearAllWalks()
}
