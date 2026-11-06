package com.pawsup.monitoring

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.pawsup.R
import com.pawsup.break_experience.BreakOverlayActivity
import com.pawsup.data.UserPreferences
import com.pawsup.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {
    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var journal: com.pawsup.diagnostics.DiagnosticsJournal
    @Inject lateinit var repo: MonitoringRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine = UsageAccumulator()
    private var pendingId: String? = null
    private var pendingAt = 0L
    private var retryAfter = 0L
    private var lastWall = 0L
    private var lastElapsed = 0L
    private var lastSaved = 0L
    private var activeId: String? = null
    private var queryCursor = 0L
    private var historyGeneration = -1L
    private val observations = mutableListOf<com.pawsup.productivity.ObservedInterval>()
    private val bootIdentity by lazy { BootIdentity.read(this) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, getString(com.pawsup.R.string.ui_monitoring_and_breaks_5a729e), NotificationManager.IMPORTANCE_LOW))
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1001,
                notification(getString(com.pawsup.R.string.ui_starting_monitoring_6cc8fa)), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(1001, notification(getString(com.pawsup.R.string.ui_starting_monitoring_6cc8fa)))
        } catch (_: RuntimeException) {
            repo.status.value = getString(com.pawsup.R.string.ui_monitoring_unavailable_open_pawsup_to_resume_21b326)
            stopSelf(); return
        }
        repo.isServiceRunning = true
        journal.record(com.pawsup.diagnostics.DiagnosticCode.MONITOR_STARTED)
        scope.launch {
            restore()
            while (isActive) {
                try { poll() }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { journal.record(com.pawsup.diagnostics.DiagnosticCode.MONITOR_FAILED); updateStatus(getString(com.pawsup.R.string.ui_monitoring_error_open_pawsup_to_retry_869db4)) }
                delay(2_000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) scope.launch {
            prefs.setMonitorMeEnabled(false); stopSelf()
        }
        if (intent?.action == ACTION_SNOOZE) scope.launch { prefs.snooze(15) }
        return START_NOT_STICKY
    }

    private suspend fun restore() {
        val now = System.currentTimeMillis()
        engine.cursor = SystemClock.elapsedRealtime()
        queryCursor = now
        runCatching {
            val json = prefs.checkpoint()?.let(::JSONObject) ?: return@runCatching
            val saved = json.getLong("wallCursor")

            if (bootIdentity >= 0 && bootIdentity == json.getLong("boot") && now - saved in 0..300_000) {
                engine = UsageAccumulator(json.getBoolean("combined"), json.getLong("total"),
                    json.optString("foreground").takeIf { it.isNotEmpty() }, json.getBoolean("interactive"), json.getLong("cursor"))
                engine.foregroundActivity = json.optString("activity").takeIf { it.isNotEmpty() }
                val appTimes = json.optJSONObject("appTimes") ?: JSONObject()
                appTimes.keys().forEach { key -> engine.perAppMs[key] = appTimes.optLong(key).coerceAtLeast(0) }
                queryCursor = saved
            } else updateStatus(getString(com.pawsup.R.string.ui_resumed_with_an_unmeasured_gap_b4cbd6))
        }
        prefs.activeBreak()?.let { raw ->
            runCatching {
                val record = JSONObject(raw)
                if (bootIdentity < 0 || bootIdentity != record.getLong("boot") ||
                    record.getLong("deadline") < SystemClock.elapsedRealtime()) {
                    prefs.finishBreak(record.getString("id"), "interrupted", 0)
                }
            }
        }
        lastWall = now; lastElapsed = SystemClock.elapsedRealtime()
    }

    @Suppress("DEPRECATION")
    private suspend fun poll() {
        if (!prefs.snapshotMonitorMeEnabled()) { stopSelf(); return }
        if (!MonitoringAccess.hasUsage(this)) {
            journal.record(com.pawsup.diagnostics.DiagnosticCode.USAGE_REVOKED)
            updateStatus(getString(com.pawsup.R.string.ui_usage_access_needed_62ab34)); stopSelf(); return
        }
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val drift = (now - lastWall) - (elapsed - lastElapsed)
        if (kotlin.math.abs(drift) > 1_000) {
            journal.record(com.pawsup.diagnostics.DiagnosticCode.CLOCK_CHANGED)
            engine.cursor = elapsed; queryCursor = now; engine.foreground = null
            updateStatus(getString(com.pawsup.R.string.ui_clock_changed_waiting_for_an_app_visit_0044dc))
        }
        lastWall = now; lastElapsed = elapsed
        val currentGeneration = prefs.historyGeneration.first()
        if (currentGeneration != historyGeneration) { observations.clear(); historyGeneration = currentGeneration }
        val combined = prefs.combinedUsage.first()
        if (engine.combined != combined) { engine.resetBudget(); engine.combined = combined }
        engine.selected = prefs.snapshotMonitoredPackages() - packageName
        val power = getSystemService(PowerManager::class.java)
        val unlocked = !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val quiet = QuietHours.contains(hour, prefs.quietStart.first(), prefs.quietEnd.first())
        val schedule = prefs.focusSchedule.first()
        val scheduleActive = schedule.allowsAt(now)
        val snoozed = prefs.isSnoozed(elapsed, bootIdentity)
        val paused = snoozed || quiet || !scheduleActive
        val globalEnabled = prefs.globalBudgetEnabled.first()
        val limits = prefs.appLimits.first()
        engine.perAppMs.keys.retainAll(engine.selected)
        val quietStart = prefs.quietStart.first(); val quietEnd = prefs.quietEnd.first()
        engine.eligibleIntervals = { start, end ->
            com.pawsup.productivity.CalendarEligibility.intervals(now + start - elapsed, now + end - elapsed, schedule, quietStart, quietEnd)
                .map { (a, b) -> (elapsed + a - now) to (elapsed + b - now) }
        }
        engine.onUsage = { start, end ->
            observations += com.pawsup.productivity.ObservedInterval(now + start - elapsed, now + end - elapsed, usageMs = end - start)
        }
        var stored = prefs.activeBreak()?.let { runCatching { JSONObject(it) }.getOrNull() }
        // A live UI owns completion. A missing UI cannot leave monitoring paused forever.
        if (stored != null && repo.visibleBreakId != stored.optString("id") &&
            (stored.optLong("deadline") < elapsed || bootIdentity < 0 || bootIdentity != stored.optLong("boot"))) {
            prefs.finishBreak(stored.getString("id"), "interrupted", 0)
            stored = null
        }
        val storedId = stored?.optString("id")
        if (pendingId != null && repo.visibleBreakId == pendingId) {
            activeId = pendingId; pendingId = null; engine.resetBudget()
        }
        if (activeId == null && storedId != null) activeId = storedId
        if (activeId != null && storedId == null) {
            activeId = null; engine.resetBudget(); retryAfter = elapsed + 30_000
        }
        engine.paused = snoozed || activeId != null
        val events = getSystemService(UsageStatsManager::class.java).queryEvents(queryCursor, now)
        if (events == null) {
            engine.cursor = elapsed; engine.foreground = null; queryCursor = now
            updateStatus(getString(com.pawsup.R.string.ui_usage_data_unavailable_open_pawsup_to_check_acce_6f21e2))
            return
        }
        val event = UsageEvents.Event()
        // Keep a fixed lower bound: distinct transitions can share a timestamp.
        val lowerBound = queryCursor
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.timeStamp < lowerBound) continue
            val eventElapsed = (elapsed - (now - event.timeStamp)).coerceAtMost(elapsed)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> engine.resume(event.packageName, eventElapsed, event.className)
                UsageEvents.Event.MOVE_TO_BACKGROUND -> engine.pause(event.packageName, eventElapsed, event.className)
                UsageEvents.Event.ACTIVITY_STOPPED -> if (Build.VERSION.SDK_INT >= 29) engine.pause(event.packageName, eventElapsed, event.className)
                16, 17 -> engine.screen(false, eventElapsed)
                15, 18 -> engine.screen(true, eventElapsed)
            }
        }
        queryCursor = now
        if (!power.isInteractive || !unlocked) engine.screen(false, elapsed)
        engine.advance(elapsed)
        // Before API 28 no screen/keyguard usage events are emitted. Resume counting
        // only from this observation, never retroactively through an unobserved lock.
        if (power.isInteractive && unlocked && !engine.interactive) engine.screen(true, elapsed)
        repo.lastPollElapsed = elapsed
        if (pendingId != null && elapsed - pendingAt > 5_000) {
            journal.record(com.pawsup.diagnostics.DiagnosticCode.PRESENTATION_UNCONFIRMED)
            pendingId = null; retryAfter = elapsed + 60_000
            repo.status.value = getString(com.pawsup.R.string.ui_break_ready_tap_the_notification_to_open_it_2c0e59)
        }
        when {
            engine.selected.isEmpty() -> updateStatus(getString(com.pawsup.R.string.ui_choose_apps_to_monitor_6cd99a))
            paused -> updateStatus(if (quiet) getString(com.pawsup.R.string.ui_quiet_hours_706b24) else if (!scheduleActive) getString(com.pawsup.R.string.ui_outside_your_focus_schedule_d28c0f) else getString(com.pawsup.R.string.ui_paused_temporarily_6f4519))
            activeId != null -> updateStatus(getString(com.pawsup.R.string.ui_break_in_progress_aa384b))
            !power.isInteractive || !unlocked -> updateStatus(getString(com.pawsup.R.string.ui_waiting_for_an_unlocked_screen_2df9fc))
            engine.foreground !in engine.selected -> updateStatus(getString(com.pawsup.R.string.ui_waiting_for_a_selected_app_110201))
            engine.dueRule(globalEnabled, prefs.snapshotMaxMinutes(), limits) != null && elapsed >= retryAfter && pendingId == null ->
                presentBreak(elapsed, engine.dueRule(globalEnabled, prefs.snapshotMaxMinutes(), limits)!!)
            pendingId == null && elapsed >= retryAfter -> updateStatus(getString(com.pawsup.R.string.ui_monitoring_selected_apps_d7c276))
        }
        if (elapsed - lastSaved >= 5_000) {
            prefs.saveObservation(JSONObject().put("cursor", engine.cursor).put("wallCursor", queryCursor).put("boot", bootIdentity)
                .put("total", engine.totalMs).put("foreground", engine.foreground ?: "").put("activity", engine.foregroundActivity ?: "")
                .put("interactive", engine.interactive).put("combined", engine.combined).put("appTimes", JSONObject(engine.perAppMs as Map<*, *>)).toString(), observations.toList(), historyGeneration)
            observations.clear()
            lastSaved = elapsed
        }
    }

    private suspend fun presentBreak(elapsed: Long, rule: String) {
        journal.record(com.pawsup.diagnostics.DiagnosticCode.PRESENTATION_REQUESTED)
        val id = UUID.randomUUID().toString()
        val requested = prefs.snapshotActiveCatId()
        val fresh = com.pawsup.billing.EntitlementPolicy.canUseCache(prefs.entitlementsLastSynced.first(), System.currentTimeMillis())
        val cat = if (requested in setOf("miso", "mocha") || fresh && (prefs.snapshotHasCafeBundle() || requested in prefs.snapshotOwnedCatIds())) requested else "miso"
        val intent = Intent(this, BreakOverlayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(BreakOverlayActivity.EXTRA_CAT_ID, cat)
            .putExtra(BreakOverlayActivity.EXTRA_BREAK_MINUTES, prefs.snapshotBreakMinutes())
            .putExtra("session_id", id)
            .putExtra("trigger_rule", rule)
        val pending = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(1001, notification(getString(com.pawsup.R.string.ui_time_for_a_break_tap_to_start_91f8eb), pending))
        retryAfter = elapsed + 60_000
        if (Settings.canDrawOverlays(this)) {
            pendingId = id; pendingAt = elapsed
            try { startActivity(intent) } catch (_: RuntimeException) { pendingId = null }
        }
        repo.status.value = getString(com.pawsup.R.string.ui_break_ready_tap_the_notification_if_it_does_not__6a5036)
    }

    private fun updateStatus(value: String) {
        if (repo.status.value == value) return
        repo.status.value = value
        getSystemService(NotificationManager::class.java).notify(1001, notification(value))
    }

    private fun notification(message: String, content: PendingIntent? = null): Notification {
        val open = content ?: PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(name: String, code: Int) = PendingIntent.getService(this, code,
            Intent(this, MonitoringService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_launcher_cat)
            .setContentTitle("PawsUp").setContentText(message).setContentIntent(open).setOngoing(true)
            .setSilent(true).setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(com.pawsup.R.string.ui_pause_15_min_b28d39), action(ACTION_SNOOZE, 3))
            .addAction(0, "Stop", action(ACTION_STOP, 4)).build()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        repo.isServiceRunning = false
        journal.record(com.pawsup.diagnostics.DiagnosticCode.MONITOR_STOPPED)
        if (repo.status.value != getString(com.pawsup.R.string.ui_usage_access_needed_62ab34)) repo.status.value = getString(com.pawsup.R.string.ui_monitoring_stopped_open_pawsup_to_resume_96b217)
        scope.cancel(); super.onDestroy()
    }
    companion object {
        const val ACTION_STOP = "com.pawsup.STOP"
        const val ACTION_SNOOZE = "com.pawsup.SNOOZE"
        private const val CHANNEL = "pawsup_monitoring_v2"
    }
}
