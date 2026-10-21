package com.paceghost.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paceghost.app.data.gpx.GpxExporter
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.VoiceSettingsPreferences
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
import com.paceghost.app.domain.model.VoiceSettings
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalkRepository = WalkRepositoryImpl(
        AppDatabase.getInstance(application, viewModelScope).walkDao()
    )

    val walks: StateFlow<List<WalkEntity>> = repository.getAllWalks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val voicePrefs = VoiceSettingsPreferences(application)
    private val _voiceSettings = MutableStateFlow(voicePrefs.getVoiceSettings())
    val voiceSettings: StateFlow<VoiceSettings> = _voiceSettings.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val userMessage = _userMessage.asSharedFlow()

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
        if (enabled) {
            viewModelScope.launch {
                val db = AppDatabase.getInstance(getApplication())
                if (db.walkDao().getWalkCount() == 0) {
                    AppDatabase.populateDemoWalk(db)
                }
            }
        }
    }

    fun deleteWalk(id: Long) {
        viewModelScope.launch {
            repository.deleteWalk(id)
            _userMessage.emit("Walk deleted")
        }
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        voicePrefs.saveVoiceSettings(settings)
        _voiceSettings.value = settings
        viewModelScope.launch {
            _userMessage.emit("Pace alert settings saved")
        }
    }

    fun importGpxContent(content: String, titleOverride: String? = null) {
        viewModelScope.launch {
            try {
                val newId = repository.importWalkFromGpx(content, titleOverride)
                _userMessage.emit("Route successfully imported (ID: $newId)")
            } catch (e: Exception) {
                _userMessage.emit("GPX import error: ${e.localizedMessage ?: "Invalid format"}")
            }
        }
    }

    suspend fun getGpxContent(walkId: Long): String? {
        return repository.exportWalkToGpx(walkId)
    }

    suspend fun exportWalkToFile(walkId: Long): GpxExportResult {
        return try {
            val walk = repository.getWalkById(walkId) ?: return GpxExportResult.WalkNotFound
            val points = repository.getPointsForWalk(walkId)
            if (points.isEmpty()) {
                return GpxExportResult.NoPoints
            }
            val file = GpxExporter.exportToCache(getApplication(), walk, points)
            GpxExportResult.Success(file = file, title = walk.title)
        } catch (e: Exception) {
            GpxExportResult.Error(e.localizedMessage ?: "Unknown export error")
        }
    }
}

sealed interface GpxExportResult {
    data class Success(val file: File, val title: String) : GpxExportResult
    data object NoPoints : GpxExportResult
    data object WalkNotFound : GpxExportResult
    data class Error(val message: String) : GpxExportResult
}
