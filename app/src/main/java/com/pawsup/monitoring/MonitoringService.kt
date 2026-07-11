package com.pawsup.monitoring

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pawsup.R
import com.pawsup.break_experience.BreakOverlayActivity
import com.pawsup.data.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MonitoringService : Service() {

    @Inject lateinit var prefs: UserPreferences
    @Inject lateinit var repo: MonitoringRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentSessionPackage: String? = null
    private var sessionStartTime: Long = 0L
    private var breakFireable = true

    override fun onCreate() {
        super.onCreate()
        repo.isServiceRunning = true
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, notification)
        }
        WatchdogWorker.enqueue(this)
        AlarmScheduler.schedule(this)
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            scope.launch {
                if (!prefs.snapshotMonitorMeEnabled()) stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        repo.isServiceRunning = false
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                try { onPoll() } catch (e: Exception) { /* keep polling on any error */ }
                delay(2_000)
            }
        }
    }

    private suspend fun onPoll() {
        val monitored = prefs.snapshotMonitoredPackages()
        if (monitored.isEmpty()) return

        val maxMin = prefs.snapshotMaxMinutes()
        val windowMs = maxOf(maxMin * 60_000L * 2, 600_000L)

        val foreground = getForegroundPackage(windowMs)

        if (foreground == null || foreground !in monitored) {
            if (currentSessionPackage != null) {
                currentSessionPackage = null
                breakFireable = true
            }
            return
        }

        if (foreground != currentSessionPackage) {
            currentSessionPackage = foreground
            sessionStartTime = System.currentTimeMillis()
            breakFireable = true
            return
        }

        val elapsedSec = (System.currentTimeMillis() - sessionStartTime) / 1_000
        if (elapsedSec >= maxMin * 60L && breakFireable) {
            breakFireable = false
            fireBreakOverlay()
        }
    }

    private suspend fun fireBreakOverlay() {
        val catId = prefs.snapshotActiveCatId()
        val breakMin = prefs.snapshotBreakMinutes()
        startActivity(
            Intent(this, BreakOverlayActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(BreakOverlayActivity.EXTRA_CAT_ID, catId)
                .putExtra(BreakOverlayActivity.EXTRA_BREAK_MINUTES, breakMin)
        )
    }

    private fun getForegroundPackage(windowMs: Long): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - windowMs, now) ?: return null

        var latestPkg: String? = null
        var latestTime = 0L

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp > latestTime) {
                latestPkg = event.packageName
                latestTime = event.timeStamp
            }
        }
        return latestPkg
    }

    private fun buildNotification(): Notification {
        val channelId = "pawsup_monitoring"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, getString(R.string.notification_monitoring_title),
                    NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) }
            )
        }
        val openIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_cat)
            .setContentTitle(getString(R.string.notification_monitoring_title))
            .setContentText(getString(R.string.notification_monitoring_body))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .build()
    }

    fun resetSession() {
        currentSessionPackage = null
        breakFireable = true
    }

    companion object {
        private const val NOTIF_ID = 1001
    }
}
