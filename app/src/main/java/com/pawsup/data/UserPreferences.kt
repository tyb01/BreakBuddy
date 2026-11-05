package com.pawsup.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pawsup_prefs",
    corruptionHandler = androidx.datastore.core.handlers.ReplaceFileCorruptionHandler {
        androidx.datastore.preferences.core.preferencesOf(
            androidx.datastore.preferences.core.booleanPreferencesKey("recovered_corruption") to true)
    }
)

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore
    private val combinedKey = androidx.datastore.preferences.core.booleanPreferencesKey("combined_usage")
    private val soundKey = androidx.datastore.preferences.core.booleanPreferencesKey("sound_enabled")
    private val motionKey = androidx.datastore.preferences.core.booleanPreferencesKey("reduce_motion")
    private val snoozeKey = androidx.datastore.preferences.core.longPreferencesKey("snooze_until")
    private val snoozeElapsedKey = androidx.datastore.preferences.core.longPreferencesKey("snooze_elapsed")
    private val snoozeBootKey = androidx.datastore.preferences.core.longPreferencesKey("snooze_boot")
    private val quietStartKey = androidx.datastore.preferences.core.intPreferencesKey("quiet_start")
    private val quietEndKey = androidx.datastore.preferences.core.intPreferencesKey("quiet_end")
    private val checkpointKey = androidx.datastore.preferences.core.stringPreferencesKey("usage_checkpoint")
    private val breakKey = androidx.datastore.preferences.core.stringPreferencesKey("active_break")
    private val historyKey = androidx.datastore.preferences.core.stringPreferencesKey("break_history")
    private val completedIdsKey = androidx.datastore.preferences.core.stringSetPreferencesKey("finished_break_ids")
    val recoveredCorruption = store.data.map { it[androidx.datastore.preferences.core.booleanPreferencesKey("recovered_corruption")] ?: false }
    val combinedUsage = store.data.map { it[combinedKey] ?: !(it[DataStoreKeys.ONBOARDING_COMPLETED] ?: false) }
    val soundEnabled = store.data.map { it[soundKey] ?: false }
    val reduceMotion = store.data.map { it[motionKey] ?: false }
    val snoozeUntil = store.data.map { it[snoozeKey] ?: 0L }
    val quietStart = store.data.map { it[quietStartKey] ?: 0 }
    val quietEnd = store.data.map { it[quietEndKey] ?: 0 }
    val history = store.data.map { it[historyKey] ?: "[]" }
    suspend fun setCombinedUsage(value: Boolean) = store.edit { it[combinedKey] = value }
    suspend fun setSoundEnabled(value: Boolean) = store.edit { it[soundKey] = value }
    suspend fun setReduceMotion(value: Boolean) = store.edit { it[motionKey] = value }
    suspend fun snooze(minutes: Int) = store.edit {
        val duration = minutes.coerceIn(0, 60) * 60_000L
        it[snoozeKey] = if (duration == 0L) 0 else System.currentTimeMillis() + duration
        it[snoozeElapsedKey] = if (duration == 0L) 0 else android.os.SystemClock.elapsedRealtime() + duration
        it[snoozeBootKey] = com.pawsup.monitoring.BootIdentity.read(context)
    }
    suspend fun isSnoozed(elapsed: Long, boot: Long): Boolean {
        val data = store.data.first()
        return boot >= 0 && data[snoozeBootKey] == boot && (data[snoozeElapsedKey] ?: 0) > elapsed
    }
    suspend fun setQuietHours(start: Int, end: Int) = store.edit {
        it[quietStartKey] = start.coerceIn(0, 23); it[quietEndKey] = end.coerceIn(0, 23)
    }
    suspend fun checkpoint() = store.data.first()[checkpointKey]
    suspend fun checkpoint(value: String) = store.edit { it[checkpointKey] = value }
    suspend fun isBreakFinished(id: String) = id in (store.data.first()[completedIdsKey] ?: emptySet())
    suspend fun activeBreak() = store.data.first()[breakKey]
    suspend fun setActiveBreak(value: String) = store.edit { it[breakKey] = value }
    suspend fun clearActiveBreak() = store.edit { it.remove(breakKey) }
    suspend fun finishBreak(id: String, outcome: String, seconds: Int): Boolean {
        var added = false
        store.edit { data ->
            val ids = data[completedIdsKey] ?: emptySet()
            val activeId = runCatching { org.json.JSONObject(data[breakKey] ?: "{}").optString("id") }.getOrNull()
            if (id !in ids && activeId == id) {
                added = true
                data[completedIdsKey] = (ids.toList().takeLast(99) + id).toSet()
                if (outcome == "completed") data[DataStoreKeys.TOTAL_BREAKS_COMPLETED] =
                    (data[DataStoreKeys.TOTAL_BREAKS_COMPLETED] ?: 0) + 1
                val old = runCatching { org.json.JSONArray(data[historyKey] ?: "[]") }.getOrDefault(org.json.JSONArray())
                val entries = org.json.JSONArray()
                for (i in maxOf(0, old.length() - 199) until old.length()) entries.put(old.get(i))
                entries.put(org.json.JSONObject().put("time", System.currentTimeMillis()).put("outcome", outcome).put("seconds", seconds))
                data[historyKey] = entries.toString()
                data.remove(breakKey)
            }
        }
        return added
    }
    suspend fun deleteHistory() = store.edit {
        it.remove(historyKey) // Keep bounded session IDs to prevent late callbacks recreating deleted history.
        it.remove(DataStoreKeys.TOTAL_BREAKS_COMPLETED); it.remove(DataStoreKeys.TOTAL_PETS_LIFETIME)
    }
    suspend fun resetSettings() = store.edit { it.clear(); it[DataStoreKeys.MONITOR_ME_ENABLED] = false }
    suspend fun togglePackage(packageName: String) = store.edit {
        val selected = (it[DataStoreKeys.MONITORED_PACKAGES] ?: emptySet()).toMutableSet()
        if (!selected.add(packageName)) selected.remove(packageName)
        it[DataStoreKeys.MONITORED_PACKAGES] = selected
    }
    suspend fun syncEntitlements(owned: Set<String>, bundle: Boolean, time: Long) = store.edit {
        it[DataStoreKeys.OWNED_CAT_IDS] = owned
        it[DataStoreKeys.HAS_CAFE_BUNDLE] = bundle
        it[DataStoreKeys.ENTITLEMENTS_LAST_SYNCED] = time
        val active = it[DataStoreKeys.ACTIVE_CAT_ID] ?: "miso"
        if (!bundle && active !in owned) it[DataStoreKeys.ACTIVE_CAT_ID] = "miso"
    }


    val monitoredPackages: Flow<Set<String>> = store.data.map { it[DataStoreKeys.MONITORED_PACKAGES] ?: emptySet() }
    val maxMinutesPerVisit: Flow<Int>        = store.data.map { it[DataStoreKeys.MAX_MINUTES_PER_VISIT] ?: 15 }
    val breakDurationMinutes: Flow<Int>      = store.data.map { it[DataStoreKeys.BREAK_DURATION_MINUTES] ?: 2 }
    val activeCatId: Flow<String>            = store.data.map { it[DataStoreKeys.ACTIVE_CAT_ID] ?: "miso" }
    val ownedCatIds: Flow<Set<String>>       = store.data.map { it[DataStoreKeys.OWNED_CAT_IDS] ?: setOf("miso", "mocha") }
    val hasCafeBundle: Flow<Boolean>         = store.data.map { it[DataStoreKeys.HAS_CAFE_BUNDLE] ?: false }
    val entitlementsLastSynced: Flow<Long>   = store.data.map { it[DataStoreKeys.ENTITLEMENTS_LAST_SYNCED] ?: 0L }
    val totalBreaksCompleted: Flow<Int>      = store.data.map { it[DataStoreKeys.TOTAL_BREAKS_COMPLETED] ?: 0 }
    val totalPetsLifetime: Flow<Long>        = store.data.map { it[DataStoreKeys.TOTAL_PETS_LIFETIME] ?: 0L }
    val onboardingCompleted: Flow<Boolean>   = store.data.map { it[DataStoreKeys.ONBOARDING_COMPLETED] ?: false }
    val useGlChromaKey: Flow<Boolean>        = store.data.map { it[DataStoreKeys.USE_GL_CHROMA_KEY] ?: false }
    val monitorMeEnabled: Flow<Boolean>      = store.data.map { it[DataStoreKeys.MONITOR_ME_ENABLED] ?: false }

    suspend fun setMonitoredPackages(packages: Set<String>)  = store.edit { it[DataStoreKeys.MONITORED_PACKAGES] = packages }
    suspend fun setMaxMinutesPerVisit(minutes: Int)          = store.edit { it[DataStoreKeys.MAX_MINUTES_PER_VISIT] = minutes.coerceIn(1, 60) }
    suspend fun setBreakDurationMinutes(minutes: Int)        = store.edit { it[DataStoreKeys.BREAK_DURATION_MINUTES] = minutes.coerceIn(1, 30) }
    suspend fun setActiveCatId(catId: String)                = store.edit { it[DataStoreKeys.ACTIVE_CAT_ID] = catId }
    suspend fun setOwnedCatIds(catIds: Set<String>)          = store.edit { it[DataStoreKeys.OWNED_CAT_IDS] = catIds }
    suspend fun setHasCafeBundle(has: Boolean)               = store.edit { it[DataStoreKeys.HAS_CAFE_BUNDLE] = has }
    suspend fun setEntitlementsLastSynced(ts: Long)          = store.edit { it[DataStoreKeys.ENTITLEMENTS_LAST_SYNCED] = ts }
    suspend fun setOnboardingCompleted(done: Boolean)        = store.edit { it[DataStoreKeys.ONBOARDING_COMPLETED] = done }
    suspend fun setUseGlChromaKey(use: Boolean)              = store.edit { it[DataStoreKeys.USE_GL_CHROMA_KEY] = use }
    suspend fun setMonitorMeEnabled(enabled: Boolean)        = store.edit { it[DataStoreKeys.MONITOR_ME_ENABLED] = enabled }
    suspend fun snapshotMonitorMeEnabled(): Boolean          = store.data.first()[DataStoreKeys.MONITOR_ME_ENABLED] ?: false

    suspend fun incrementBreaksCompleted() {
        store.edit { it[DataStoreKeys.TOTAL_BREAKS_COMPLETED] = (it[DataStoreKeys.TOTAL_BREAKS_COMPLETED] ?: 0) + 1 }
    }

    suspend fun incrementPetsLifetime() {
        store.edit { it[DataStoreKeys.TOTAL_PETS_LIFETIME] = (it[DataStoreKeys.TOTAL_PETS_LIFETIME] ?: 0L) + 1L }
    }

    suspend fun setGuestVisitLastShown(catId: String, ts: Long) =
        store.edit { it[DataStoreKeys.guestVisitLastShown(catId)] = ts }

    suspend fun getGuestVisitLastShown(catId: String): Long =
        store.data.first()[DataStoreKeys.guestVisitLastShown(catId)] ?: 0L

    // Synchronous snapshots for use inside services/workers
    suspend fun snapshotMonitoredPackages(): Set<String>  = store.data.first()[DataStoreKeys.MONITORED_PACKAGES] ?: emptySet()
    suspend fun snapshotMaxMinutes(): Int                  = store.data.first()[DataStoreKeys.MAX_MINUTES_PER_VISIT] ?: 15
    suspend fun snapshotBreakMinutes(): Int                = store.data.first()[DataStoreKeys.BREAK_DURATION_MINUTES] ?: 2
    suspend fun snapshotActiveCatId(): String              = store.data.first()[DataStoreKeys.ACTIVE_CAT_ID] ?: "miso"
    suspend fun snapshotTotalBreaks(): Int                 = store.data.first()[DataStoreKeys.TOTAL_BREAKS_COMPLETED] ?: 0
    suspend fun snapshotOwnedCatIds(): Set<String>         = store.data.first()[DataStoreKeys.OWNED_CAT_IDS] ?: setOf("miso", "mocha")
    suspend fun snapshotHasCafeBundle(): Boolean           = store.data.first()[DataStoreKeys.HAS_CAFE_BUNDLE] ?: false
}
