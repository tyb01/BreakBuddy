package com.pawsup.break_experience

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pawsup.cats.*
import com.pawsup.data.UserPreferences
import com.pawsup.monitoring.MonitoringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class BreakUiState(
    val cat: Cat? = null, val breakState: BreakState = BreakState.Entrance,
    val remainingSeconds: Int = 0, val currentDialogueLine: String = "",
    val sessionPetCount: Int = 0, val showPetHint: Boolean = false,
    val totalBreaks: Int = 0, val reduceMotion: Boolean = false, val sound: Boolean = false
)
@HiltViewModel
class BreakOverlayViewModel @Inject constructor(
    private val prefs: UserPreferences, private val cats: CatRegistry,
    private val saved: SavedStateHandle, private val monitoring: MonitoringRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    private val _state = MutableStateFlow(BreakUiState())
    val state = _state.asStateFlow()
    private val _finish = MutableSharedFlow<BreakSummary>(replay = 1)
    val finishEvent = _finish.asSharedFlow()
    private val started = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)
    private var id: String = saved.get<String>("session_id") ?: UUID.randomUUID().toString().also { saved["session_id"] = it }
    private var minutes = (saved.get<Int>(BreakOverlayActivity.EXTRA_BREAK_MINUTES) ?: 2).coerceIn(1, 30)
    private var deadline = 0L
    private var timer: Job? = null
    private var initialization: Job? = null
    private var startedAt = 0L

    fun onEntranceStarted(cat: Cat) {
        if (!started.compareAndSet(false, true)) return
        initialization = persistenceScope.launch {
            if (prefs.isBreakFinished(id)) {
                finished.set(true)
                _finish.emit(BreakSummary(cat.displayName, minutes, 0, "interrupted"))
                return@launch
            }
            val now = SystemClock.elapsedRealtime()
            val boot = com.pawsup.monitoring.BootIdentity.read(context)
            val old = prefs.activeBreak()?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (old != null && BreakTiming.canRestore(old.getLong("boot"), boot, old.getLong("deadline"), now)) {
                id = old.getString("id"); deadline = old.getLong("deadline")
                minutes = old.optInt("minutes", minutes).coerceIn(1, 30)
                startedAt = old.optLong("started", deadline - minutes * 60_000L)
            } else {
                old?.let {
                    prefs.finishBreak(it.getString("id"), "interrupted", 0)
                    if (it.getString("id") == id) {
                        finished.set(true)
                        _finish.emit(BreakSummary(cat.displayName, minutes, 0, "interrupted"))
                        return@launch
                    }
                }
                startedAt = now
                deadline = now + minutes * 60_000L
            }
            val restoredCat = cats.find(old?.takeIf { it.optString("id") == id }?.optString("cat")?.takeIf { it.isNotBlank() } ?: cat.id)
            saved["session_id"] = id
            prefs.setActiveBreak(JSONObject().put("id", id).put("deadline", deadline).put("boot", boot).put("started", startedAt).put("minutes", minutes).put("cat", restoredCat.id).toString())
            if (!finished.get()) monitoring.visibleBreakId = id
            _state.value = BreakUiState(cat = restoredCat, remainingSeconds = BreakTiming.remainingSeconds(deadline, now),
                currentDialogueLine = CatVoiceProvider.getEntrance(cat.voiceBucket), showPetHint = true,
                reduceMotion = prefs.reduceMotion.first(), sound = prefs.soundEnabled.first())
            if (finished.get()) return@launch
            timer = viewModelScope.launch {
                val lines = CatVoiceProvider.getLoopLines(cat.voiceBucket)
                var ticks = 0
                while (isActive && !finished.get()) {
                    val remaining = BreakTiming.remainingSeconds(deadline, SystemClock.elapsedRealtime())
                    _state.update { it.copy(remainingSeconds = remaining) }
                    if (remaining == 0) { complete("completed"); break }
                    if (ticks++ % 100 == 99 && lines.isNotEmpty()) _state.update { it.copy(currentDialogueLine = lines[(ticks / 100) % lines.size]) }
                    delay(250)
                }
            }
        }
    }
    fun onEntranceComplete() { _state.update { if (it.breakState == BreakState.Entrance) it.copy(breakState = BreakState.CompanionLoop) else it } }
    fun onOutroComplete() = Unit // Media completion never controls the session outcome.
    fun dismissGuestVisit() = Unit
    fun skip() = complete("skipped")
    fun interrupted() = complete("interrupted")
    private fun complete(outcome: String) {
        if (!started.get() || !finished.compareAndSet(false, true)) return
        timer?.cancel()
        // The transaction must survive Activity/ViewModel destruction (e.g. Back/Home).
        persistenceScope.launch {
            initialization?.join()
            prefs.finishBreak(id, outcome, ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt().coerceIn(0, minutes * 60))
            monitoring.visibleBreakId = null
            _state.update { it.copy(breakState = BreakState.Done) }
            _finish.emit(BreakSummary(state.value.cat?.displayName ?: "Miso", minutes, state.value.sessionPetCount, outcome))
        }
    }
    fun onPetTapped() {
        if (finished.get()) return
        _state.update { it.copy(sessionPetCount = it.sessionPetCount + 1) }
        viewModelScope.launch { prefs.incrementPetsLifetime() }
    }
    companion object { private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}
data class BreakSummary(val catName: String, val durationMinutes: Int, val petCount: Int, val outcome: String = "completed")
