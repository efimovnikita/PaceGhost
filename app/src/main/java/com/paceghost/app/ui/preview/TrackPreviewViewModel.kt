package com.paceghost.app.ui.preview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
import com.paceghost.app.domain.model.TrackPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TrackPreviewUiState {
    object Loading : TrackPreviewUiState
    data class Success(
        val walk: WalkEntity,
        val trackPoints: List<TrackPoint>
    ) : TrackPreviewUiState
    data class Error(val message: String) : TrackPreviewUiState
}

class TrackPreviewViewModel(
    application: Application,
    private val repository: WalkRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        WalkRepositoryImpl(
            AppDatabase.getInstance(application).walkDao()
        )
    )

    private val _uiState = MutableStateFlow<TrackPreviewUiState>(TrackPreviewUiState.Loading)
    val uiState: StateFlow<TrackPreviewUiState> = _uiState.asStateFlow()

    private var currentWalkId: Long? = null

    fun loadWalk(walkId: Long, forceReload: Boolean = false) {
        if (!forceReload && currentWalkId == walkId && _uiState.value is TrackPreviewUiState.Success) {
            return
        }
        currentWalkId = walkId

        viewModelScope.launch {
            _uiState.value = TrackPreviewUiState.Loading
            try {
                val walk = repository.getWalkById(walkId)
                if (walk == null) {
                    _uiState.value = TrackPreviewUiState.Error("Walk not found")
                } else {
                    val trackPoints = repository.getTrackPointsForWalk(walkId)
                    _uiState.value = TrackPreviewUiState.Success(
                        walk = walk,
                        trackPoints = trackPoints
                    )
                }
            } catch (e: Exception) {
                _uiState.value = TrackPreviewUiState.Error(
                    e.localizedMessage ?: "Failed to load route preview"
                )
            }
        }
    }
}
