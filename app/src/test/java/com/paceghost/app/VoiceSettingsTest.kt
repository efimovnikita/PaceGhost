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
        assertEquals(5.0, settings.targetSpeedKmh, 0.001)
        assertEquals(0.30, settings.toleranceKmh, 0.001)
    }

    @Test
    fun `formatTargetSpeed formats speed and pace`() {
        val formatted = VoiceSettings.formatTargetSpeed(5.0)
        assertEquals("5.0 km/h • 12:00/km", formatted)
    }

    @Test
    fun `formatToleranceRange formats range`() {
        val formatted = VoiceSettings.formatToleranceRange(5.0, 0.3)
        assertEquals("Silent corridor: 4.70 – 5.30 km/h", formatted)
    }

    @Test
    fun `formatTolerance formats tolerance range`() {
        val formatted = VoiceSettings.formatTolerance(5.0, 0.3)
        assertEquals("±0.30 km/h [4.70 – 5.30 km/h]", formatted)
    }
}
