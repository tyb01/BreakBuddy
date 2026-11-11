package com.pawsup.break_experience

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pawsup.data.UserPreferences
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class BreakExitTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun earlyExitRecordsSkippedInsteadOfCompleted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = UserPreferences(context)
        runBlocking { prefs.resetSettings(); prefs.setReduceMotion(true) }
        ActivityScenario.launch(BreakOverlayActivity::class.java).use {
            compose.onNodeWithText(context.getString(com.pawsup.R.string.production_end_break_early)).performClick()
            compose.waitUntil(10_000) { runBlocking { JSONArray(prefs.history.first()).length() == 1 } }
            runBlocking {
                assertEquals("skipped", JSONArray(prefs.history.first()).getJSONObject(0).getString("outcome"))
                assertEquals(0, prefs.totalBreaksCompleted.first())
                prefs.resetSettings()
            }
        }
    }
}
