package com.pawsup.diagnostics

import com.pawsup.data.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Closed vocabulary: no exception text, package names, tokens or user input is accepted. */
enum class DiagnosticCode { MONITOR_STARTED, MONITOR_STOPPED, MONITOR_FAILED, USAGE_REVOKED, CLOCK_CHANGED,
    PRESENTATION_REQUESTED, PRESENTATION_UNCONFIRMED, BILLING_UNAVAILABLE, BILLING_CHECKED, BILLING_PENDING, ACK_RETRY_EXHAUSTED }

@Singleton
class DiagnosticsJournal @Inject constructor(private val prefs: UserPreferences) {
    private val queue = Channel<DiagnosticCode>(64, kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init { scope.launch { for (event in queue) runCatching { prefs.appendDiagnostic(event) } } }
    fun record(code: DiagnosticCode) { queue.trySend(code) }
    suspend fun export(): String {
        // Rebuild with allowlisted fields: even corrupted/imported data cannot leak arbitrary text.
        val source = runCatching { org.json.JSONArray(prefs.diagnostics.first()) }.getOrDefault(org.json.JSONArray())
        val safe = org.json.JSONArray()
        for (i in 0 until source.length()) {
            val row = source.optJSONObject(i) ?: continue
            val code = runCatching { DiagnosticCode.valueOf(row.optString("code")) }.getOrNull() ?: continue
            safe.put(org.json.JSONObject().put("time", row.optLong("time")).put("code", code.name))
        }
        return org.json.JSONObject().put("schema", 1).put("events", safe).toString(2)
    }
}
