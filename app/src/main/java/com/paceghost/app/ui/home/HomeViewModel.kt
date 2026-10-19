package com.paceghost.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
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

    private val _isDemoMode = MutableStateFlow(true)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val userMessage = _userMessage.asSharedFlow()

    init {
        // Ensure initial demo walk is available
        viewModelScope.launch {
            AppDatabase.populateDemoWalk(AppDatabase.getInstance(application))
        }
    }

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun deleteWalk(id: Long) {
        viewModelScope.launch {
            repository.deleteWalk(id)
            _userMessage.emit("Прогулка удалена")
        }
    }

    fun importGpxContent(content: String, titleOverride: String? = null) {
        viewModelScope.launch {
            try {
                val newId = repository.importWalkFromGpx(content, titleOverride)
                _userMessage.emit("Маршрут успешно импортирован (ID: $newId)")
            } catch (e: Exception) {
                _userMessage.emit("Ошибка импорта GPX: ${e.localizedMessage ?: "Неверный формат"}")
            }
        }
    }

    suspend fun getGpxContent(walkId: Long): String? {
        return repository.exportWalkToGpx(walkId)
    }
}
