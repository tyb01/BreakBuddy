package com.pawsup.monitoring

import org.junit.Assert.*
import org.junit.Test

class UsageAccumulatorTest {
    private fun engine(combined: Boolean = true) = UsageAccumulator(combined, cursor = 1).apply {
        selected = setOf("a", "b")
    }
    @Test fun combinedAppSwitchingDoesNotEraseTime() {
        val e = engine(); e.resume("a", 1); e.resume("b", 480001); e.advance(900001)
        assertTrue(e.due(15)); assertEquals(900000, e.totalMs)
    }
    @Test fun lockTimeDoesNotCount() {
        val e = engine(); e.resume("a", 1); e.screen(false, 60001); e.advance(1260001)
        assertEquals(60000, e.totalMs)
    }
    @Test fun repeatedOrOldEventsCannotDoubleCount() {
        val e = engine(); e.resume("a", 1); e.advance(60001); e.advance(60001); e.advance(1)
        assertEquals(60000, e.totalMs)
    }
    @Test fun perVisitResetsWhenLeaving() {
        val e = engine(false); e.resume("a", 1); e.resume("b", 480001); e.advance(900001)
        assertFalse(e.due(15)); assertEquals(420000, e.totalMs)
    }
    @Test fun latePauseFromOtherActivityDoesNotEndCurrentApp() {
        val e = engine(); e.resume("a", 1); e.resume("b", 1001); e.pause("a", 1001)
        e.advance(2001); assertEquals(2000, e.totalMs); assertEquals("b", e.foreground)
    }
    @Test fun explicitPauseExcludesUsage() {
        val e = engine(); e.resume("a", 1); e.paused = true; e.advance(900001)
        assertEquals(0, e.totalMs)
    }
    @Test fun quietHoursCrossMidnight() {
        assertTrue(QuietHours.contains(23, 22, 7)); assertTrue(QuietHours.contains(6, 22, 7))
        assertFalse(QuietHours.contains(7, 22, 7)); assertFalse(QuietHours.contains(12, 0, 0))
    }
    @Test fun staleTransitionsCannotChangeCurrentAppOrScreen() {
        val e = engine(); e.resume("a", 1); e.advance(60001)
        e.resume("b", 1000); e.pause("a", 2000); e.screen(false, 3000)
        e.advance(120001)
        assertEquals("a", e.foreground); assertTrue(e.interactive); assertEquals(120000L, e.totalMs)
    }
    @Test fun unselectedAppDoesNotConsumeSharedBudget() {
        val e = engine(); e.resume("a", 1); e.resume("other", 60001); e.advance(900001)
        assertEquals(60000L, e.totalMs)
    }
    @Test fun checkpointRestorationCountsKnownGapOnce() {
        val e = UsageAccumulator(true, 60000, "a", true, 60001).apply { selected = setOf("a") }
        e.advance(120001); e.advance(120001); assertEquals(120000L, e.totalMs)
    }
    @Test fun screenUnlockDoesNotInventForegroundApp() {
        val e = engine(); e.resume("a", 1); e.screen(false, 60001); e.screen(true, 120001); e.advance(180001)
        assertEquals(60000L, e.totalMs)
    }
    @Test fun latePauseFromPreviousActivityInSameAppDoesNotStopCounting() {
        val e = engine(false)
        e.resume("a", 1, "First"); e.resume("a", 60001, "Second"); e.pause("a", 60001, "First")
        e.advance(120001)
        assertEquals("a", e.foreground); assertEquals(120000L, e.totalMs)
    }
    @Test fun samePackageActivityHandoffDoesNotResetPerVisitBudget() {
        val e = engine(false)
        e.resume("a", 1, "First"); e.pause("a", 60001, "First"); e.resume("a", 60001, "Second")
        e.advance(120001); assertEquals(120000L, e.totalMs)
        e.resume("home", 120001); e.resume("a", 120002, "Second")
        assertEquals(0L, e.totalMs)
    }
}
