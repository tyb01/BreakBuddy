package com.pawsup.settings

import androidx.core.net.toUri
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
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    private val vm: SettingsViewModel by viewModels()
    @Inject lateinit var journal: com.pawsup.diagnostics.DiagnosticsJournal
    private val exportDiagnostics = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) lifecycleScope.launch {
            val success = runCatching {
                val text = journal.export()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    requireNotNull(contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use { it.write(text) }
                }
            }.isSuccess
            android.widget.Toast.makeText(this@SettingsActivity, getString(if (success) com.pawsup.R.string.diagnostics_saved else com.pawsup.R.string.diagnostics_failed), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    @Inject lateinit var cats: CatRegistry
    override fun onResume() { super.onResume(); vm.refreshPermissions() }
    // Context.stopService resolves the explicit component; it is not a listener-removal API.
    @android.annotation.SuppressLint("ImplicitSamInstance")
    private fun stopMonitoringForReset() { stopService(Intent(this, com.pawsup.monitoring.MonitoringService::class.java)) }
    private fun open(intent: Intent) {
        runCatching { startActivity(intent) }.onFailure {
            android.widget.Toast.makeText(this, getString(com.pawsup.R.string.ui_this_settings_page_is_not_available_on_your_devi_91248a), android.widget.Toast.LENGTH_LONG).show()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PawsUpTheme {
            val state by vm.state.collectAsStateWithLifecycle()
            val health by vm.health.collectAsStateWithLifecycle(initialValue = stringResource(com.pawsup.R.string.ui_checking_monitoring_status_beadb9))
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
                    listOf(stringResource(com.pawsup.R.string.ui_home_70f8bb), stringResource(com.pawsup.R.string.ui_cafe_37efa8), stringResource(com.pawsup.R.string.ui_reliability_6780c7)).forEachIndexed { index, title ->
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
                            Text(if (state.monitoredCount == 0) stringResource(com.pawsup.R.string.ui_choose_apps_before_starting_monitoring_ec57d3) else health)
                            Toggle(stringResource(com.pawsup.R.string.production_monitor_selected_apps), state.monitorMeEnabled) { vm.setMonitorMe(it, this@SettingsActivity) }
                            Text(stringResource(com.pawsup.R.string.production_after_a_restart_or_system_stop_open_pawsup_to_resume_breaks_are_v))
                            OutlinedButton(onClick = { open(Intent(this@SettingsActivity, AppPickerActivity::class.java)) }) { Text(androidx.compose.ui.res.pluralStringResource(com.pawsup.R.plurals.choose_apps, state.monitoredCount, state.monitoredCount)) }
                            Toggle(stringResource(com.pawsup.R.string.production_combine_time_across_selected_apps), combined) { scope.launch { vm.prefs.setCombinedUsage(it) } }
                            Text(if (combined) stringResource(com.pawsup.R.string.ui_switching_between_selected_apps_keeps_the_same_b_0e6a5a) else stringResource(com.pawsup.R.string.ui_each_continuous_app_visit_starts_a_new_timer_27af19))
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
                            ProductivityPanel(vm.prefs)
                            Text(stringResource(com.pawsup.R.string.production_recent_breaks), style = MaterialTheme.typography.titleLarge)
                            val entries = remember(history) { runCatching { org.json.JSONArray(history) }.getOrDefault(org.json.JSONArray()) }
                            if (entries.length() == 0) Text(stringResource(com.pawsup.R.string.production_no_breaks_recorded_yet))
                            for (i in entries.length() - 1 downTo maxOf(0, entries.length() - 10)) {
                                val entry = entries.getJSONObject(i)
                                Text(stringResource(com.pawsup.R.string.history_entry,
                                    java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(entry.getLong("time"))),
                                    stringResource(when (entry.getString("outcome")) { "completed" -> com.pawsup.R.string.outcome_completed; "skipped" -> com.pawsup.R.string.outcome_skipped; else -> com.pawsup.R.string.outcome_interrupted })))
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
                                            Text(stringResource(cat.personalityBlurbRes))
                                            Button(onClick = {
                                                if (state.entitlements.owns(cat.id)) vm.setActiveCat(cat.id)
                                                else open(Intent(this@SettingsActivity, PaywallActivity::class.java).putExtra(PaywallActivity.EXTRA_CAT_ID, cat.id))
                                            }) { Text(if (state.activeCat?.id == cat.id) stringResource(com.pawsup.R.string.ui_selected_9a976f) else if (state.entitlements.owns(cat.id)) stringResource(com.pawsup.R.string.ui_use_this_cat_63630d) else stringResource(com.pawsup.R.string.ui_view_subscription_493c3e)) }
                                        }
                                    }
                                }
                            }
                            Text(billingMessage)
                            Text(stringResource(com.pawsup.R.string.production_paid_access_is_checked_with_google_play_when_online_cached_access))
                            OutlinedButton(onClick = vm::restorePurchases) { Text(stringResource(com.pawsup.R.string.production_restore_purchases_retry)) }
                            OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/account/subscriptions".toUri())) }) { Text(stringResource(com.pawsup.R.string.production_manage_subscriptions)) }
                        }
                        else -> {
                            Text(stringResource(com.pawsup.R.string.production_monitoring_status), style = MaterialTheme.typography.headlineMedium)
                            Text(health)
                            Text(stringResource(com.pawsup.R.string.diagnostics_help))
                            OutlinedButton(onClick = { exportDiagnostics.launch("pawsup-diagnostics.json") }) { Text(stringResource(com.pawsup.R.string.diagnostics_export)) }
                            OutlinedButton(onClick = { scope.launch { vm.prefs.clearDiagnostics() } }) { Text(stringResource(com.pawsup.R.string.diagnostics_delete)) }
                            Text(stringResource(if (state.usagePermGranted) com.pawsup.R.string.usage_granted else com.pawsup.R.string.usage_needed))
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) { Text(stringResource(com.pawsup.R.string.production_usage_access_settings)) }
                            Text(stringResource(if (state.overlayPermGranted) com.pawsup.R.string.overlay_allowed else com.pawsup.R.string.overlay_off))
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())) }) { Text(stringResource(com.pawsup.R.string.production_display_over_apps_settings)) }
                            Text(stringResource(if (state.notifPermGranted) com.pawsup.R.string.notifications_allowed else com.pawsup.R.string.notifications_off))
                            OutlinedButton(onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= 26) open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                                else open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
                            }) { Text(stringResource(com.pawsup.R.string.production_notification_settings)) }
                            Text(stringResource(com.pawsup.R.string.production_some_phones_restrict_background_apps_pawsup_cannot_verify_manufac))
                            OutlinedButton(onClick = { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())) }) { Text(stringResource(com.pawsup.R.string.production_app_settings)) }
                            OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, "https://www.techniact.com/apps/pawsup/privacy-policy".toUri())) }) { Text(stringResource(com.pawsup.R.string.production_privacy_policy)) }
                            Text(stringResource(com.pawsup.R.string.production_app_selections_and_break_history_stay_in_app_storage_google_play_))
                            OutlinedButton(onClick = { confirmDelete = true }) { Text(stringResource(com.pawsup.R.string.production_delete_break_history)) }
                            OutlinedButton(onClick = { confirmReset = true }) { Text(stringResource(com.pawsup.R.string.production_reset_app_settings)) }
                        }
                    }
                }
            }
            if (confirmDelete || confirmReset) AlertDialog(onDismissRequest = { confirmDelete = false; confirmReset = false },
                title = { Text(if (confirmReset) stringResource(com.pawsup.R.string.ui_reset_pawsup_c13071) else stringResource(com.pawsup.R.string.ui_delete_break_history_2a5001)) },
                text = { Text(stringResource(com.pawsup.R.string.production_this_removes_local_data_google_play_subscriptions_are_not_cancell)) },
                confirmButton = { TextButton(onClick = {
                    val reset = confirmReset
                    confirmDelete = false; confirmReset = false
                    scope.launch {
                        if (reset) { stopMonitoringForReset(); vm.prefs.resetSettings(); finish() }
                        else vm.prefs.deleteHistory()
                    }
                }) { Text(stringResource(com.pawsup.R.string.production_confirm)) } }, dismissButton = { TextButton(onClick = { confirmDelete = false; confirmReset = false }) { Text(stringResource(com.pawsup.R.string.production_cancel)) } })
        } }
    }
}

@Composable
fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = null)
    }
}
@Composable
fun MinuteControl(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text(stringResource(com.pawsup.R.string.control_value, label, draft.toInt()))
    Slider(modifier = Modifier.semantics { contentDescription = label }, value = draft, onValueChange = { draft = it }, onValueChangeFinished = { onChange(draft.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(), steps = range.last - range.first - 1)
}
