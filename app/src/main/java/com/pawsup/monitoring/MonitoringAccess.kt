package com.pawsup.monitoring

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

object MonitoringAccess {
    fun hasUsage(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    }

    fun start(context: Context, repo: MonitoringRepository? = null): Boolean {
        if (!hasUsage(context)) {
            repo?.status?.value = "Usage access needed"
            return false
        }
        return try {
            ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java))
            true
        } catch (_: RuntimeException) {
            repo?.status?.value = "Monitoring unavailable. Open PawsUp to resume."
            false
        }
    }
}
