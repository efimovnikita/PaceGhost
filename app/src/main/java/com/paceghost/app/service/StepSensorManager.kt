package com.paceghost.app.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.paceghost.app.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StepSensorManager(
    private val context: Context
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val stepCounterSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isHardwareSensorAvailable: Boolean = stepCounterSensor != null

    private var initialStepCount: Int? = null
    private var isListening: Boolean = false

    private val _stepCountFlow = MutableStateFlow(0)
    val stepCountFlow: StateFlow<Int> = _stepCountFlow.asStateFlow()

    fun start() {
        if (isListening) return
        isListening = true
        AppLogger.i("StepSensor", "start: hardwareSensorAvailable=$isHardwareSensorAvailable")

        if (isHardwareSensorAvailable && stepCounterSensor != null) {
            sensorManager?.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        AppLogger.i("StepSensor", "stop: finalSessionSteps=${_stepCountFlow.value}")
        if (isHardwareSensorAvailable) {
            sensorManager?.unregisterListener(this)
        }
    }

    fun reset() {
        initialStepCount = null
        _stepCountFlow.value = 0
    }

    /**
     * Fallback step calculation when hardware sensor is unavailable.
     * Uses standard human stride length ~0.75m per step.
     */
    fun onDistanceUpdated(distanceMeters: Double) {
        if (!isHardwareSensorAvailable) {
            val estimated = (distanceMeters / 0.75).toInt().coerceAtLeast(0)
            _stepCountFlow.value = estimated
        }
    }

    /**
     * Directly set step count (e.g. for demo mode simulation).
     */
    fun setSteps(steps: Int) {
        _stepCountFlow.value = steps.coerceAtLeast(0)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val rawTotalSteps = event.values.getOrNull(0)?.toInt() ?: return

        if (initialStepCount == null) {
            initialStepCount = rawTotalSteps
            AppLogger.i("StepSensor", "Hardware step counter baseline set: $rawTotalSteps")
        }

        val baseline = initialStepCount ?: rawTotalSteps
        val sessionSteps = (rawTotalSteps - baseline).coerceAtLeast(0)
        _stepCountFlow.value = sessionSteps

        if (sessionSteps == 1 || sessionSteps % 50 == 0) {
            AppLogger.d("StepSensor", "Hardware step count update: $sessionSteps (raw: $rawTotalSteps)")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for step counter
    }
}
