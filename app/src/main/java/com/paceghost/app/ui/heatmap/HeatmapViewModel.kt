package com.paceghost.app.ui.heatmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint

data class HeatmapUiState(
    val isLoading: Boolean = true,
    val recentWalkBounds: BoundingBox? = null,
    val initialCenter: GeoPoint? = null,
    val hasWalks: Boolean = false
)

class HeatmapViewModel(
    application: Application,
    private val repository: WalkRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        WalkRepositoryImpl(
            application,
            AppDatabase.getInstance(application).walkDao()
        )
    )

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    init {
        loadRecentBounds()
    }

    fun loadRecentBounds() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val walks = repository.getAllWalks().firstOrNull().orEmpty()
                if (walks.isEmpty()) {
                    _uiState.value = HeatmapUiState(
                        isLoading = false,
                        recentWalkBounds = null,
                        initialCenter = null,
                        hasWalks = false
                    )
                    return@launch
                }

                // Recent walk is first due to ORDER BY startTime DESC
                val latestWalk = walks.first()
                val points = repository.getTrackPointsForWalk(latestWalk.id)

                if (points.isEmpty()) {
                    _uiState.value = HeatmapUiState(
                        isLoading = false,
                        recentWalkBounds = null,
                        initialCenter = null,
                        hasWalks = true
                    )
                    return@launch
                }

                val minLat = points.minOf { it.lat }
                val maxLat = points.maxOf { it.lat }
                val minLon = points.minOf { it.lon }
                val maxLon = points.maxOf { it.lon }

                val bbox = BoundingBox(maxLat, maxLon, minLat, minLon)
                val center = GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)

                _uiState.value = HeatmapUiState(
                    isLoading = false,
                    recentWalkBounds = bbox,
                    initialCenter = center,
                    hasWalks = true
                )
            } catch (_: Exception) {
                _uiState.value = HeatmapUiState(
                    isLoading = false,
                    recentWalkBounds = null,
                    initialCenter = null,
                    hasWalks = false
                )
            }
        }
    }
}
