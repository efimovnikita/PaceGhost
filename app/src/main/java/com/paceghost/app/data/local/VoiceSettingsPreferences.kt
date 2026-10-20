package com.paceghost.app.data.local

import android.content.Context
import com.paceghost.app.domain.model.VoiceSettings

class VoiceSettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getVoiceSettings(): VoiceSettings {
        val behind = prefs.getLong(KEY_BEHIND_COOLDOWN, VoiceSettings.DEFAULT_BEHIND_COOLDOWN_SECONDS)
        val isAheadEnabled = prefs.getBoolean(KEY_AHEAD_ENABLED, VoiceSettings.DEFAULT_AHEAD_VOICE_ENABLED)
        val ahead = prefs.getLong(KEY_AHEAD_COOLDOWN, VoiceSettings.DEFAULT_AHEAD_COOLDOWN_SECONDS)
        return VoiceSettings(
            behindCooldownSeconds = behind,
            isAheadVoiceEnabled = isAheadEnabled,
            aheadCooldownSeconds = ahead
        )
    }

    fun saveVoiceSettings(settings: VoiceSettings) {
        prefs.edit()
            .putLong(KEY_BEHIND_COOLDOWN, settings.behindCooldownSeconds)
            .putBoolean(KEY_AHEAD_ENABLED, settings.isAheadVoiceEnabled)
            .putLong(KEY_AHEAD_COOLDOWN, settings.aheadCooldownSeconds)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "paceghost_voice_settings"
        private const val KEY_BEHIND_COOLDOWN = "behind_cooldown_seconds"
        private const val KEY_AHEAD_ENABLED = "ahead_voice_enabled"
        private const val KEY_AHEAD_COOLDOWN = "ahead_cooldown_seconds"
    }
}
