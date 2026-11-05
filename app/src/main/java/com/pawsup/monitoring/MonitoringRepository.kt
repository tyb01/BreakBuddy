package com.pawsup.monitoring

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoringRepository @Inject constructor() {
    @Volatile var isServiceRunning: Boolean = false
    val status = kotlinx.coroutines.flow.MutableStateFlow("Monitoring needs to be started")
    @Volatile var lastPollElapsed: Long = 0
    @Volatile var visibleBreakId: String? = null
}
