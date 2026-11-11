package com.pawsup.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.pawsup.cats.Cat
import com.pawsup.cats.CatAssetResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Application-scoped in-memory cache of chroma-keyed cat posters.
 * Populated at app startup by PawsUpApplication; composables read from here
 * synchronously, so the image is available the moment the composable first
 * enters composition — no LaunchedEffect delay.
 */
object CatPosterCache {
    private val store = ConcurrentHashMap<String, ImageBitmap>()

    fun put(catId: String, bmp: ImageBitmap) { store[catId] = bmp }
    fun get(catId: String): ImageBitmap? = store[catId]
}

/**
 * Displays a cat's poster with transparent background.
 * If the poster is already in [CatPosterCache] (preloaded at startup) it is
 * shown immediately with zero delay. Only falls back to background decoding
 * on a cache miss (e.g. very first launch before preload completes).
 */
@Composable
fun CatPosterImage(
    cat: Cat,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current

    // Snapshot cache at composition time — instant on a hit
    var bitmap by remember(cat.id) { mutableStateOf(CatPosterCache.get(cat.id)) }

    // Only runs if preload hasn't finished yet for this cat
    LaunchedEffect(cat.id, cat.keyColor) {
        if (bitmap != null) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val raw = runCatching {
                context.assets.open(CatAssetResolver.poster(cat.id)).use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull() ?: return@withContext
            val processed = chromaKeyBitmap(raw, cat.keyColor).asImageBitmap()
            raw.recycle()
            CatPosterCache.put(cat.id, processed)
            bitmap = processed
        }
    }

    // Box always occupies the full requested space (weight, size, etc.) so the
    // surrounding layout never shifts when the bitmap fades in.
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = bitmap != null,
            enter = fadeIn(animationSpec = tween(250)),
            modifier = Modifier.fillMaxSize()
        ) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = cat.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}

fun chromaKeyBitmap(
    source: android.graphics.Bitmap,
    keyColorArgb: Int,
    threshold: Float = 0.18f,
    softness: Float  = 0.08f
): android.graphics.Bitmap {
    val w = source.width
    val h = source.height
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)

    val kr = android.graphics.Color.red(keyColorArgb)   / 255f
    val kg = android.graphics.Color.green(keyColorArgb) / 255f
    val kb = android.graphics.Color.blue(keyColorArgb)  / 255f

    for (i in pixels.indices) {
        val p = pixels[i]
        val r = android.graphics.Color.red(p)   / 255f
        val g = android.graphics.Color.green(p) / 255f
        val b = android.graphics.Color.blue(p)  / 255f
        val dist  = sqrt((r - kr) * (r - kr) + (g - kg) * (g - kg) + (b - kb) * (b - kb))
        val alpha = smoothstep(threshold, threshold + softness, dist)
        pixels[i] = android.graphics.Color.argb(
            (alpha * 255f).toInt().coerceIn(0, 255),
            android.graphics.Color.red(p),
            android.graphics.Color.green(p),
            android.graphics.Color.blue(p)
        )
    }

    val result = androidx.core.graphics.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, w, 0, 0, w, h)
    return result
}

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
