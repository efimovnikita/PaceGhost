package com.paceghost.app.data.repository

import android.content.Context
import com.paceghost.app.data.gpx.GpxExporter
import com.paceghost.app.data.gpx.GpxImporter
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.heatmap.HeatmapTileProvider
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.util.AppLogger
import kotlinx.coroutines.flow.Flow
import java.io.File

interface WalkRepository {
    fun getAllWalks(): Flow<List<WalkEntity>>
    suspend fun getWalkById(id: Long): WalkEntity?
    suspend fun getPointsForWalk(walkId: Long): List<TrackPointEntity>
    suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint>
    suspend fun saveWalk(walk: WalkEntity, points: List<TrackPointEntity>): Long
    suspend fun insertWalk(walk: WalkEntity, points: List<TrackPointEntity> = emptyList()): Long = saveWalk(walk, points)
    suspend fun deleteWalk(id: Long)
    suspend fun clearAllWalks() {}
    suspend fun updateWalk(walk: WalkEntity)
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    suspend fun exportWalkToGpx(walkId: Long): String?
    suspend fun importWalkFromGpx(gpxContent: String, titleOverride: String? = null): Long
    fun clearHeatmapCache() {}
}

class WalkRepositoryImpl(
    private val walkDao: WalkDao,
    private val heatmapCacheDir: File? = null
) : WalkRepository {

    constructor(context: Context, walkDao: WalkDao) : this(
        walkDao = walkDao,
        heatmapCacheDir = HeatmapTileProvider.getCacheDir(context)
    )

    constructor(walkDao: WalkDao, context: Context) : this(
        walkDao = walkDao,
        heatmapCacheDir = HeatmapTileProvider.getCacheDir(context)
    )

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
        clearHeatmapCache()
        return walkId
    }

    override suspend fun insertWalk(walk: WalkEntity, points: List<TrackPointEntity>): Long {
        return saveWalk(walk, points)
    }

    override suspend fun deleteWalk(id: Long) {
        walkDao.deleteWalk(id)
        clearHeatmapCache()
    }

    override suspend fun clearAllWalks() {
        walkDao.clearAllWalks()
        clearHeatmapCache()
    }

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

    override fun clearHeatmapCache() {
        try {
            heatmapCacheDir?.deleteRecursively()
        } catch (e: Exception) {
            AppLogger.e("WalkRepository", "Failed to clear heatmap tile cache", e)
        }
    }
}
