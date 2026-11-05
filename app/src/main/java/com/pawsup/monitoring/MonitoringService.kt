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
    private val bootIdentity by lazy { BootIdentity.read(this) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Monitoring and breaks", NotificationManager.IMPORTANCE_LOW))
        }
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(1001,
                notification("Starting monitoring"), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(1001, notification("Starting monitoring"))
        } catch (_: RuntimeException) {
            repo.status.value = "Monitoring unavailable. Open PawsUp to resume."
            stopSelf(); return
        }
        repo.isServiceRunning = true
        scope.launch {
            restore()
            while (isActive) {
                try { poll() }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { updateStatus("Monitoring error. Open PawsUp to retry.") }
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
                queryCursor = saved
            } else updateStatus("Resumed with an unmeasured gap")
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
            updateStatus("Usage access needed"); stopSelf(); return
        }
        val now = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        val drift = (now - lastWall) - (elapsed - lastElapsed)
        if (kotlin.math.abs(drift) > 1_000) {
            engine.cursor = elapsed; queryCursor = now; engine.foreground = null
            updateStatus("Clock changed; waiting for an app visit")
        }
        lastWall = now; lastElapsed = elapsed
        val combined = prefs.combinedUsage.first()
        if (engine.combined != combined) { engine.resetBudget(); engine.combined = combined }
        engine.selected = prefs.snapshotMonitoredPackages() - packageName
        val power = getSystemService(PowerManager::class.java)
        val unlocked = !getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val quiet = QuietHours.contains(hour, prefs.quietStart.first(), prefs.quietEnd.first())
        val paused = prefs.isSnoozed(elapsed, bootIdentity) || quiet
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
        engine.paused = paused || activeId != null
        val events = getSystemService(UsageStatsManager::class.java).queryEvents(queryCursor, now)
        if (events == null) {
            engine.cursor = elapsed; engine.foreground = null; queryCursor = now
            updateStatus("Usage data unavailable. Open PawsUp to check access.")
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
            pendingId = null; retryAfter = elapsed + 60_000
            repo.status.value = "Break ready. Tap the notification to open it."
        }
        when {
            engine.selected.isEmpty() -> updateStatus("Choose apps to monitor")
            paused -> updateStatus(if (quiet) "Quiet hours" else "Paused temporarily")
            activeId != null -> updateStatus("Break in progress")
            !power.isInteractive || !unlocked -> updateStatus("Waiting for an unlocked screen")
            engine.foreground !in engine.selected -> updateStatus("Waiting for a selected app")
            engine.due(prefs.snapshotMaxMinutes()) && elapsed >= retryAfter && pendingId == null -> presentBreak(elapsed)
            pendingId == null && elapsed >= retryAfter -> updateStatus("Monitoring selected apps")
        }
        if (elapsed - lastSaved >= 5_000) {
            prefs.checkpoint(JSONObject().put("cursor", engine.cursor).put("wallCursor", queryCursor).put("boot", bootIdentity)
                .put("total", engine.totalMs).put("foreground", engine.foreground ?: "").put("activity", engine.foregroundActivity ?: "")
                .put("interactive", engine.interactive).put("combined", engine.combined).toString())
            lastSaved = elapsed
        }
    }

    private suspend fun presentBreak(elapsed: Long) {
        val id = UUID.randomUUID().toString()
        val requested = prefs.snapshotActiveCatId()
        val fresh = com.pawsup.billing.EntitlementPolicy.canUseCache(prefs.entitlementsLastSynced.first(), System.currentTimeMillis())
        val cat = if (requested in setOf("miso", "mocha") || fresh && (prefs.snapshotHasCafeBundle() || requested in prefs.snapshotOwnedCatIds())) requested else "miso"
        val intent = Intent(this, BreakOverlayActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(BreakOverlayActivity.EXTRA_CAT_ID, cat)
            .putExtra(BreakOverlayActivity.EXTRA_BREAK_MINUTES, prefs.snapshotBreakMinutes())
            .putExtra("session_id", id)
        val pending = PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        getSystemService(NotificationManager::class.java).notify(1001, notification("Time for a break. Tap to start.", pending))
        retryAfter = elapsed + 60_000
        if (Settings.canDrawOverlays(this)) {
            pendingId = id; pendingAt = elapsed
            try { startActivity(intent) } catch (_: RuntimeException) { pendingId = null }
        }
        repo.status.value = "Break ready. Tap the notification if it does not appear."
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
            .addAction(0, "Pause 15 min", action(ACTION_SNOOZE, 3))
            .addAction(0, "Stop", action(ACTION_STOP, 4)).build()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        repo.isServiceRunning = false
        if (repo.status.value != "Usage access needed") repo.status.value = "Monitoring stopped. Open PawsUp to resume."
        scope.cancel(); super.onDestroy()
    }
    companion object {
        const val ACTION_STOP = "com.pawsup.STOP"
        const val ACTION_SNOOZE = "com.pawsup.SNOOZE"
        private const val CHANNEL = "pawsup_monitoring_v2"
    }
}
