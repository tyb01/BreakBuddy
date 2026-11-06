package com.pawsup.monitoring

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepository @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context) {
    @Volatile var isServiceRunning: Boolean = false
    val status = kotlinx.coroutines.flow.MutableStateFlow(context.getString(com.pawsup.R.string.ui_monitoring_needs_to_be_started_643fc9))
    @Volatile var lastPollElapsed: Long = 0
    @Volatile var visibleBreakId: String? = null
}
