package com.pawsup.apppicker

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsRepository @Inject constructor(
    private val pm: PackageManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private var cachedApps: List<InstalledApp>? = null
    private var cacheTime: Long = 0L
    private val iconCache = LruCache<String, Drawable>(100)

    suspend fun getInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (cachedApps != null && now - cacheTime < 60_000L) return@withContext cachedApps!!

        val result = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .mapNotNull { info ->
                runCatching {
                val pkg = info.activityInfo.packageName
                val icon = iconCache.get(pkg) ?: info.loadIcon(pm).also { iconCache.put(pkg, it) }
                InstalledApp(pkg, info.loadLabel(pm).toString(), icon)
                }.getOrNull()
            }
            .sortedBy { it.displayName.lowercase() }

        cachedApps = result
        cacheTime = now
        result
    }
}
