package com.pawsup.break_experience

/** All time inputs are elapsedRealtime; changing wall time cannot affect these rules. */
object BreakTiming {
    fun canRestore(savedBoot: Long, currentBoot: Long, deadline: Long, now: Long) =
        currentBoot >= 0 && savedBoot == currentBoot && deadline > now
    fun remainingSeconds(deadline: Long, now: Long): Int =
        ((deadline - now).coerceAtLeast(0) / 1000 + if ((deadline - now).coerceAtLeast(0) % 1000 > 0) 1 else 0)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}
