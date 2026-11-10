package com.paceghost.app.domain.heatmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.util.AppLogger
import kotlinx.coroutines.runBlocking
import org.osmdroid.tileprovider.IRegisterReceiver
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.MapTileIndex
import java.io.File
import java.io.FileOutputStream

/**
 * Tile module provider responsible for on-demand generation and disk caching of Personal Heatmap tiles.
 */
class HeatmapTileModuleProvider(
    private val context: Context,
    private val walkDao: WalkDao,
    private val rasterizer: HeatmapTileRasterizer = HeatmapTileRasterizer(),
    val cacheDir: File = HeatmapTileProvider.getCacheDir(context),
    threadPoolSize: Int = 2,
    pendingQueueSize: Int = 40
) : MapTileModuleProviderBase(threadPoolSize, pendingQueueSize) {

    private val loader = HeatmapTileLoader()

    override fun getName(): String = "HeatmapTileModuleProvider"

    override fun getThreadGroupName(): String = "heatmap"

    override fun getTileLoader(): TileLoader = loader

    override fun getUsesDataConnection(): Boolean = false

    override fun getMinimumZoomLevel(): Int = 1

    override fun getMaximumZoomLevel(): Int = 20

    override fun setTileSource(tileSource: ITileSource?) {
        // Managed internally
    }

    inner class HeatmapTileLoader : TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val tileX = MapTileIndex.getX(pMapTileIndex)
            val tileY = MapTileIndex.getY(pMapTileIndex)

            val bitmap = getOrRenderTile(zoom, tileX, tileY) ?: return null
            return BitmapDrawable(context.resources, bitmap)
        }
    }

    fun getOrRenderTile(zoom: Int, tileX: Int, tileY: Int): Bitmap? {
        val cachedFile = File(cacheDir, "$zoom/$tileX/$tileY.png")
        if (cachedFile.exists() && cachedFile.length() > 0) {
            try {
                BitmapFactory.decodeFile(cachedFile.absolutePath)?.let { return it }
            } catch (e: Exception) {
                AppLogger.e("HeatmapTileProvider", "Corrupt tile cache file ${cachedFile.path}", e)
                cachedFile.delete()
            }
        }

        // Query track points in tile bounding box with 8% edge padding to avoid border cuts
        val bbox = WebMercatorProjection.tileToBounds(zoom, tileX, tileY, bufferFraction = 0.08)
        val points = runBlocking {
            walkDao.getPointsInBoundingBox(
                minLat = bbox.minLat,
                maxLat = bbox.maxLat,
                minLon = bbox.minLon,
                maxLon = bbox.maxLon
            )
        }

        if (points.size < 2) return null

        val bitmap = rasterizer.renderTile(
            points = points,
            zoom = zoom,
            tileX = tileX,
            tileY = tileY,
            tileSize = WebMercatorProjection.DEFAULT_TILE_SIZE
        ) ?: return null

        // Cache rendered tile to disk
        try {
            cachedFile.parentFile?.mkdirs()
            val tempFile = File(cacheDir, "$zoom/$tileX/$tileY.png.tmp")
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            tempFile.renameTo(cachedFile)
        } catch (e: Exception) {
            AppLogger.e("HeatmapTileProvider", "Failed to write tile cache $zoom/$tileX/$tileY", e)
        }

        return bitmap
    }
}

/**
 * Osmdroid [MapTileProviderArray] implementation for the Personal Heatmap layer.
 */
class HeatmapTileProvider(
    context: Context,
    walkDao: WalkDao,
    rasterizer: HeatmapTileRasterizer = HeatmapTileRasterizer(),
    cacheDir: File = getCacheDir(context),
    registerReceiver: IRegisterReceiver = SimpleRegisterReceiver(context.applicationContext),
    tileSource: ITileSource = XYTileSource(
        "Heatmap",
        1,
        20,
        WebMercatorProjection.DEFAULT_TILE_SIZE,
        ".png",
        emptyArray()
    )
) : MapTileProviderArray(
    tileSource,
    registerReceiver,
    arrayOf(
        HeatmapTileModuleProvider(
            context = context,
            walkDao = walkDao,
            rasterizer = rasterizer,
            cacheDir = cacheDir
        )
    )
) {
    override fun getMinimumZoomLevel(): Int = 1
    override fun getMaximumZoomLevel(): Int = 20
    override fun getTileWriter(): IFilesystemCache? = null

    companion object {
        const val CACHE_DIR_NAME = "heatmap_tiles"

        /**
         * Returns the localized disk cache directory for Personal Heatmap raster tiles:
         * context.cacheDir.resolve("heatmap_tiles")
         */
        fun getCacheDir(context: Context): File = context.cacheDir.resolve(CACHE_DIR_NAME)

        /**
         * Clears all cached heatmap tiles on disk.
         */
        fun clearCache(context: Context): Boolean = getCacheDir(context).deleteRecursively()

        /**
         * Clears all cached heatmap tiles within the given directory.
         */
        fun clearCache(cacheDir: File): Boolean = cacheDir.deleteRecursively()
    }
}
