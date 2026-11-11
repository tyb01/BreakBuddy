package com.pawsup.settings

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawsup.billing.BillingRepository
import com.pawsup.billing.Entitlements
import com.pawsup.cats.Cat
import com.pawsup.cats.CatRegistry
import com.pawsup.data.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val activeCat: Cat? = null,
    val visitLimitMin: Int = 15,
    val breakDurationMin: Int = 2,
    val monitoredCount: Int = 0,
    val entitlements: Entitlements = Entitlements.DEFAULT,
    val usagePermGranted: Boolean = false,
    val overlayPermGranted: Boolean = false,
    val notifPermGranted: Boolean = false,
    val monitorMeEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: UserPreferences,
    private val catRegistry: CatRegistry,
    private val billingRepo: BillingRepository,
    val monitoring: com.pawsup.monitoring.MonitoringRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.activeCatId,
                prefs.maxMinutesPerVisit,
                prefs.breakDurationMinutes,
                prefs.monitoredPackages,
                combine(billingRepo.entitlements, prefs.monitorMeEnabled) { e, m -> Pair(e, m) }
            ) { catId, maxMin, breakMin, monitored, (entitlements, monitorMe) ->
                SettingsUiState(
                    activeCat = catRegistry.find(if (entitlements.owns(catId)) catId else "miso"),
                    visitLimitMin = maxMin,
                    breakDurationMin = breakMin,
                    monitoredCount = monitored.size,
                    entitlements = entitlements,
                    usagePermGranted = hasUsagePermission(),
                    overlayPermGranted = Settings.canDrawOverlays(context),
                    notifPermGranted = hasNotifPermission(),
                    monitorMeEnabled = monitorMe
                )
            }.collect { _state.value = it }
        }
    }

    fun setVisitLimit(minutes: Int) {
        viewModelScope.launch { prefs.setMaxMinutesPerVisit(minutes) }
    }

    fun setBreakDuration(minutes: Int) {
        viewModelScope.launch { prefs.setBreakDurationMinutes(minutes) }
    }

    fun setActiveCat(catId: String) {
        viewModelScope.launch { if (state.value.entitlements.owns(catId)) prefs.setActiveCatId(catId) }
    }

    fun setMonitorMe(enabled: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            if (enabled && prefs.snapshotMonitoredPackages().isEmpty()) {
                monitoring.status.value = context.getString(com.pawsup.R.string.ui_choose_apps_before_starting_monitoring_ec57d3)
                return@launch
            }
            prefs.setMonitorMeEnabled(enabled)
            val intent = android.content.Intent(context, com.pawsup.monitoring.MonitoringService::class.java)
            if (enabled) {
                com.pawsup.monitoring.MonitoringAccess.start(context, monitoring)
            } else {
                context.stopService(intent)
            }
        }
    }

    val health = flow {
        while (true) {
            val stale = monitoring.isServiceRunning && monitoring.lastPollElapsed > 0 &&
                android.os.SystemClock.elapsedRealtime() - monitoring.lastPollElapsed > 10_000
            emit(if (stale) context.getString(com.pawsup.R.string.ui_monitoring_status_is_stale_stop_and_resume_to_re_ae9847) else monitoring.status.value)
            kotlinx.coroutines.delay(1_000)
        }
    }
    val billingMessage = billingRepo.message
    fun restorePurchases() = billingRepo.refresh()

    fun refreshPermissions() {
        billingRepo.refresh()
        _state.update { it.copy(
            usagePermGranted = hasUsagePermission(),
            overlayPermGranted = Settings.canDrawOverlays(context),
            notifPermGranted = hasNotifPermission()
        )}
    }

    private fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasNotifPermission(): Boolean {
        return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
