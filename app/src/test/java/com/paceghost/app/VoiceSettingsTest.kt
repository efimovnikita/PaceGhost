package com.paceghost.app

import com.paceghost.app.domain.model.VoiceSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceSettingsTest {

    @Test
    fun `formatSeconds formats seconds and minutes correctly`() {
        assertEquals("15 sec", VoiceSettings.formatSeconds(15L))
        assertEquals("45 sec", VoiceSettings.formatSeconds(45L))
        assertEquals("1 min", VoiceSettings.formatSeconds(60L))
        assertEquals("1 min 15 sec", VoiceSettings.formatSeconds(75L))
        assertEquals("1 min 30 sec", VoiceSettings.formatSeconds(90L))
        assertEquals("3 min", VoiceSettings.formatSeconds(180L))
        assertEquals("5 min", VoiceSettings.formatSeconds(300L))
        assertEquals("10 min", VoiceSettings.formatSeconds(600L))
    }

    @Test
    fun `default values match expected specifications`() {
        val settings = VoiceSettings()
        assertEquals(60L, settings.behindCooldownSeconds)
        assertEquals(true, settings.isAheadVoiceEnabled)
        assertEquals(180L, settings.aheadCooldownSeconds)
    }
}
