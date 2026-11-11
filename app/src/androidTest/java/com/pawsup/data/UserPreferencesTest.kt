package com.pawsup.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Runs in the debug app sandbox; requires a disposable test installation/device. */
@RunWith(AndroidJUnit4::class)
class UserPreferencesTest {
    private lateinit var prefs: UserPreferences
    @Before fun setUp() = runBlocking {
        prefs = UserPreferences(ApplicationProvider.getApplicationContext())
        prefs.resetSettings()
        Unit
    }
    @After fun tearDown() = runBlocking { prefs.resetSettings(); Unit }
    @Test fun concurrentPetIncrementsAreNotLost() = runBlocking {
        coroutineScope { repeat(100) { launch(Dispatchers.Default) { prefs.incrementPetsLifetime() } } }
        assertEquals(100L, prefs.totalPetsLifetime.first())
    }
    @Test fun concurrentPackageTogglesAreTransactional() = runBlocking {
        coroutineScope { repeat(100) { launch(Dispatchers.Default) { prefs.togglePackage("test.app") } } }
        assertTrue(prefs.monitoredPackages.first().isEmpty())
    }
    @Test fun repeatedCompletionRecordsExactlyOneOutcome() = runBlocking {
        prefs.setActiveBreak(JSONObject().put("id", "session").toString())
        coroutineScope { repeat(20) { launch(Dispatchers.Default) { prefs.finishBreak("session", "completed", 120) } } }
        assertEquals(1, prefs.totalBreaksCompleted.first())
        assertEquals(1, org.json.JSONArray(prefs.history.first()).length())
        assertNull(prefs.activeBreak())
    }
    @Test fun staleCompletionCannotClearAnotherSession() = runBlocking {
        prefs.setActiveBreak(JSONObject().put("id", "current").toString())
        assertFalse(prefs.finishBreak("stale", "completed", 120))
        assertEquals("current", JSONObject(prefs.activeBreak()!!).getString("id"))
        assertEquals(0, prefs.totalBreaksCompleted.first())
    }
    @Test fun deletingHistoryDoesNotAllowLateCallbackToRecreateIt() = runBlocking {
        prefs.setActiveBreak(JSONObject().put("id", "session").toString())
        prefs.finishBreak("session", "completed", 120)
        prefs.deleteHistory()
        assertFalse(prefs.finishBreak("session", "completed", 120))
        assertEquals("[]", prefs.history.first())
        assertEquals(0, prefs.totalBreaksCompleted.first())
    }
    @Test fun successfulEmptyEntitlementsSelectFreeCatAtomically() = runBlocking {
        prefs.syncEntitlements(setOf("miso", "mocha", "yuki"), false, 10)
        prefs.setActiveCatId("yuki")
        prefs.syncEntitlements(setOf("miso", "mocha"), false, 20)
        assertEquals("miso", prefs.activeCatId.first())
        assertEquals(20L, prefs.entitlementsLastSynced.first())
        assertFalse("yuki" in prefs.ownedCatIds.first())
    }

    @Test fun repeatedCheckpointDoesNotDoubleCountUsage() = runBlocking {
        val generation = prefs.historyGeneration.first()
        val interval = com.pawsup.productivity.ObservedInterval(1_000, 61_000, usageMs = 60_000)
        val checkpoint = JSONObject().put("boot", 1).put("cursor", 61_000).toString()
        prefs.saveObservation(checkpoint, listOf(interval), generation)
        prefs.saveObservation(checkpoint, listOf(interval), generation)
        assertEquals(60_000L, prefs.dailyTotals.first().sumOf { it.usageMs })
    }
    @Test fun deletionRejectsAlreadyQueuedUsage() = runBlocking {
        val previous = prefs.historyGeneration.first()
        prefs.deleteHistory()
        prefs.saveObservation(JSONObject().put("boot", 1).put("cursor", 61_000).toString(),
            listOf(com.pawsup.productivity.ObservedInterval(1_000, 61_000, usageMs = 60_000)), previous)
        assertTrue(prefs.dailyTotals.first().isEmpty())
    }
    @Test fun dailyBreakSummaryIsIdempotentAndDeletable() = runBlocking {
        prefs.setActiveBreak(JSONObject().put("id", "record").toString())
        prefs.finishBreak("record", "skipped", 40)
        prefs.finishBreak("record", "skipped", 40)
        assertEquals(40_000L, prefs.dailyTotals.first().sumOf { it.breakMs })
        assertEquals(1, prefs.dailyTotals.first().sumOf { it.skipped })
        prefs.deleteHistory(); assertTrue(prefs.dailyTotals.first().isEmpty())
    }
    @Test fun scheduleUpdatesDoNotLoseConcurrentWeekdayChanges() = runBlocking {
        prefs.setFocusSchedule(com.pawsup.productivity.FocusSchedule(true, emptySet()))
        coroutineScope { (1..7).forEach { day -> launch { prefs.updateFocusSchedule { it.copy(days = it.days + day) } } } }
        assertEquals((1..7).toSet(), prefs.focusSchedule.first().days)
    }
    @Test fun appOverrideCanBeChangedAndRemovedWithoutChangingOtherRules() = runBlocking {
        prefs.setAppLimit("a", 5); prefs.setAppLimit("b", 500); prefs.setAppLimit("a", null)
        assertEquals(mapOf("b" to 60), prefs.appLimits.first())
        assertTrue(prefs.globalBudgetEnabled.first())
    }
    @Test fun diagnosticsAreBoundedAndExportContainsOnlyApprovedFields() = runBlocking {
        repeat(105) { prefs.appendDiagnostic(com.pawsup.diagnostics.DiagnosticCode.MONITOR_STARTED) }
        val report = JSONObject(com.pawsup.diagnostics.DiagnosticsJournal(prefs).export())
        val events = report.getJSONArray("events")
        assertEquals(100, events.length())
        assertEquals(setOf("time", "code"), events.getJSONObject(0).keys().asSequence().toSet())
        prefs.clearDiagnostics(); assertEquals("[]", prefs.diagnostics.first())
    }

    @Test fun deletingHistoryRejectsAnOlderBreakFinishingLater() = runBlocking {
        prefs.setActiveBreak(JSONObject().put("id", "old").toString())
        prefs.deleteHistory()
        assertFalse(prefs.finishBreak("old", "completed", 120))
        assertEquals("[]", prefs.history.first())
        assertTrue(prefs.dailyTotals.first().isEmpty())
        assertNull(prefs.activeBreak())
    }
}
