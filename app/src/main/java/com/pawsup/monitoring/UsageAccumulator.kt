package com.pawsup.monitoring

/** Event-time accumulator. Android input is normalized before reaching this class. */
class UsageAccumulator(
    var combined: Boolean = false,
    var totalMs: Long = 0,
    var foreground: String? = null,
    var interactive: Boolean = true,
    var cursor: Long = 0
) {
    var foregroundActivity: String? = null
    private var lastVisited: String? = foreground
    var selected: Set<String> = emptySet()
    var paused: Boolean = false
    var lastDeltaMs: Long = 0
        private set

    fun advance(time: Long) {
        lastDeltaMs = 0
        if (time <= cursor) return
        if (cursor > 0 && interactive && !paused && foreground in selected) {
            lastDeltaMs = time - cursor
            totalMs += lastDeltaMs
        }
        cursor = time
    }

    fun resume(packageName: String, time: Long, activity: String? = null) {
        if (time < cursor) return
        advance(time)
        if (!combined && lastVisited != packageName) totalMs = 0
        foreground = packageName
        foregroundActivity = activity
        lastVisited = packageName
    }

    fun pause(packageName: String, time: Long, activity: String? = null) {
        if (time < cursor) return
        advance(time)
        if (foreground == packageName && (activity == null || foregroundActivity == null || foregroundActivity == activity)) {
            foreground = null
            foregroundActivity = null
            // A same-package activity handoff is still one app visit. The next
            // different resumed package (including Home) or screen-off resets it.
        }
    }

    fun screen(on: Boolean, time: Long) {
        if (time < cursor) return
        advance(time)
        interactive = on
        if (!on) {
            foreground = null
            foregroundActivity = null
            lastVisited = null
            if (!combined) totalMs = 0
        }
    }

    fun resetBudget() { totalMs = 0 }
    fun due(limitMinutes: Int) = totalMs >= limitMinutes.coerceIn(1, 60) * 60_000L
}
