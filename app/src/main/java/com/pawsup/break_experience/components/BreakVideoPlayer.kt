package com.pawsup.break_experience.components

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.pawsup.break_experience.BreakState
import com.pawsup.cats.CatAssetResolver
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Single ExoPlayer manages the full break sequence as a 3-item playlist:
 *   [0] entrance.mp4  → plays once, then automatically transitions to idle
 *   [1] idle.mp4      → loops indefinitely via REPEAT_MODE_ONE
 *   [2] outro.mp4     → played on demand, no repeat
 *
 * All three items are prepared upfront so the idle starts instantly when
 * entrance ends, and the outro is already buffered when needed.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun BreakVideoPlayer(
    catId: String,
    breakState: BreakState,
    onEntranceComplete: () -> Unit,
    onOutroComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var failed by remember(catId) { mutableStateOf(false) }
    var ready by remember(catId) { mutableStateOf(false) }
    LaunchedEffect(catId) { kotlinx.coroutines.delay(8_000); if (!ready) failed = true }


    val player = remember(catId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(listOf(
                MediaItem.fromUri(Uri.parse("asset:///${CatAssetResolver.entranceVideo(catId)}")),
                MediaItem.fromUri(Uri.parse("asset:///${CatAssetResolver.idleVideo(catId)}")),
                MediaItem.fromUri(Uri.parse("asset:///${CatAssetResolver.outroVideo(catId)}"))
            ))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    val entranceCompleteReportedOnce = remember(catId) { AtomicBoolean(false) }

    // Item transition: entrance (0) → idle (1) is seamless (ExoPlayer gapless).
    // When idle starts, enable loop. When outro (2) ends, notify done.
    // Note: idle uses REPEAT_MODE_ONE — ExoPlayer can fire additional transitions onto
    // index 1 as the clip loops; we must only react to the first idle start or the countdown resets.
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) { failed = true }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (player.currentMediaItemIndex != 1) return
                player.repeatMode = Player.REPEAT_MODE_ONE
                if (!entranceCompleteReportedOnce.compareAndSet(false, true)) return
                onEntranceComplete()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) ready = true
                if (playbackState == Player.STATE_ENDED &&
                    player.currentMediaItemIndex == 2) {
                    onOutroComplete()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(player, lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) player.pause()
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) player.play()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Navigate to outro when the state machine asks for it.
    // REPEAT_MODE_ONE on idle means the player never reaches index 2 on its own.
    LaunchedEffect(failed) { if (failed) player.stop() }
    LaunchedEffect(breakState) {
        if (breakState == BreakState.CompanionLoop && player.currentMediaItemIndex == 0) {
            player.seekToDefaultPosition(1); player.repeatMode = Player.REPEAT_MODE_ONE
        }
        if ((breakState == BreakState.Outro || breakState == BreakState.Done)
            && player.currentMediaItemIndex != 2) {
            player.repeatMode = Player.REPEAT_MODE_OFF
            player.seekToDefaultPosition(2)
        }
    }

    if (failed) {
        com.pawsup.ui.CatPosterImage(com.pawsup.cats.CatRegistry().find(catId), modifier.fillMaxSize())
    } else AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                setKeepContentOnPlayerReset(true)
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
