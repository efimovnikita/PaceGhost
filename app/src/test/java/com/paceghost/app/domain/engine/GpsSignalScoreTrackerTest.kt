package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.GpsQuality
import com.paceghost.app.domain.model.GpsSignalSummary
import com.paceghost.app.domain.model.GpsSignalTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GpsSignalScoreTrackerTest {

    private lateinit var tracker: GpsSignalScoreTracker

    @Before
    fun setUp() {
        tracker = GpsSignalScoreTracker()
    }

    @Test
    fun `empty session returns unavailable summary and NA`() {
        val summary = tracker.currentSummary()
        assertNull(summary.scorePercent)
        assertEquals(GpsSignalTier.UNAVAILABLE, summary.tier)
        assertEquals(0, summary.totalActiveSeconds)
        assertEquals("N/A", summary.formatDisplay())
    }

    @Test
    fun `100 percent GOOD with valid fresh fix returns Good 100`() {
        repeat(5) {
            tracker.recordActiveTick(
                currentQuality = GpsQuality.GOOD,
                hasValidFix = true,
                timeSinceLastFixMs = 1000L,
                isDemoMode = false
            )
        }
        val summary = tracker.currentSummary()
        assertEquals(100, summary.scorePercent)
        assertEquals(GpsSignalTier.GOOD, summary.tier)
        assertEquals(5, summary.totalActiveSeconds)
        assertEquals("Good · 100%", summary.formatDisplay())
    }

    @Test
    fun `100 percent WEAK returns Fair 50`() {
        repeat(4) {
            tracker.recordActiveTick(
                currentQuality = GpsQuality.WEAK,
                hasValidFix = true,
                timeSinceLastFixMs = 1000L,
                isDemoMode = false
            )
        }
        val summary = tracker.currentSummary()
        assertEquals(50, summary.scorePercent)
        assertEquals(GpsSignalTier.FAIR, summary.tier)
        assertEquals(4, summary.totalActiveSeconds)
        assertEquals("Fair · 50%", summary.formatDisplay())
    }

    @Test
    fun `100 percent PDR_ACTIVE returns Poor 0`() {
        repeat(3) {
            tracker.recordActiveTick(
                currentQuality = GpsQuality.PDR_ACTIVE,
                hasValidFix = true,
                timeSinceLastFixMs = 1000L,
                isDemoMode = false
            )
        }
        val summary = tracker.currentSummary()
        assertEquals(0, summary.scorePercent)
        assertEquals(GpsSignalTier.POOR, summary.tier)
        assertEquals(3, summary.totalActiveSeconds)
        assertEquals("Poor · 0%", summary.formatDisplay())
    }

    @Test
    fun `mixed scenarios and rounding produce expected scores`() {
        // 8 GOOD + 2 WEAK = (8 + 1) / 10 = 90%
        repeat(8) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        repeat(2) {
            tracker.recordActiveTick(GpsQuality.WEAK, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        val first = tracker.currentSummary()
        assertEquals(90, first.scorePercent)
        assertEquals(GpsSignalTier.GOOD, first.tier)
        assertEquals("Good · 90%", first.formatDisplay())

        // Reset and test 1 GOOD + 1 WEAK + 1 PDR = 1.5 / 3 = 50%
        tracker.reset()
        tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        tracker.recordActiveTick(GpsQuality.WEAK, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        val second = tracker.currentSummary()
        assertEquals(50, second.scorePercent)
        assertEquals(GpsSignalTier.FAIR, second.tier)
        assertEquals("Fair · 50%", second.formatDisplay())

        // Reset and test rounding: 2 GOOD + 1 PDR = 2 / 3 = 66.67% -> 67%
        tracker.reset()
        repeat(2) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        val third = tracker.currentSummary()
        assertEquals(67, third.scorePercent)
        assertEquals(GpsSignalTier.FAIR, third.tier)
        assertEquals("Fair · 67%", third.formatDisplay())
    }

    @Test
    fun `exact tier boundaries 80 79 50 49 map to expected tiers`() {
        // 80% -> GOOD (80 GOOD + 20 PDR)
        repeat(80) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        repeat(20) {
            tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        assertEquals(80, tracker.currentSummary().scorePercent)
        assertEquals(GpsSignalTier.GOOD, tracker.currentSummary().tier)

        // 79% -> FAIR (79 GOOD + 21 PDR)
        tracker.reset()
        repeat(79) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        repeat(21) {
            tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        assertEquals(79, tracker.currentSummary().scorePercent)
        assertEquals(GpsSignalTier.FAIR, tracker.currentSummary().tier)

        // 50% -> FAIR (50 GOOD + 50 PDR)
        tracker.reset()
        repeat(50) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        repeat(50) {
            tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        assertEquals(50, tracker.currentSummary().scorePercent)
        assertEquals(GpsSignalTier.FAIR, tracker.currentSummary().tier)

        // 49% -> POOR (49 GOOD + 51 PDR)
        tracker.reset()
        repeat(49) {
            tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        repeat(51) {
            tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        }
        assertEquals(49, tracker.currentSummary().scorePercent)
        assertEquals(GpsSignalTier.POOR, tracker.currentSummary().tier)
    }

    @Test
    fun `real mode before first valid fix counts as PDR_ACTIVE even if quality is GOOD`() {
        val summary = tracker.recordActiveTick(
            currentQuality = GpsQuality.GOOD,
            hasValidFix = false,
            timeSinceLastFixMs = Long.MAX_VALUE,
            isDemoMode = false
        )
        assertEquals(0, summary.scorePercent)
        assertEquals(GpsSignalTier.POOR, summary.tier)
        assertEquals(1, summary.totalActiveSeconds)
        assertEquals("Poor · 0%", summary.formatDisplay())
    }

    @Test
    fun `real mode stale fix threshold strictly separates 4500ms and 4501ms`() {
        // 4500ms -> still counts as GOOD
        val freshSummary = tracker.recordActiveTick(
            currentQuality = GpsQuality.GOOD,
            hasValidFix = true,
            timeSinceLastFixMs = 4500L,
            isDemoMode = false
        )
        assertEquals(100, freshSummary.scorePercent)
        assertEquals(GpsSignalTier.GOOD, freshSummary.tier)

        // 4501ms -> counts as PDR_ACTIVE (1 GOOD + 1 PDR = 50%)
        val staleSummary = tracker.recordActiveTick(
            currentQuality = GpsQuality.GOOD,
            hasValidFix = true,
            timeSinceLastFixMs = 4501L,
            isDemoMode = false
        )
        assertEquals(50, staleSummary.scorePercent)
        assertEquals(GpsSignalTier.FAIR, staleSummary.tier)
        assertEquals(2, staleSummary.totalActiveSeconds)
    }

    @Test
    fun `demo mode uses currentQuality even when hasValidFix is false`() {
        repeat(3) {
            tracker.recordActiveTick(
                currentQuality = GpsQuality.GOOD,
                hasValidFix = false,
                timeSinceLastFixMs = Long.MAX_VALUE,
                isDemoMode = true
            )
        }
        val summary = tracker.currentSummary()
        assertEquals(100, summary.scorePercent)
        assertEquals(GpsSignalTier.GOOD, summary.tier)
        assertEquals("Good · 100%", summary.formatDisplay())
    }

    @Test
    fun `reset clears all accumulated state between walks`() {
        tracker.recordActiveTick(GpsQuality.GOOD, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        assertEquals(100, tracker.currentSummary().scorePercent)

        tracker.reset()
        val afterReset = tracker.currentSummary()
        assertEquals(GpsSignalSummary(), afterReset)
        assertNull(afterReset.scorePercent)
        assertEquals(GpsSignalTier.UNAVAILABLE, afterReset.tier)
        assertEquals(0, afterReset.totalActiveSeconds)

        // New walk starts fresh
        val newWalk = tracker.recordActiveTick(GpsQuality.PDR_ACTIVE, hasValidFix = true, timeSinceLastFixMs = 500L, isDemoMode = false)
        assertEquals(0, newWalk.scorePercent)
        assertEquals(1, newWalk.totalActiveSeconds)
    }
}
