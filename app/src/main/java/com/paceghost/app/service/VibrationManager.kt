package com.paceghost.app.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class VibrationManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) {
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        null
    }

    private val _hapticFeedbackEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 10)
    val hapticFeedbackEvents = _hapticFeedbackEvents.asSharedFlow()

    /**
     * Gentle double pulse: 150ms on, 100ms pause, 250ms on.
     * Triggered when the user falls behind the ghost.
     */
    fun vibrateBehind() {
        val message = "Отставание: двойная вибрация"
        emitHapticEvent(message)

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 100, 250)
                val amplitudes = intArrayOf(0, 180, 0, 220)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(longArrayOf(0, 150, 100, 250), -1)
            }
        } catch (_: Exception) {}
    }

    /**
     * Single crisp celebratory pulse (200ms).
     * Triggered when the user overtakes the ghost.
     */
    fun vibrateOvertake() {
        val message = "Обгон: победная вибрация"
        emitHapticEvent(message)

        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    private fun emitHapticEvent(message: String) {
        scope.launch {
            _hapticFeedbackEvents.emit(message)
        }
    }
}
