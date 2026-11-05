package com.pawsup.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawsup.apppicker.AppPickerActivity
import com.pawsup.break_experience.BreakOverlayActivity
import com.pawsup.cats.CatRegistry
import com.pawsup.paywall.PaywallActivity
import com.pawsup.ui.CatPosterImage
import com.pawsup.ui.theme.PawsUpTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()
    @Inject lateinit var cats: CatRegistry
    override fun onResume() { super.onResume(); vm.refreshPermissions() }
    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            android.widget.Toast.makeText(this, "This settings page is not available on your device.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PawsUpTheme {
            val state by vm.state.collectAsStateWithLifecycle()
            val health by vm.health.collectAsStateWithLifecycle(initialValue = "Checking monitoring status...")
            val recovered by vm.prefs.recoveredCorruption.collectAsStateWithLifecycle(initialValue = false)
            val combined by vm.prefs.combinedUsage.collectAsStateWithLifecycle(initialValue = false)
            val sound by vm.prefs.soundEnabled.collectAsStateWithLifecycle(initialValue = false)
            val motion by vm.prefs.reduceMotion.collectAsStateWithLifecycle(initialValue = false)
            val history by vm.prefs.history.collectAsStateWithLifecycle(initialValue = "[]")
            val quietStart by vm.prefs.quietStart.collectAsStateWithLifecycle(initialValue = 0)
            val quietEnd by vm.prefs.quietEnd.collectAsStateWithLifecycle(initialValue = 0)
            val billingMessage by vm.billingMessage.collectAsStateWithLifecycle()
            val scope = rememberCoroutineScope()
            var tab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
            var confirmReset by remember { mutableStateOf(false) }
            var confirmDelete by remember { mutableStateOf(false) }
            Scaffold(topBar = { TopAppBar(title = { Text(stringResource(com.pawsup.R.string.production_pawsup)) }) }, bottomBar = {
                NavigationBar {
                    listOf("Home", "Cafe", "Reliability").forEachIndexed { index, title ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Text((index + 1).toString()) }, label = { Text(title) })
                    }
                }
            }) { inset ->
                Column(Modifier.padding(inset).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (recovered) Text(stringResource(com.pawsup.R.string.production_saved_settings_could_not_be_read_and_were_recovered_with_safe_def))
                    when (tab) {
                        0 -> {
                            Text(stringResource(com.pawsup.R.string.production_your_break_companion), style = MaterialTheme.typography.headlineMedium)
                            state.activeCat?.let { CatPosterImage(it, Modifier.size(100.dp)); Text(it.displayName) }
                            Text(if (state.monitoredCount == 0) "Choose apps before starting monitoring." else health)
                            Toggle(stringResource(com.pawsup.R.string.production_monitor_selected_apps), state.monitorMeEnabled) { vm.setMonitorMe(it, this@SettingsActivity) }
                            Text(stringResource(com.pawsup.R.string.production_after_a_restart_or_system_stop_open_pawsup_to_resume_breaks_are_v))
                            OutlinedButton(onClick = { open(Intent(this@SettingsActivity, AppPickerActivity::class.java)) }) { Text("Choose apps (${state.monitoredCount})") }
                            Toggle(stringResource(com.pawsup.R.string.production_combine_time_across_selected_apps), combined) { scope.launch { vm.prefs.setCombinedUsage(it) } }
                            Text(if (combined) "Switching between selected apps keeps the same budget." else "Each continuous app visit starts a new timer.")
                            MinuteControl(stringResource(com.pawsup.R.string.production_usage_limit), state.visitLimitMin, 1..60, vm::setVisitLimit)
                            MinuteControl(stringResource(com.pawsup.R.string.production_break_length), state.breakDurationMin, 1..30, vm::setBreakDuration)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { scope.launch { vm.prefs.snooze(15) } }) { Text(stringResource(com.pawsup.R.string.production_pause_15_min)) }
                                OutlinedButton(onClick = { scope.launch { vm.prefs.snooze(0) }; vm.setMonitorMe(true, this@SettingsActivity) }) { Text(stringResource(com.pawsup.R.string.production_resume)) }
                            }
                            OutlinedButton(onClick = {
                                open(Intent(this@SettingsActivity, BreakOverlayActivity::class.java)
                                    .putExtra(BreakOverlayActivity.EXTRA_CAT_ID, state.activeCat?.id ?: "miso")
                                    .putExtra(BreakOverlayActivity.EXTRA_BREAK_MINUTES, state.breakDurationMin))
                            }) { Text(stringResource(com.pawsup.R.string.production_try_a_break)) }
                            Toggle(stringResource(com.pawsup.R.string.production_cat_sounds), sound) { scope.launch { vm.prefs.setSoundEnabled(it) } }
                            Toggle(stringResource(com.pawsup.R.string.production_static_cat_reduced_motion), motion) { scope.launch { vm.prefs.setReduceMotion(it) } }
                            Text(stringResource(com.pawsup.R.string.production_quiet_hours_local_time_matching_hours_disables))
                            MinuteControl(stringResource(com.pawsup.R.string.production_from_hour), quietStart, 0..23) { scope.launch { vm.prefs.setQuietHours(it, quietEnd) } }
                            MinuteControl(stringResource(com.pawsup.R.string.production_until_hour), quietEnd, 0..23) { scope.launch { vm.prefs.setQuietHours(quietStart, it) } }
                            Text(stringResource(com.pawsup.R.string.production_recent_breaks), style = MaterialTheme.typography.titleLarge)
                            val entries = remember(history) { runCatching { org.json.JSONArray(history) }.getOrDefault(org.json.JSONArray()) }
                            if (entries.length() == 0) Text(stringResource(com.pawsup.R.string.production_no_breaks_recorded_yet))
                            for (i in entries.length() - 1 downTo maxOf(0, entries.length() - 10)) {
                                val entry = entries.getJSONObject(i)
                                Text("${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(entry.getLong("time")))}: ${entry.getString("outcome")}")
                            }
                        }
                        1 -> {
                            Text(stringResource(com.pawsup.R.string.production_choose_your_companion), style = MaterialTheme.typography.headlineMedium)
                            cats.all.forEach { cat ->
                                Card(Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        CatPosterImage(cat, Modifier.size(72.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(cat.displayName, style = MaterialTheme.typography.titleLarge)
                                            Text(cat.personalityBlurb)
                                            Button(onClick = {
                                                if (state.entitlements.owns(cat.id)) vm.setActiveCat(cat.id)
                                                else open(Intent(this@SettingsActivity, PaywallActivity::class.java).putExtra(PaywallActivity.EXTRA_CAT_ID, cat.id))
                                            }) { Text(if (state.activeCat?.id == cat.id) "Selected" else if (state.entitlements.owns(cat.id)) "Use this cat" else "View subscription") }
                                        }
                                    }
                                }
                            }
                            Text(billingMessage)
                            Text(stringResource(com.pawsup.R.string.production_paid_access_is_checked_with_google_play_when_online_cached_access))
                            OutlinedButton(onClick = vm::restorePurchases) { Text(stringResource(com.pawsup.R.string.production_restore_purchases_retry)) }
                            OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))) }) { Text(stringResource(com.pawsup.R.string.production_manage_subscriptions)) }
                        }
                        else -> {
                            Text(stringResource(com.pawsup.R.string.production_monitoring_status), style = MaterialTheme.typography.headlineMedium)
                            Text(health)
                            Text("Usage access: ${if (state.usagePermGranted) "Granted" else "Needed"}")
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text(stringResource(com.pawsup.R.string.production_usage_access_settings)) }
                            Text("Automatic break display: ${if (state.overlayPermGranted) "Allowed" else "Off; use notifications to open breaks"}")
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }) { Text(stringResource(com.pawsup.R.string.production_display_over_apps_settings)) }
                            Text("Notifications: ${if (state.notifPermGranted) "Allowed" else "Off"}. Notification reminders need notifications enabled.")
                            OutlinedButton(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= 26) open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                else open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                            }) { Text(stringResource(com.pawsup.R.string.production_notification_settings)) }
                            Text(stringResource(com.pawsup.R.string.production_some_phones_restrict_background_apps_pawsup_cannot_verify_manufac))
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }) { Text(stringResource(com.pawsup.R.string.production_app_settings)) }
                            OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.techniact.com/apps/pawsup/privacy-policy"))) }) { Text(stringResource(com.pawsup.R.string.production_privacy_policy)) }
                            Text(stringResource(com.pawsup.R.string.production_app_selections_and_break_history_stay_in_app_storage_google_play_))
                            OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(com.pawsup.R.string.production_delete_break_history)) }
                            OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(com.pawsup.R.string.production_reset_app_settings)) }
                        }
                    }
                }
            }
            if (confirmDelete || confirmReset) AlertDialog(onDismissRequest = { confirmDelete = false; confirmReset = false },
                title = { Text(if (confirmReset) "Reset PawsUp?" else "Delete break history?") },
                text = { Text(stringResource(com.pawsup.R.string.production_this_removes_local_data_google_play_subscriptions_are_not_cancell)) },
                confirmButton = { TextButton(onClick = {
                    val reset = confirmReset
                    confirmDelete = false; confirmReset = false
                    scope.launch {
                        if (reset) { stopService(Intent(this@SettingsActivity, com.pawsup.monitoring.MonitoringService::class.java)); vm.prefs.resetSettings(); finish() }
                        else vm.prefs.deleteHistory()
                    }
                }) { Text(stringResource(com.pawsup.R.string.production_confirm)) } }, dismissButton = { TextButton(onClick = { confirmDelete = false; confirmReset = false }) { Text(stringResource(com.pawsup.R.string.production_cancel)) } })
        } }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = null)
    }
}
@Composable
private fun MinuteControl(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text("$label: ${draft.toInt()}")
    Slider(modifier = Modifier.semantics { contentDescription = label }, value = draft, onValueChange = { draft = it }, onValueChangeFinished = { onChange(draft.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1)
}
