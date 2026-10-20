package com.paceghost.app.domain.model

data class VoiceSettings(
    val behindCooldownSeconds: Long = DEFAULT_BEHIND_COOLDOWN_SECONDS,
    val isAheadVoiceEnabled: Boolean = DEFAULT_AHEAD_VOICE_ENABLED,
    val aheadCooldownSeconds: Long = DEFAULT_AHEAD_COOLDOWN_SECONDS
) {
    companion object {
        const val DEFAULT_BEHIND_COOLDOWN_SECONDS = 60L
        const val MIN_BEHIND_COOLDOWN_SECONDS = 15L
        const val MAX_BEHIND_COOLDOWN_SECONDS = 300L
        const val BEHIND_COOLDOWN_STEP_SECONDS = 15L

        const val DEFAULT_AHEAD_VOICE_ENABLED = true

        const val DEFAULT_AHEAD_COOLDOWN_SECONDS = 180L
        const val MIN_AHEAD_COOLDOWN_SECONDS = 30L
        const val MAX_AHEAD_COOLDOWN_SECONDS = 600L
        const val AHEAD_COOLDOWN_STEP_SECONDS = 30L

        fun formatSeconds(seconds: Long): String {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            return when {
                minutes > 0 && remainingSeconds > 0 -> "$minutes min $remainingSeconds sec"
                minutes > 0 -> "$minutes min"
                else -> "$remainingSeconds sec"
            }
        }
    }
}
