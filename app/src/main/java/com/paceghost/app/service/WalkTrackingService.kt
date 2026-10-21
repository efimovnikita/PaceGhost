package com.paceghost.app.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.VoiceSettingsPreferences
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.data.repository.WalkRepositoryImpl
import com.paceghost.app.domain.engine.AutoPauseDetector
import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.engine.GhostEngine
import com.paceghost.app.domain.engine.KalmanLocationFilter
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.domain.engine.PedestrianDeadReckoningEngine
import com.paceghost.app.domain.model.GhostComparisonResult
import com.paceghost.app.domain.model.GhostStatus
import com.paceghost.app.domain.model.GpsQuality
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.domain.model.VoiceSettings
import com.paceghost.app.domain.model.WalkState
import com.paceghost.app.domain.simulator.WalkSimulator
import com.paceghost.app.util.AppLogger
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalkTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.paceghost.app.action.START"
        const val ACTION_PAUSE = "com.paceghost.app.action.PAUSE"
        const val ACTION_RESUME = "com.paceghost.app.action.RESUME"
        const val ACTION_STOP = "com.paceghost.app.action.STOP"
        const val ACTION_SET_DEMO_SPEED = "com.paceghost.app.action.SET_DEMO_SPEED"
        const val ACTION_SET_DEMO_OFF_ROUTE = "com.paceghost.app.action.SET_DEMO_OFF_ROUTE"
        const val ACTION_TOGGLE_VOICE = "com.paceghost.app.action.TOGGLE_VOICE"

        const val EXTRA_GHOST_WALK_ID = "extra_ghost_walk_id"
        const val EXTRA_IS_DEMO_MODE = "extra_is_demo_mode"
        const val EXTRA_SPEED_KMH = "extra_speed_kmh"
        const val EXTRA_IS_OFF_ROUTE = "extra_is_off_route"
        const val EXTRA_IS_VOICE_ENABLED = "extra_is_voice_enabled"
    }

    inner class LocalBinder : Binder() {
        fun getService(): WalkTrackingService = this@WalkTrackingService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var vibrationManager: VibrationManager
        private set

    lateinit var voiceGuidanceManager: VoiceGuidanceManager
        private set

    lateinit var stepSensorManager: StepSensorManager
        private set

    lateinit var orientationSensorManager: OrientationSensorManager
        private set

    val kalmanFilter = KalmanLocationFilter()
    val pdrEngine = PedestrianDeadReckoningEngine()

    lateinit var notificationHelper: NotificationHelper
        private set

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: WalkRepository

    private val autoPauseDetector = AutoPauseDetector()
    private val ghostEngine = GhostEngine()
    private var walkSimulator = WalkSimulator()

    private val _walkState = MutableStateFlow(WalkState())
    val walkState: StateFlow<WalkState> = _walkState.asStateFlow()

    private var tickerJob: Job? = null
    private var demoJob: Job? = null
    private var vibrationObservationJob: Job? = null
    private var voiceObservationJob: Job? = null
    private var lastRecordedLocation: Location? = null
    private var lastGpsUpdateTimeMs: Long = 0L
    private var isLocationUpdatesActive = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var consecutiveRejectedSpikes = 0
    private var wasInPdrMode = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleRealLocationUpdate(location)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "PaceGhost:WalkTrackingWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        }
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(4 * 60 * 60 * 1000L) // 4 hours max timeout
                    AppLogger.i("WalkTrackingService", "PARTIAL_WAKE_LOCK acquired")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("WalkTrackingService", "Failed to acquire WakeLock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    AppLogger.i("WalkTrackingService", "PARTIAL_WAKE_LOCK released")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("WalkTrackingService", "Failed to release WakeLock", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        vibrationManager = VibrationManager(applicationContext, serviceScope)
        voiceGuidanceManager = VoiceGuidanceManager(applicationContext, serviceScope)
        stepSensorManager = StepSensorManager(applicationContext)
        orientationSensorManager = OrientationSensorManager(this)
        notificationHelper = NotificationHelper(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val db = AppDatabase.getInstance(applicationContext, serviceScope)
        repository = WalkRepositoryImpl(db.walkDao())

        observeHapticFeedback()
        observeVoiceGuidance()
        observeStepSensor()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val ghostWalkId = intent.getLongExtra(EXTRA_GHOST_WALK_ID, -1L)
                val isDemo = intent.getBooleanExtra(EXTRA_IS_DEMO_MODE, false)
                startTracking(ghostWalkId, isDemo)
            }
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
            ACTION_SET_DEMO_SPEED -> {
                val speed = intent.getDoubleExtra(EXTRA_SPEED_KMH, WalkSimulator.DEFAULT_SPEED_KMH)
                setDemoSpeed(speed)
            }
            ACTION_SET_DEMO_OFF_ROUTE -> {
                val offRoute = intent.getBooleanExtra(EXTRA_IS_OFF_ROUTE, false)
                setDemoOffRoute(offRoute)
            }
            ACTION_TOGGLE_VOICE -> {
                val hasExplicit = intent.hasExtra(EXTRA_IS_VOICE_ENABLED)
                if (hasExplicit) {
                    val enabled = intent.getBooleanExtra(EXTRA_IS_VOICE_ENABLED, true)
                    setVoiceGuidanceEnabled(enabled)
                } else {
                    toggleVoiceGuidance()
                }
            }
        }
        return START_NOT_STICKY
    }

    fun startTracking(ghostWalkId: Long = -1L, isDemo: Boolean = false) {
        if (_walkState.value.isTracking) return
        AppLogger.i("WalkTrackingService", "startTracking: ghostWalkId=$ghostWalkId, isDemo=$isDemo")

        val voiceSettings = VoiceSettingsPreferences(applicationContext).getVoiceSettings()
        ghostEngine.updateVoiceSettings(
            behindCooldownSeconds = voiceSettings.behindCooldownSeconds,
            isAheadVoiceEnabled = voiceSettings.isAheadVoiceEnabled,
            aheadCooldownSeconds = voiceSettings.aheadCooldownSeconds
        )

        resetTrackingData()
        _walkState.value = WalkState(
            isTracking = true,
            isPaused = false,
            isAutoPaused = false,
            isDemoMode = isDemo,
            isVoiceGuidanceEnabled = voiceGuidanceManager.isVoiceEnabled
        )

        promoteToForeground()
        if (!isDemo) {
            acquireWakeLock()
        }

        serviceScope.launch {
            loadGhostRoute(ghostWalkId, isDemo)
            withContext(Dispatchers.Main) {
                if (isDemo) {
                    startDemoMode()
                } else {
                    startRealMode()
                }
                startTicker()
            }
        }
    }

    private fun promoteToForeground() {
        val notification = notificationHelper.buildNotification(
            steps = 0,
            distanceMeters = 0.0,
            paceSecondsPerKm = 0.0,
            isPaused = false,
            isDemo = _walkState.value.isDemoMode
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(NotificationHelper.NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private suspend fun loadGhostRoute(ghostWalkId: Long, isDemo: Boolean) {
        val points = if (ghostWalkId > 0) {
            repository.getTrackPointsForWalk(ghostWalkId)
        } else if (isDemo) {
            // In demo mode without explicit ghost, provide default Park Pobedy loop as ghost
            WalkSimulator.defaultParkPobedyLoop()
        } else {
            emptyList()
        }

        ghostEngine.setGhostRoute(points)
        if (isDemo) {
            walkSimulator = WalkSimulator(points.ifEmpty { WalkSimulator.defaultParkPobedyLoop() })
            walkSimulator.reset()
        }

        _walkState.value = _walkState.value.copy(
            ghostPoints = points,
            ghostComparison = if (points.isNotEmpty()) {
                GhostComparisonResult(
                    status = GhostStatus.ON_PACE,
                    timeDeltaSeconds = 0L,
                    distanceDeltaMeters = 0.0,
                    currentProgressMeters = 0.0,
                    totalGhostMeters = points.lastOrNull()?.cumulativeDistance ?: 0.0,
                    ghostLat = points.firstOrNull()?.lat,
                    ghostLon = points.firstOrNull()?.lon
                )
            } else {
                GhostComparisonResult.NO_GHOST
            }
        )
    }

    fun pauseTracking() {
        AppLogger.i("WalkTrackingService", "pauseTracking called")
        _walkState.value = _walkState.value.copy(isPaused = true)
        updateNotification()
    }

    fun resumeTracking() {
        AppLogger.i("WalkTrackingService", "resumeTracking called")
        _walkState.value = _walkState.value.copy(isPaused = false)
        updateNotification()
    }

    fun stopTracking() {
        AppLogger.i("WalkTrackingService", "stopTracking called")
        tickerJob?.cancel()
        demoJob?.cancel()
        stopRealMode()
        releaseWakeLock()

        _walkState.value = _walkState.value.copy(
            isTracking = false,
            isPaused = false,
            isAutoPaused = false
        )

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            AppLogger.w("WalkTrackingService", "Error stopping foreground: ${e.message}")
        }
        stopSelf()
    }

    fun setDemoSpeed(speedKmh: Double) {
        walkSimulator.setSpeedKmh(speedKmh)
    }

    fun setDemoOffRoute(offRoute: Boolean) {
        walkSimulator.setOffRoute(offRoute)
    }

    private fun startRealMode() {
        acquireWakeLock()
        orientationSensorManager.start()
        stepSensorManager.start()

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        AppLogger.i("WalkTrackingService", "startRealMode: hasFineLocation=$hasFineLocation")

        if (hasFineLocation) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500L)
                .setMinUpdateIntervalMillis(1500L)
                .setMinUpdateDistanceMeters(0f)
                .build()

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                isLocationUpdatesActive = true
                AppLogger.i("WalkTrackingService", "requestLocationUpdates registered successfully")
            } catch (e: SecurityException) {
                AppLogger.e("WalkTrackingService", "SecurityException requesting location updates", e)
            }
        } else {
            AppLogger.w("WalkTrackingService", "Cannot start location updates: ACCESS_FINE_LOCATION not granted")
        }
    }

    private fun stopRealMode() {
        AppLogger.i("WalkTrackingService", "stopRealMode called: isLocationUpdatesActive=$isLocationUpdatesActive")
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isLocationUpdatesActive = false
        }
        orientationSensorManager.stop()
        stepSensorManager.stop()
        releaseWakeLock()
    }

    private fun startDemoMode() {
        demoJob?.cancel()
        demoJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val currentState = _walkState.value
                if (!currentState.isTracking || currentState.isPaused) {
                    continue
                }

                // Advance simulator by 1 second
                val simulatedPoint = walkSimulator.tick(1.0)
                handleSimulatedUpdate(simulatedPoint)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val state = _walkState.value
                if (state.isTracking && !state.isPaused && !state.isAutoPaused) {
                    val newElapsed = state.elapsedSeconds + 1
                    val newPace = PaceCalculator.calculatePaceSecondsPerKm(state.distanceMeters, newElapsed)
                    _walkState.value = state.copy(
                        elapsedSeconds = newElapsed,
                        currentPaceSecondsPerKm = newPace
                    )
                    updateNotification()
                }
            }
        }
    }

    private fun handleSimulatedUpdate(point: TrackPoint) {
        val currentState = _walkState.value
        val speedKmh = point.speed * 3.6f

        val isAutoPaused = autoPauseDetector.update(speedKmh, currentState.elapsedSeconds)
        stepSensorManager.setSteps(point.stepCount)

        val updatedPoints = currentState.currentPoints + point
        val ghostResult = if (!isAutoPaused && currentState.ghostPoints.isNotEmpty()) {
            ghostEngine.compare(point.lat, point.lon, currentState.elapsedSeconds)
        } else if (isAutoPaused) {
            currentState.ghostComparison.copy(status = GhostStatus.PAUSED)
        } else {
            currentState.ghostComparison
        }

        checkAndTriggerFeedback(ghostResult)

        _walkState.value = currentState.copy(
            isAutoPaused = isAutoPaused,
            distanceMeters = point.cumulativeDistance,
            stepCount = point.stepCount,
            currentSpeedKmh = speedKmh.toDouble(),
            ghostComparison = ghostResult,
            currentPoints = updatedPoints,
            gpsQuality = GpsQuality.GOOD,
            currentAccuracyMeters = 5f
        )
    }

    private fun handleRealLocationUpdate(location: Location) {
        val currentState = _walkState.value
        if (!currentState.isTracking || currentState.isPaused) {
            AppLogger.d("Location", "Ignored update (tracking=${currentState.isTracking}, paused=${currentState.isPaused})")
            return
        }

        // GPS Suppression / Jamming Detection: Accuracy worse than 35m
        if (location.accuracy > 35f) {
            wasInPdrMode = true
            _walkState.value = currentState.copy(
                gpsQuality = GpsQuality.PDR_ACTIVE,
                currentAccuracyMeters = location.accuracy
            )
            AppLogger.d("Location", "Degraded GPS ignored (jammed/weak): acc=${location.accuracy}m")
            return
        }

        val lastPoint = currentState.currentPoints.lastOrNull()
        val currentSteps = stepSensorManager.stepCountFlow.value
        val deltaSteps = if (lastPoint != null) {
            (currentSteps - lastPoint.stepCount).coerceAtLeast(0)
        } else {
            0
        }

        val timeDeltaMs = if (lastPoint != null) location.time - lastPoint.timestamp else 0L
        val timeDeltaSec = if (timeDeltaMs > 0) timeDeltaMs / 1000.0 else 0.0

        val isRecoveryFromPdr = wasInPdrMode
        if (isRecoveryFromPdr) {
            if (lastPoint != null) {
                val directDist = GeoMath.haversineDistance(
                    lastPoint.lat,
                    lastPoint.lon,
                    location.latitude,
                    location.longitude
                )
                if (!pdrEngine.isKinematicallyPlausible(directDist, deltaSteps, timeDeltaSec)) {
                    consecutiveRejectedSpikes++
                    AppLogger.w(
                        "Location",
                        "PDR recovery fix rejected by kinematic gate: dist=${String.format(Locale.US, "%.1f", directDist)}m, steps=$deltaSteps, consecutive=$consecutiveRejectedSpikes"
                    )
                    if (consecutiveRejectedSpikes < 5 && timeDeltaSec < 60.0) {
                        return
                    }
                }
            }
            consecutiveRejectedSpikes = 0
            kalmanFilter.reset()
            kalmanFilter.update(location.latitude, location.longitude, location.accuracy, location.time)
            wasInPdrMode = false
            AppLogger.i("Location", "GPS recovered from PDR/blackout, re-anchored at lat=${location.latitude}, lon=${location.longitude}")
        } else {
            if (lastPoint != null) {
                val directDist = GeoMath.haversineDistance(
                    lastPoint.lat,
                    lastPoint.lon,
                    location.latitude,
                    location.longitude
                )
                if (!pdrEngine.isKinematicallyPlausible(directDist, deltaSteps, timeDeltaSec)) {
                    consecutiveRejectedSpikes++
                    AppLogger.w(
                        "Location",
                        "Spike rejected by kinematic gate: dist=${String.format(Locale.US, "%.1f", directDist)}m, steps=$deltaSteps, dt=${String.format(Locale.US, "%.1f", timeDeltaSec)}s, consecutive=$consecutiveRejectedSpikes"
                    )
                    if (consecutiveRejectedSpikes < 3 && timeDeltaSec < 45.0) {
                        return
                    } else {
                        AppLogger.i(
                            "Location",
                            "Re-anchoring after spike/gap recovery: consecutive=$consecutiveRejectedSpikes, dt=${String.format(Locale.US, "%.1f", timeDeltaSec)}s"
                        )
                        consecutiveRejectedSpikes = 0
                        kalmanFilter.reset()
                    }
                } else {
                    consecutiveRejectedSpikes = 0
                }
            }
            kalmanFilter.update(location.latitude, location.longitude, location.accuracy, location.time)
        }

        lastRecordedLocation = location
        lastGpsUpdateTimeMs = System.currentTimeMillis()

        val filteredLat = kalmanFilter.filteredLat
        val filteredLon = kalmanFilter.filteredLon
        val filteredSpeed = if (kalmanFilter.isInitialized) kalmanFilter.filteredSpeed else location.speed

        var addedDistance = 0.0
        if (lastPoint != null) {
            if (isRecoveryFromPdr) {
                // Distance was already accumulated step-by-step during blackout
                addedDistance = 0.0
            } else {
                val dist = GeoMath.haversineDistance(
                    lastPoint.lat,
                    lastPoint.lon,
                    filteredLat,
                    filteredLon
                )
                if (pdrEngine.isStationaryDrift(dist, deltaSteps, filteredSpeed)) {
                    addedDistance = 0.0
                } else {
                    addedDistance = dist
                }
            }
        }

        // Auto-calibrate stride on reliable GPS movement
        if (location.accuracy <= 15f && deltaSteps > 0 && addedDistance > 0.0) {
            pdrEngine.onGpsDistanceProgress(addedDistance, deltaSteps)
        }

        val gpsQuality = when {
            location.accuracy <= 15f -> GpsQuality.GOOD
            location.accuracy <= 30f -> GpsQuality.WEAK
            else -> GpsQuality.PDR_ACTIVE
        }

        val newDistance = currentState.distanceMeters + addedDistance
        val speedKmh = if (filteredSpeed > 0f) filteredSpeed * 3.6f else (if (location.hasSpeed()) location.speed * 3.6f else 0f)
        val isAutoPaused = autoPauseDetector.update(speedKmh, currentState.elapsedSeconds)

        stepSensorManager.onDistanceUpdated(newDistance)

        val trackPoint = TrackPoint(
            lat = filteredLat,
            lon = filteredLon,
            altitude = location.altitude,
            timestamp = location.time,
            elapsedSeconds = currentState.elapsedSeconds,
            cumulativeDistance = newDistance,
            speed = filteredSpeed,
            stepCount = currentSteps,
            isPdr = false,
            accuracy = kalmanFilter.filteredAccuracy
        )

        val updatedPoints = currentState.currentPoints + trackPoint

        val ghostResult = if (!isAutoPaused && currentState.ghostPoints.isNotEmpty()) {
            ghostEngine.compare(filteredLat, filteredLon, currentState.elapsedSeconds)
        } else if (isAutoPaused) {
            currentState.ghostComparison.copy(status = GhostStatus.PAUSED)
        } else {
            currentState.ghostComparison
        }

        checkAndTriggerFeedback(ghostResult)

        _walkState.value = currentState.copy(
            isAutoPaused = isAutoPaused,
            distanceMeters = newDistance,
            stepCount = currentSteps,
            currentSpeedKmh = speedKmh.toDouble(),
            ghostComparison = ghostResult,
            currentPoints = updatedPoints,
            gpsQuality = gpsQuality,
            currentAccuracyMeters = kalmanFilter.filteredAccuracy
        )

        AppLogger.d(
            "Location",
            "Accepted: lat=${String.format(Locale.US, "%.5f", filteredLat)}, lon=${String.format(Locale.US, "%.5f", filteredLon)}, acc=${String.format(Locale.US, "%.1f", kalmanFilter.filteredAccuracy)}m, quality=$gpsQuality, added=${String.format(Locale.US, "%.1f", addedDistance)}m, total=${String.format(Locale.US, "%.1f", newDistance)}m"
        )
    }

    fun updateVoiceSettings(settings: VoiceSettings) {
        ghostEngine.updateVoiceSettings(
            behindCooldownSeconds = settings.behindCooldownSeconds,
            isAheadVoiceEnabled = settings.isAheadVoiceEnabled,
            aheadCooldownSeconds = settings.aheadCooldownSeconds
        )
    }

    fun setVoiceGuidanceEnabled(enabled: Boolean) {
        voiceGuidanceManager.isVoiceEnabled = enabled
        _walkState.value = _walkState.value.copy(isVoiceGuidanceEnabled = enabled)
    }

    fun toggleVoiceGuidance(): Boolean {
        val newEnabled = !_walkState.value.isVoiceGuidanceEnabled
        setVoiceGuidanceEnabled(newEnabled)
        return newEnabled
    }

    private fun checkAndTriggerFeedback(result: GhostComparisonResult) {
        if (!voiceGuidanceManager.isVoiceEnabled) {
            return
        }

        // 1. Vibration
        if (result.shouldVibrateBehind) {
            vibrationManager.vibrateBehind()
        } else if (result.shouldVibrateOvertake) {
            vibrationManager.vibrateOvertake()
        } else if (result.shouldVibrateAheadReminder) {
            vibrationManager.vibrateAheadReminder()
        } else if (result.shouldVibrateOffRoute) {
            vibrationManager.vibrateOffRoute()
        }

        // 2. Voice Guidance
        if (result.shouldSpeakBehind) {
            voiceGuidanceManager.speakBehind()
        } else if (result.shouldSpeakOvertake) {
            voiceGuidanceManager.speakOvertake()
        } else if (result.shouldSpeakAheadReminder) {
            voiceGuidanceManager.speakAheadReminder()
        } else if (result.shouldSpeakOffRoute) {
            voiceGuidanceManager.speakOffRoute()
        }
    }

    private fun observeHapticFeedback() {
        vibrationObservationJob?.cancel()
        vibrationObservationJob = serviceScope.launch {
            vibrationManager.hapticFeedbackEvents.collect { message ->
                _walkState.value = _walkState.value.copy(recentVibrationMessage = message)
                // Clear message after 3.5 seconds
                launch {
                    delay(3500L)
                    if (_walkState.value.recentVibrationMessage == message) {
                        _walkState.value = _walkState.value.copy(recentVibrationMessage = null)
                    }
                }
            }
        }
    }

    private fun observeVoiceGuidance() {
        voiceObservationJob?.cancel()
        voiceObservationJob = serviceScope.launch {
            voiceGuidanceManager.voiceEvents.collect { message ->
                _walkState.value = _walkState.value.copy(recentVoiceMessage = message)
                // Clear message after 3.5 seconds
                launch {
                    delay(3500L)
                    if (_walkState.value.recentVoiceMessage == message) {
                        _walkState.value = _walkState.value.copy(recentVoiceMessage = null)
                    }
                }
            }
        }
    }

    private fun observeStepSensor() {
        serviceScope.launch {
            stepSensorManager.stepCountFlow.collect { steps ->
                handleStepCountUpdate(steps)
            }
        }
    }

    private fun handleStepCountUpdate(steps: Int) {
        val currentState = _walkState.value
        if (!currentState.isTracking) return

        if (currentState.isDemoMode || currentState.isPaused || currentState.isAutoPaused) {
            _walkState.value = currentState.copy(stepCount = steps)
            return
        }

        val lastPoint = currentState.currentPoints.lastOrNull()
        if (lastPoint == null) {
            _walkState.value = currentState.copy(stepCount = steps)
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLastGps = if (lastGpsUpdateTimeMs > 0L) now - lastGpsUpdateTimeMs else Long.MAX_VALUE
        val isAccuracyDegraded = currentState.currentAccuracyMeters > 35f
        val isGpsStale = timeSinceLastGps > 4500L

        if (isGpsStale || isAccuracyDegraded) {
            wasInPdrMode = true
            val deltaSteps = (steps - currentState.stepCount).coerceAtLeast(0)
            if (deltaSteps >= 1) {
                val addedDistance = deltaSteps * pdrEngine.currentStrideLengthMeters
                val newDistance = currentState.distanceMeters + addedDistance

                if (currentState.ghostPoints.isNotEmpty()) {
                    // Ghost Mode: advance position along ghost route geometry
                    val ghostResult = ghostEngine.advanceGhostProgress(
                        currentProgressMeters = newDistance,
                        elapsedSeconds = currentState.elapsedSeconds
                    )
                    checkAndTriggerFeedback(ghostResult)

                    val projectedPoint = GeoMath.interpolatePointAtDistance(currentState.ghostPoints, newDistance)
                    val pdrTrackPoint = projectedPoint.copy(
                        timestamp = now,
                        elapsedSeconds = currentState.elapsedSeconds,
                        cumulativeDistance = newDistance,
                        stepCount = steps,
                        isPdr = true,
                        accuracy = 25f
                    )
                    val updatedPoints = currentState.currentPoints + pdrTrackPoint

                    _walkState.value = currentState.copy(
                        distanceMeters = newDistance,
                        stepCount = steps,
                        gpsQuality = GpsQuality.PDR_ACTIVE,
                        currentAccuracyMeters = 25f,
                        ghostComparison = ghostResult,
                        currentPoints = updatedPoints
                    )
                } else {
                    // Free Walk Mode: advance distance and steps accurately without compass distortion
                    _walkState.value = currentState.copy(
                        distanceMeters = newDistance,
                        stepCount = steps,
                        gpsQuality = GpsQuality.PDR_ACTIVE,
                        currentAccuracyMeters = 30f
                    )
                }

                AppLogger.d(
                    "PDR",
                    "PDR step propagated: delta=$deltaSteps, totalSteps=$steps, stride=${String.format(Locale.US, "%.2f", pdrEngine.currentStrideLengthMeters)}m, added=${String.format(Locale.US, "%.2f", addedDistance)}m, total=${String.format(Locale.US, "%.1f", newDistance)}m"
                )
                return
            }
        }

        _walkState.value = currentState.copy(stepCount = steps)
    }

    private fun updateNotification() {
        val state = _walkState.value
        notificationHelper.updateNotification(
            steps = state.stepCount,
            distanceMeters = state.distanceMeters,
            paceSecondsPerKm = state.currentPaceSecondsPerKm,
            isPaused = state.isPaused || state.isAutoPaused,
            isDemo = state.isDemoMode
        )
    }

    private fun resetTrackingData() {
        lastRecordedLocation = null
        lastGpsUpdateTimeMs = 0L
        consecutiveRejectedSpikes = 0
        wasInPdrMode = false
        kalmanFilter.reset()
        pdrEngine.reset()
        orientationSensorManager.reset()
        stepSensorManager.reset()
        autoPauseDetector.reset()
        ghostEngine.reset()
        AppLogger.d("WalkTrackingService", "resetTrackingData completed")
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLogger.i("WalkTrackingService", "onDestroy called")
        tickerJob?.cancel()
        demoJob?.cancel()
        vibrationObservationJob?.cancel()
        voiceObservationJob?.cancel()
        stopRealMode()
        releaseWakeLock()
        voiceGuidanceManager.shutdown()
        serviceScope.cancel()
    }
}
