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

/**
 * Manages device orientation tracking for Pedestrian Dead Reckoning (PDR).
 *
 * Utilizes low-power Geomagnetic Rotation Vector sensor when available,
 * falling back to Standard Rotation Vector.
 */
class OrientationSensorManager(
    private val context: Context
) : SensorEventListener {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val orientationSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    val isHardwareSensorAvailable: Boolean = orientationSensor != null

    private var isListening: Boolean = false

    private val _headingFlow = MutableStateFlow(0f)
    val headingFlow: StateFlow<Float> = _headingFlow.asStateFlow()

    val currentHeading: Float
        get() = _headingFlow.value

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun start() {
        if (isListening) return
        isListening = true
        AppLogger.i("OrientationSensor", "start: sensorAvailable=$isHardwareSensorAvailable, sensorType=${orientationSensor?.type}")

        if (isHardwareSensorAvailable && orientationSensor != null) {
            sensorManager?.registerListener(
                this,
                orientationSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        AppLogger.i("OrientationSensor", "stop: finalHeading=${_headingFlow.value}")
        if (isHardwareSensorAvailable) {
            sensorManager?.unregisterListener(this)
        }
    }

    fun reset() {
        _headingFlow.value = 0f
    }

    /**
     * Directly update heading (useful for simulation and testing).
     */
    fun setHeading(headingDeg: Float) {
        val normalized = ((headingDeg % 360f) + 360f) % 360f
        _headingFlow.value = normalized
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val type = event.sensor.type
        if (type != Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR && type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // orientationAngles[0] is azimuth in radians [-PI, PI], 0 = North, PI/2 = East, -PI/2 = West
        val azimuthRad = orientationAngles[0]
        val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        val normalizedHeading = ((azimuthDeg % 360f) + 360f) % 360f

        _headingFlow.value = normalizedHeading
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op for orientation
    }
}
