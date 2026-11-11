package com.pawsup.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawsup.R
import com.pawsup.data.UserPreferences
import com.pawsup.productivity.*
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar

@Composable
fun ProductivityPanel(prefs: UserPreferences) {
    val global by prefs.globalBudgetEnabled.collectAsStateWithLifecycle(initialValue = true)
    val selected by prefs.monitoredPackages.collectAsStateWithLifecycle(initialValue = emptySet())
    val limits by prefs.appLimits.collectAsStateWithLifecycle(initialValue = emptyMap())
    val schedule by prefs.focusSchedule.collectAsStateWithLifecycle(initialValue = FocusSchedule())
    val days by prefs.dailyTotals.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Text(stringResource(R.string.productivity_title), style = MaterialTheme.typography.titleLarge)
    Toggle(stringResource(R.string.global_budget), global) { scope.launch { prefs.setGlobalBudgetEnabled(it) } }
    Text(stringResource(R.string.budget_rules_help))
    selected.sorted().forEach { packageName ->
        val label = remember(packageName) { runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName) }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Toggle(stringResource(R.string.app_budget, label), packageName in limits) {
                    scope.launch { prefs.setAppLimit(packageName, if (it) 15 else null) }
                }
                limits[packageName]?.let { value -> MinuteControl(stringResource(R.string.app_budget_minutes, label), value, 1..60) {
                    scope.launch { prefs.setAppLimit(packageName, it) }
                } }
            }
        }
    }
    if (!global && selected.none { it in limits }) Text(stringResource(R.string.no_budget_rules))
    FocusScheduleEditor(schedule) { transform -> scope.launch { prefs.updateFocusSchedule(transform) } }
    val now by remember { kotlinx.coroutines.flow.flow {
        while (true) { emit(System.currentTimeMillis()); kotlinx.coroutines.delay(60_000) }
    } }.collectAsStateWithLifecycle(initialValue = System.currentTimeMillis())
    val calendar = Calendar.getInstance().apply { timeInMillis = now }
    val today = DailyBuckets.date(calendar.timeInMillis)
    calendar.add(Calendar.DAY_OF_MONTH, -6)
    val first = DailyBuckets.date(calendar.timeInMillis)
    SummaryCard(stringResource(R.string.summary_today), days.filter { it.date == today })
    SummaryCard(stringResource(R.string.summary_week), days.filter { it.date >= first && it.date <= today })
    Text(stringResource(R.string.summary_limitations))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScheduleEditor(schedule: FocusSchedule, update: ((FocusSchedule) -> FocusSchedule) -> Unit) {
    Toggle(stringResource(R.string.focus_schedule), schedule.enabled) { value -> update { it.copy(enabled = value) } }
    if (schedule.enabled) {
        Text(stringResource(R.string.schedule_help))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..7).forEach { day ->
                val calendarDay = if (day == 7) Calendar.SUNDAY else day + 1
                FilterChip(selected = day in schedule.days, onClick = {
                    update { old -> old.copy(days = if (day in old.days) old.days - day else old.days + day) }
                }, label = { Text(DateFormatSymbols.getInstance().weekdays[calendarDay]) })
            }
        }
        ScheduleTimeControl(stringResource(R.string.schedule_start_label), schedule.startMinute) { value -> update { it.copy(startMinute = value) } }
        ScheduleTimeControl(stringResource(R.string.schedule_end_label), schedule.endMinute) { value -> update { it.copy(endMinute = value) } }
        if (schedule.days.isEmpty()) Text(stringResource(R.string.schedule_no_days))
    }
}

private fun timeLabel(minute: Int): String = String.format(java.util.Locale.getDefault(), "%02d:%02d", minute / 60, minute % 60)

@Composable
fun SummaryCard(title: String, rows: List<DailyTotal>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (rows.isEmpty()) Text(stringResource(R.string.summary_empty))
            else {
                Text(stringResource(R.string.summary_usage, rows.sumOf { it.usageMs } / 60_000.0))
                Text(stringResource(R.string.summary_break, rows.sumOf { it.breakMs } / 60_000.0))
                Text(stringResource(R.string.summary_outcomes, rows.sumOf { it.completed }, rows.sumOf { it.skipped }, rows.sumOf { it.interrupted }))
            }
        }
    }
}

@Composable
fun ScheduleTimeControl(label: String, minute: Int, onChange: (Int) -> Unit) {
    var draft by remember(minute) { mutableFloatStateOf(minute / 15f) }
    Text(stringResource(R.string.schedule_time_value, label, timeLabel(draft.toInt() * 15)))
    Slider(value = draft, onValueChange = { draft = it }, onValueChangeFinished = { onChange(draft.toInt() * 15) },
        valueRange = 0f..95f, steps = 94, modifier = Modifier.semantics { contentDescription = label })
}
