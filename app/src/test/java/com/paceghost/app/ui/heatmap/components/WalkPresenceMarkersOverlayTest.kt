package com.paceghost.app.ui.heatmap.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkPresenceMarkersOverlayTest {

    @Test
    fun `markers shown only strictly below zoom 10`() {
        assertTrue(WalkPresenceMarkersOverlay.shouldShowMarkers(3.0))
        assertTrue(WalkPresenceMarkersOverlay.shouldShowMarkers(9.99))
        assertFalse(WalkPresenceMarkersOverlay.shouldShowMarkers(10.0))
        assertFalse(WalkPresenceMarkersOverlay.shouldShowMarkers(16.0))
    }
}
