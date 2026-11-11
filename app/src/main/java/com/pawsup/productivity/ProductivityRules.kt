package com.pawsup.productivity

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class FocusSchedule(val enabled: Boolean = false, val days: Set<Int> = (1..5).toSet(), val startMinute: Int = 540, val endMinute: Int = 1020) {
    /** ISO weekdays 1=Monday. An overnight window belongs to its starting day. */
    fun allows(day: Int, minute: Int): Boolean {
        if (!enabled) return true
        val start = startMinute.coerceIn(0, 1439)
        val end = endMinute.coerceIn(0, 1439)
        return when {
            start == end -> day in days
            start < end -> day in days && minute in start until end
            minute >= start -> day in days
            minute < end -> (if (day == 1) 7 else day - 1) in days
            else -> false
        }
    }
    fun allowsAt(wall: Long, zone: TimeZone = TimeZone.getDefault()): Boolean {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = wall }
        val day = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        return allows(day, calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE))
    }
}

data class ObservedInterval(val startWall: Long, val endWall: Long, val usageMs: Long = 0, val breakMs: Long = 0)
data class DailyTotal(val date: String, val usageMs: Long = 0, val breakMs: Long = 0, val completed: Int = 0, val skipped: Int = 0, val interrupted: Int = 0)

object DailyBuckets {
    fun date(wall: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = zone }.format(wall)
    /** Calendar boundaries, including 23/25-hour DST days. No fixed 24-hour assumption. */
    fun split(interval: ObservedInterval, zone: TimeZone = TimeZone.getDefault()): List<DailyTotal> {
        if (interval.endWall <= interval.startWall || interval.endWall - interval.startWall > 86_400_000L * 2) return emptyList()
        val result = mutableListOf<DailyTotal>()
        var cursor = interval.startWall
        var usedUsage = 0L; var usedBreak = 0L
        val duration = interval.endWall - interval.startWall
        while (cursor < interval.endWall) {
            val calendar = Calendar.getInstance(zone).apply {
                timeInMillis = cursor
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            val end = minOf(interval.endWall, calendar.timeInMillis)
            val last = end == interval.endWall
            val usage = if (last) interval.usageMs - usedUsage else interval.usageMs * (end - cursor) / duration
            val rest = if (last) interval.breakMs - usedBreak else interval.breakMs * (end - cursor) / duration
            result += DailyTotal(date(cursor, zone), usage.coerceAtLeast(0), rest.coerceAtLeast(0))
            usedUsage += usage; usedBreak += rest; cursor = end
        }
        return result
    }
}

object CalendarEligibility {
    fun intervals(start: Long, end: Long, schedule: FocusSchedule, quietStart: Int, quietEnd: Int,
                  zone: TimeZone = TimeZone.getDefault()): List<Pair<Long, Long>> {
        if (end <= start) return emptyList()
        val result = mutableListOf<Pair<Long, Long>>()
        var cursor = start
        while (cursor < end) {
            val next = minOf(end, (Math.floorDiv(cursor, 60_000L) + 1) * 60_000L)
            val calendar = Calendar.getInstance(zone).apply { timeInMillis = cursor }
            val quiet = com.pawsup.monitoring.QuietHours.contains(calendar.get(Calendar.HOUR_OF_DAY), quietStart, quietEnd)
            if (!quiet && schedule.allowsAt(cursor, zone)) result += cursor to next
            cursor = next
        }
        return result
    }
}
