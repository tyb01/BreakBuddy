package com.pawsup.cats

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatRegistry @Inject constructor() {
    val all: List<Cat> = listOf(
        Cat("miso",    "Miso",    CatTier.FREE,    "#000000", Color.Black.toArgb(),       VoiceBucket.MISO,    "Your warm, slightly judgmental roommate. Free, always.",           "#E8A87C", 1.00f),
        Cat("mocha",   "Mocha",   CatTier.FREE,    "#000000", Color.Black.toArgb(),       VoiceBucket.MOCHA,   "A Siamese kitten with way too much to say. Free, always.",         "#AED9E0", 1.25f),
        Cat("yuki",    "Yuki",    CatTier.PREMIUM, "#000000", Color.Black.toArgb(),       VoiceBucket.YUKI,    "Soft, sleepy, maternal. She'll sit with you when you need calm.",  "#F5ECD7", 1.05f),
        Cat("shadow",  "Shadow",  CatTier.PREMIUM, "#0D4F4F", Color(0xFF0D4F4F).toArgb(), VoiceBucket.SHADOW,  "Mysterious, zen, slightly witchy. Drops wisdom and leaves.",       "#062d2d", 0.85f),
        Cat("biscuit", "Biscuit", CatTier.PREMIUM, "#000000", Color.Black.toArgb(),       VoiceBucket.BISCUIT, "A cozy calico who reminds you to eat and breathe.",                "#E07A5F", 1.00f),
    )

    fun find(id: String): Cat = all.firstOrNull { it.id == id } ?: free()
    fun free(): Cat            = all.first { it.tier == CatTier.FREE }
    fun premium(): List<Cat>   = all.filter { it.tier == CatTier.PREMIUM }
}
