package com.pawsup.break_experience

import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pawsup.cats.CatRegistry
import com.pawsup.break_experience.components.*
import com.pawsup.ui.CatPosterImage
import com.pawsup.ui.theme.PawsUpTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BreakOverlayActivity : ComponentActivity() {
    @Inject lateinit var cats: CatRegistry
    private val vm: BreakOverlayViewModel by viewModels()
    private var purr: android.media.MediaPlayer? = null
    private var focusRequest: android.media.AudioFocusRequest? = null
    private val audio by lazy { getSystemService(android.media.AudioManager::class.java) }
    private val focusListener = android.media.AudioManager.OnAudioFocusChangeListener { change ->
        if (change <= 0) purr?.pause()
    }
    private fun abandonAudio() {
        if (android.os.Build.VERSION.SDK_INT >= 26) focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        else { @Suppress("DEPRECATION") audio.abandonAudioFocus(focusListener) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cat = cats.find(intent.getStringExtra(EXTRA_CAT_ID) ?: "miso")
        vm.onEntranceStarted(cat)
        setContent { PawsUpTheme {
            val state by vm.state.collectAsStateWithLifecycle()
            val displayedCat = state.cat ?: cat
            LaunchedEffect(Unit) {
                vm.finishEvent.collect { summary ->
                    if (summary.outcome == "completed") android.widget.Toast.makeText(this@BreakOverlayActivity, "Break completed with ${summary.catName}.", android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            BackHandler { vm.skip() }
            Surface(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    if (state.cat == null) CircularProgressIndicator()
                    else if (state.reduceMotion) CatPosterImage(displayedCat, Modifier.fillMaxSize())
                    else BreakVideoPlayer(displayedCat.id, state.breakState, vm::onEntranceComplete, vm::onOutroComplete)
                    Column(Modifier.align(Alignment.TopCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.currentDialogueLine, style = MaterialTheme.typography.titleLarge)
                        BreakTimerDisplay(state.remainingSeconds)
                        Text(stringResource(com.pawsup.R.string.production_you_can_turn_off_your_screen_and_rest_leaving_for_another_app_end))
                    }
                    Column(Modifier.align(Alignment.BottomCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { vm.onPetTapped(); if (state.sound) playPurr() }) { Text("Pet ${displayedCat.displayName}") }
                        TextButton(onClick = vm::skip) { Text(stringResource(com.pawsup.R.string.production_end_break_early)) }
                    }
                }
            }
        } }
    }
    private fun playPurr() {
        val granted = if (android.os.Build.VERSION.SDK_INT >= 26) {
            val request = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                .setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = request
            audio.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(focusListener, android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
        if (granted != android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        if (purr == null) runCatching {
            purr = android.media.MediaPlayer().apply {
                setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
                assets.openFd("sounds/purr.mp3").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                setOnCompletionListener { abandonAudio() }
                prepare()
            }
        }
        runCatching { purr?.let { if (!it.isPlaying) it.start() } }
    }
    override fun onStop() {
        purr?.pause(); abandonAudio()
        if (!isChangingConfigurations && !isFinishing && getSystemService(PowerManager::class.java).isInteractive) vm.interrupted()
        super.onStop()
    }
    override fun onDestroy() { abandonAudio(); purr?.release(); super.onDestroy() }
    companion object {
        const val EXTRA_CAT_ID = "cat_id"
        const val EXTRA_BREAK_MINUTES = "break_duration_minutes"
    }
}
