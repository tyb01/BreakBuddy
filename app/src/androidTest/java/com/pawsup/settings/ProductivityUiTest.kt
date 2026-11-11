package com.pawsup.settings

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.pawsup.productivity.*
import com.pawsup.ui.theme.PawsUpTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class ProductivityUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun scheduleToggleExposesAccessibleTimeControls() {
        compose.setContent { PawsUpTheme {
            var state by remember { mutableStateOf(FocusSchedule()) }
            androidx.compose.foundation.layout.Column { FocusScheduleEditor(state) { state = it(state) } }
        } }
        compose.onNodeWithText("Use a work / study schedule").performClick()
        compose.onNodeWithContentDescription("Start time").assertExists()
        compose.onNodeWithContentDescription("End time").assertExists()
    }
    @Test fun summaryUsesMeasuredTimeAndExplicitOutcomes() {
        compose.setContent { PawsUpTheme { SummaryCard("Today", listOf(DailyTotal("2026-11-05",120000,60000,1,2,3))) } }
        compose.onNodeWithText("Selected-app usage: 2.0 min").assertExists()
        compose.onNodeWithText("Recorded break time: 1.0 min").assertExists()
        compose.onNodeWithText("Completed: 1 ? Ended early: 2 ? Interrupted: 3").assertExists()
    }
    @Test fun emptySummaryDoesNotClaimZeroMeasuredUsage() {
        compose.setContent { PawsUpTheme { SummaryCard("Today",emptyList()) } }
        compose.onNodeWithText("No measurements recorded in this period.").assertExists()
    }
    @Test fun labeledSwitchHasOneToggleAction() {
        var count=0
        compose.setContent { PawsUpTheme { Toggle("Monitor",false){count++} } }
        compose.onNodeWithText("Monitor").performClick()
        compose.runOnIdle { assertEquals(1,count) }
    }

    @Test fun pseudolocaleAndLargeTextStillExposeControls() {
        val base = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = android.content.res.Configuration(base.resources.configuration).apply {
            setLocale(java.util.Locale("en", "XA")); fontScale = 2f
        }
        val localized = base.createConfigurationContext(config)
        assertNotEquals(base.getString(com.pawsup.R.string.focus_schedule), localized.getString(com.pawsup.R.string.focus_schedule))
        compose.setContent { CompositionLocalProvider(
            androidx.compose.ui.platform.LocalContext provides localized,
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.resources.displayMetrics.density, 2f)
        ) { PawsUpTheme { androidx.compose.foundation.layout.Column {
            FocusScheduleEditor(FocusSchedule(true)) { }
        } } } }
        compose.onNodeWithContentDescription(localized.getString(com.pawsup.R.string.schedule_start_label)).assertExists()
    }
}
