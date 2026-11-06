package com.pawsup.cats

import android.content.Context
import com.pawsup.R

object CatVoiceProvider {
    fun getEntrance(context: Context, bucket: VoiceBucket): String = context.getString(when (bucket) {
        VoiceBucket.MISO -> R.string.voice_miso_entrance
        VoiceBucket.YUKI -> R.string.voice_yuki_entrance
        VoiceBucket.MOCHA -> R.string.voice_mocha_entrance
        VoiceBucket.SHADOW -> R.string.voice_shadow_entrance
        VoiceBucket.BISCUIT -> R.string.voice_biscuit_entrance
    })
    fun getLoopLines(context: Context, bucket: VoiceBucket): List<String> = context.resources.getStringArray(when (bucket) {
        VoiceBucket.MISO -> R.array.voice_miso_loop
        VoiceBucket.YUKI -> R.array.voice_yuki_loop
        VoiceBucket.MOCHA -> R.array.voice_mocha_loop
        VoiceBucket.SHADOW -> R.array.voice_shadow_loop
        VoiceBucket.BISCUIT -> R.array.voice_biscuit_loop
    }).toList()
}
