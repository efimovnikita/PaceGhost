package com.paceghost.app.data.local

import android.content.Context
import com.paceghost.app.domain.heatmap.HeatmapTileProvider
import com.paceghost.app.util.AppLogger
import java.io.File

/**
 * SharedPreferences storage and cache migration helper for Personal Heatmap.
 */
class HeatmapPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCacheSchemaVersion(): Int = prefs.getInt(KEY_CACHE_SCHEMA_VERSION, 0)

    fun setCacheSchemaVersion(version: Int) {
        prefs.edit().putInt(KEY_CACHE_SCHEMA_VERSION, version).apply()
    }

    /**
     * Purges outdated heatmap disk cache if stored version is less than [HEATMAP_CACHE_SCHEMA_VERSION].
     * Returns true if cache was purged, false if already up-to-date.
     */
    fun purgeOutdatedCacheIfNeeded(cacheDir: File = HeatmapTileProvider.getCacheDir(context)): Boolean {
        val currentVersion = getCacheSchemaVersion()
        if (currentVersion < HEATMAP_CACHE_SCHEMA_VERSION) {
            try {
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                }
            } catch (e: Exception) {
                AppLogger.e("HeatmapPreferences", "Failed to purge outdated heatmap cache", e)
            }
            setCacheSchemaVersion(HEATMAP_CACHE_SCHEMA_VERSION)
            return true
        }
        return false
    }

    companion object {
        const val PREFS_NAME = "paceghost_heatmap_preferences"
        const val KEY_CACHE_SCHEMA_VERSION = "heatmap_cache_version"
        const val HEATMAP_CACHE_SCHEMA_VERSION = 2
    }
}
