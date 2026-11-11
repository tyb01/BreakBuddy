package com.pawsup.productivity

import com.pawsup.monitoring.UsageAccumulator
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ProductivityRulesTest {
    private fun instant(value: String, zone: TimeZone) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { timeZone = zone }.parse(value)!!.time
    @Test fun weekdayWindowExcludesEndpointsAndWeekend() {
        val rule = FocusSchedule(true, setOf(1,2,3,4,5), 540, 1020)
        assertFalse(rule.allows(1,539)); assertTrue(rule.allows(1,540)); assertFalse(rule.allows(1,1020)); assertFalse(rule.allows(6,600))
    }
    @Test fun fridayOvernightBelongsToFridayNotSaturdaySelection() {
        val rule = FocusSchedule(true, setOf(5), 1320, 120)
        assertTrue(rule.allows(5,1380)); assertTrue(rule.allows(6,60)); assertFalse(rule.allows(6,120)); assertFalse(rule.allows(5,60))
    }
    @Test fun sundayOvernightWrapsToMonday() { assertTrue(FocusSchedule(true,setOf(7),1320,120).allows(1,60)) }
    @Test fun equalTimesMeanSelectedFullDayAndDisabledScheduleAllowsAll() {
        assertTrue(FocusSchedule(true,setOf(2),0,0).allows(2,1000)); assertFalse(FocusSchedule(true,setOf(2),0,0).allows(3,1000))
        assertTrue(FocusSchedule(false,emptySet()).allows(7,300))
    }
    @Test fun noDaysMeansNoEligibleInterval() { assertFalse(FocusSchedule(true, emptySet()).allows(1,600)) }
    @Test fun midnightSplitsWithoutLosingMilliseconds() {
        val zone=TimeZone.getTimeZone("UTC"); val start=instant("2026-11-04 23:59",zone)
        val rows=DailyBuckets.split(ObservedInterval(start,start+120000,usageMs=120000,breakMs=30000),zone)
        assertEquals(listOf("2026-11-04","2026-11-05"),rows.map{it.date})
        assertEquals(120000L, rows.sumOf{it.usageMs}); assertEquals(30000L,rows.sumOf{it.breakMs})
    }
    @Test fun springDayIs23Hours() {
        val zone=TimeZone.getTimeZone("America/New_York")
        val start=instant("2026-03-08 00:00",zone); val end=instant("2026-03-09 00:00",zone)
        val rows=DailyBuckets.split(ObservedInterval(start,end,usageMs=end-start),zone)
        assertEquals(1,rows.size); assertEquals(23*3600000L,rows.single().usageMs)
    }
    @Test fun fallDayIs25Hours() {
        val zone=TimeZone.getTimeZone("America/New_York")
        val start=instant("2026-11-01 00:00",zone); val end=instant("2026-11-02 00:00",zone)
        assertEquals(25*3600000L,DailyBuckets.split(ObservedInterval(start,end,usageMs=end-start),zone).single().usageMs)
    }
    @Test fun quietHoursOverrideScheduleAndBoundaryIsSplit() {
        val zone=TimeZone.getTimeZone("UTC"); val start=instant("2026-11-02 08:59",zone)
        val rows=CalendarEligibility.intervals(start,start+120000,FocusSchedule(true,setOf(1),540,1020),0,0,zone)
        assertEquals(60000L,rows.sumOf{it.second-it.first})
        assertTrue(CalendarEligibility.intervals(start,start+120000,FocusSchedule(),8,10,zone).isEmpty())
    }
    @Test fun separateAppLimitSurvivesSwitchesAndOnlyOneRuleWins() {
        val e=UsageAccumulator(true,cursor=1).apply{selected=setOf("a","b")}
        e.resume("a",1); e.resume("b",30001); e.resume("a",60001); e.advance(90001)
        assertEquals("app",e.dueRule(true,1,mapOf("a" to 1)))
        e.resetBudget(); assertNull(e.dueRule(true,1,mapOf("a" to 1)))
    }
    @Test fun disablingGlobalDoesNotDisableAnAppLimit() {
        val e=UsageAccumulator(cursor=1).apply{selected=setOf("a")}
        e.resume("a",1); e.advance(60001)
        assertNull(e.dueRule(false,1,emptyMap())); assertEquals("app",e.dueRule(false,1,mapOf("a" to 1)))
    }
    @Test fun scheduleFilteringAffectsUsageAndBudgetIdentically() {
        val e=UsageAccumulator(true,cursor=1).apply{ selected=setOf("a"); eligibleIntervals={a,b -> listOf((a+1000) to b)} }
        var observed=0L; e.onUsage={a,b->observed+=b-a}; e.resume("a",1); e.advance(60001)
        assertEquals(59000L,e.totalMs); assertEquals(e.totalMs,observed); assertFalse(e.due(1))
    }
}
