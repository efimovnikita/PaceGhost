package com.paceghost.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
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

    @Query("DELETE FROM walks WHERE id = :id")
    suspend fun deleteWalk(id: Long)

    @Update
    suspend fun updateWalk(walk: WalkEntity)

    @Query("UPDATE walks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM walks")
    suspend fun getWalkCount(): Int

    @Query("DELETE FROM walks")
    suspend fun clearAllWalks()
}
