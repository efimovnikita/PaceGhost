package com.paceghost.app.data.local

import android.content.Context
import com.paceghost.app.domain.model.VoiceSettings

class VoiceSettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getVoiceSettings(): VoiceSettings {
        val behind = prefs.getLong(KEY_BEHIND_COOLDOWN, VoiceSettings.DEFAULT_BEHIND_COOLDOWN_SECONDS)
        val isAheadEnabled = prefs.getBoolean(KEY_AHEAD_ENABLED, VoiceSettings.DEFAULT_AHEAD_VOICE_ENABLED)
        val ahead = prefs.getLong(KEY_AHEAD_COOLDOWN, VoiceSettings.DEFAULT_AHEAD_COOLDOWN_SECONDS)
        val targetSpeed = prefs.getFloat(KEY_TARGET_SPEED_KMH, VoiceSettings.DEFAULT_TARGET_SPEED_KMH.toFloat()).toDouble()
        val tolerance = prefs.getFloat(KEY_TOLERANCE_KMH, VoiceSettings.DEFAULT_TOLERANCE_KMH.toFloat()).toDouble()
        return VoiceSettings(
            behindCooldownSeconds = behind,
            isAheadVoiceEnabled = isAheadEnabled,
            aheadCooldownSeconds = ahead,
            targetSpeedKmh = targetSpeed,
            toleranceKmh = tolerance
        )
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        prefs.edit()
            .putLong(KEY_BEHIND_COOLDOWN, settings.behindCooldownSeconds)
            .putBoolean(KEY_AHEAD_ENABLED, settings.isAheadVoiceEnabled)
            .putLong(KEY_AHEAD_COOLDOWN, settings.aheadCooldownSeconds)
            .putFloat(KEY_TARGET_SPEED_KMH, settings.targetSpeedKmh.toFloat())
            .putFloat(KEY_TOLERANCE_KMH, settings.toleranceKmh.toFloat())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "paceghost_voice_settings"
        private const val KEY_BEHIND_COOLDOWN = "behind_cooldown_seconds"
        private const val KEY_AHEAD_ENABLED = "ahead_voice_enabled"
        private const val KEY_AHEAD_COOLDOWN = "ahead_cooldown_seconds"
        private const val KEY_TARGET_SPEED_KMH = "target_speed_kmh"
        private const val KEY_TOLERANCE_KMH = "tolerance_kmh"
    }
}
