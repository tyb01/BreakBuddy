package com.pawsup.break_experience
import org.junit.Assert.*
import org.junit.Test
class BreakTimingTest {
    @Test fun durationDoesNotFinishBeforeDeadline() {
        assertEquals(2, BreakTiming.remainingSeconds(2001, 1000))
        assertEquals(1, BreakTiming.remainingSeconds(2001, 2000))
        assertEquals(0, BreakTiming.remainingSeconds(2001, 2001))
        assertEquals(0, BreakTiming.remainingSeconds(2001, 9000))
    }
    @Test fun sameBootCanRestoreOnlyUnfinishedBreak() {
        assertTrue(BreakTiming.canRestore(7, 7, 3000, 2000))
        assertFalse(BreakTiming.canRestore(7, 7, 3000, 3000))
    }
    @Test fun rebootOrUnknownBootCannotClaimCompletedRest() {
        assertFalse(BreakTiming.canRestore(7, 8, 3000, 2000))
        assertFalse(BreakTiming.canRestore(-1, -1, 3000, 2000))
    }
}
