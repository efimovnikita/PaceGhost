package com.paceghost.app.data.repository

import com.paceghost.app.data.gpx.GpxExporter
import com.paceghost.app.data.gpx.GpxImporter
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

interface WalkRepository {
    fun getAllWalks(): Flow<List<WalkEntity>>
    suspend fun getWalkById(id: Long): WalkEntity?
    suspend fun getPointsForWalk(walkId: Long): List<TrackPointEntity>
    suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint>
    suspend fun saveWalk(walk: WalkEntity, points: List<TrackPointEntity>): Long
    suspend fun deleteWalk(id: Long)
    suspend fun updateWalk(walk: WalkEntity)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun exportWalkToGpx(walkId: Long): String?
    suspend fun importWalkFromGpx(gpxContent: String, titleOverride: String? = null): Long
}

class WalkRepositoryImpl(
    private val walkDao: WalkDao
) : WalkRepository {

    override fun getAllWalks(): Flow<List<WalkEntity>> = walkDao.getAllWalks()

    override suspend fun getWalkById(id: Long): WalkEntity? = walkDao.getWalkById(id)

    override suspend fun getPointsForWalk(walkId: Long): List<TrackPointEntity> =
        walkDao.getPointsForWalk(walkId)

    override suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint> =
        walkDao.getPointsForWalk(walkId).map { it.toDomain() }

    override suspend fun saveWalk(walk: WalkEntity, points: List<TrackPointEntity>): Long {
        val walkId = walkDao.insertWalk(walk)
        val pointsWithWalkId = points.map { it.copy(walkId = walkId) }
        walkDao.insertTrackPoints(pointsWithWalkId)
        return walkId
    }

    override suspend fun deleteWalk(id: Long) = walkDao.deleteWalk(id)

    override suspend fun updateWalk(walk: WalkEntity) = walkDao.updateWalk(walk)

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        walkDao.setFavorite(id, isFavorite)

    override suspend fun exportWalkToGpx(walkId: Long): String? {
        val walk = walkDao.getWalkById(walkId) ?: return null
        val points = walkDao.getPointsForWalk(walkId)
        return GpxExporter.exportToGpx(walk, points)
    }

    override suspend fun importWalkFromGpx(gpxContent: String, titleOverride: String?): Long {
        val (parsedWalk, parsedPoints) = GpxImporter.importFromGpx(
            gpxContent = gpxContent,
            titleOverride = titleOverride
        )
        return saveWalk(parsedWalk, parsedPoints)
    }
}
