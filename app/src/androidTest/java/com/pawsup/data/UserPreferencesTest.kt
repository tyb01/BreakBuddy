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
}
