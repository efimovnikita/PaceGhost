package com.paceghost.app.ui.active

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
import com.paceghost.app.domain.engine.SmartRouteNamingEngine
import com.paceghost.app.domain.model.WalkState
import com.paceghost.app.service.WalkTrackingService
import com.paceghost.app.service.geocode.AndroidWalkGeocoder
import com.paceghost.app.service.geocode.RouteGeocoder
import com.paceghost.app.util.AppLogger
import com.paceghost.app.util.BatteryOptimizationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveWalkViewModel(
    application: Application,
    private val routeGeocoder: RouteGeocoder = AndroidWalkGeocoder(application)
) : AndroidViewModel(application) {

    private val repository: WalkRepository = WalkRepositoryImpl(
        AppDatabase.getInstance(application, viewModelScope).walkDao()
    )

    private val _walkState = MutableStateFlow(WalkState())
    val walkState: StateFlow<WalkState> = _walkState.asStateFlow()

    private val _showSummaryDialog = MutableStateFlow(false)
    val showSummaryDialog: StateFlow<Boolean> = _showSummaryDialog.asStateFlow()

    private val _isSuggestingTitle = MutableStateFlow(false)
    val isSuggestingTitle: StateFlow<Boolean> = _isSuggestingTitle.asStateFlow()

    private val _suggestedTitle = MutableStateFlow<String?>(null)
    val suggestedTitle: StateFlow<String?> = _suggestedTitle.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val userMessage = _userMessage.asSharedFlow()

    private val _showBatteryOptimizationDialog = MutableStateFlow(false)
    val showBatteryOptimizationDialog: StateFlow<Boolean> = _showBatteryOptimizationDialog.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ActiveWalkNavigationEvent>(extraBufferCapacity = 2)
    val navigationEvents = _navigationEvents.asSharedFlow()

    private var trackingService: WalkTrackingService? = null
    private var isBound = false
    private var stateCollectJob: Job? = null
    private var isDemoSpeedOffRoute = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? WalkTrackingService.LocalBinder ?: return
            val service = localBinder.getService()
            trackingService = service
            isBound = true

            stateCollectJob?.cancel()
            stateCollectJob = viewModelScope.launch {
                service.walkState.collect { state ->
                    _walkState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            trackingService = null
            isBound = false
            stateCollectJob?.cancel()
        }
    }

    fun start(ghostWalkId: Long, isDemo: Boolean) {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, WalkTrackingService::class.java).apply {
            action = WalkTrackingService.ACTION_START
            putExtra(WalkTrackingService.EXTRA_GHOST_WALK_ID, ghostWalkId)
            putExtra(WalkTrackingService.EXTRA_IS_DEMO_MODE, isDemo)
        }

        context.startService(serviceIntent)
        if (!isBound) {
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun togglePauseResume() {
        val service = trackingService ?: return
        val current = _walkState.value
        if (current.isPaused || current.isAutoPaused) {
            service.resumeTracking()
        } else {
            service.pauseTracking()
        }
    }

    fun toggleVoiceGuidance() {
        trackingService?.toggleVoiceGuidance()
    }

    fun requestFinish() {
        // Pause while reviewing summary
        AppLogger.i("ActiveWalkViewModel", "requestFinish: opening summary dialog")
        trackingService?.pauseTracking()
        _suggestedTitle.value = null
        _showSummaryDialog.value = true
    }

    fun dismissSummaryDialog() {
        AppLogger.i("ActiveWalkViewModel", "dismissSummaryDialog: dismissed dialog, session remains paused")
        _showSummaryDialog.value = false
    }

    fun suggestRouteTitle() {
        if (_isSuggestingTitle.value) return
        viewModelScope.launch {
            _isSuggestingTitle.value = true
            try {
                val points = _walkState.value.currentPoints
                if (points.isEmpty()) {
                    _userMessage.emit("No GPS points recorded for this walk yet")
                    return@launch
                }
                val sampledPoints = SmartRouteNamingEngine.sampleKeyPoints(points)
                val landmarks = routeGeocoder.resolveLandmarks(sampledPoints)
                val title = SmartRouteNamingEngine.formatRouteTitle(landmarks)
                if (title != null) {
                    _suggestedTitle.value = title
                } else {
                    _userMessage.emit("Could not detect street names for this route")
                }
            } catch (e: Exception) {
                AppLogger.e("ActiveWalkViewModel", "Error resolving street route title", e)
                _userMessage.emit("Failed to resolve street names")
            } finally {
                _isSuggestingTitle.value = false
            }
        }
    }

    fun checkBatteryOptimization() {
        if (BatteryOptimizationHelper.shouldPromptBatteryOptimization(getApplication())) {
            _showBatteryOptimizationDialog.value = true
        }
    }

    fun dismissBatteryOptimizationDialog(dontAskAgain: Boolean) {
        if (dontAskAgain) {
            BatteryOptimizationHelper.setDontAskAgain(getApplication())
        }
        _showBatteryOptimizationDialog.value = false
    }

    fun openBatteryOptimizationSettings(dontAskAgain: Boolean) {
        if (dontAskAgain) {
            BatteryOptimizationHelper.setDontAskAgain(getApplication())
        }
        _showBatteryOptimizationDialog.value = false
        BatteryOptimizationHelper.openBatteryOptimizationSettings(getApplication())
    }

    fun setDemoSpeed(speedKmh: Double) {
        trackingService?.setDemoSpeed(speedKmh)
    }

    fun toggleDemoOffRoute(offRoute: Boolean) {
        isDemoSpeedOffRoute = offRoute
        trackingService?.setDemoOffRoute(offRoute)
    }

    fun saveWalk(title: String) {
        viewModelScope.launch {
            val state = _walkState.value
            AppLogger.i("ActiveWalkViewModel", "saveWalk: title=$title, points=${state.currentPoints.size}, dist=${state.distanceMeters}m")
            val now = System.currentTimeMillis()
            val startTime = state.currentPoints.firstOrNull()?.timestamp ?: (now - state.elapsedSeconds * 1000L)

            val walkEntity = WalkEntity(
                title = title,
                startTime = startTime,
                endTime = now,
                durationSeconds = state.elapsedSeconds,
                distanceMeters = state.distanceMeters,
                stepCount = state.stepCount,
                averagePaceSecondsPerKm = state.currentPaceSecondsPerKm
            )

            val pointEntities = state.currentPoints.map { pt ->
                TrackPointEntity.fromDomain(pt, walkId = 0L)
            }

            repository.saveWalk(walkEntity, pointEntities)
            AppLogger.i("ActiveWalkViewModel", "saveWalk: saved to database successfully")

            cleanupAndFinish()
        }
    }

    fun discardWalk() {
        AppLogger.i("ActiveWalkViewModel", "discardWalk: walk discarded by user")
        cleanupAndFinish()
    }

    private fun cleanupAndFinish() {
        _showSummaryDialog.value = false
        trackingService?.stopTracking()
        unbindServiceSafe()
        viewModelScope.launch {
            _navigationEvents.emit(ActiveWalkNavigationEvent.NavigateBackToHome)
        }
    }

    private fun unbindServiceSafe() {
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
            trackingService = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        stateCollectJob?.cancel()
        unbindServiceSafe()
    }
}

sealed interface ActiveWalkNavigationEvent {
    object NavigateBackToHome : ActiveWalkNavigationEvent
}
