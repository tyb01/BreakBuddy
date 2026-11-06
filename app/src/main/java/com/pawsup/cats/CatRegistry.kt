package com.pawsup.cats

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatRegistry @Inject constructor() {
    val all: List<Cat> = listOf(
        Cat("miso",    "Miso",    CatTier.FREE,    "#000000", Color.Black.toArgb(),       VoiceBucket.MISO,    com.pawsup.R.string.blurb_miso,           "#E8A87C", 1.00f),
        Cat("mocha",   "Mocha",   CatTier.FREE,    "#000000", Color.Black.toArgb(),       VoiceBucket.MOCHA,   com.pawsup.R.string.blurb_mocha,         "#AED9E0", 1.25f),
        Cat("yuki",    "Yuki",    CatTier.PREMIUM, "#000000", Color.Black.toArgb(),       VoiceBucket.YUKI,    com.pawsup.R.string.blurb_yuki,  "#F5ECD7", 1.05f),
        Cat("shadow",  "Shadow",  CatTier.PREMIUM, "#0D4F4F", Color(0xFF0D4F4F).toArgb(), VoiceBucket.SHADOW,  com.pawsup.R.string.blurb_shadow,       "#062d2d", 0.85f),
        Cat("biscuit", "Biscuit", CatTier.PREMIUM, "#000000", Color.Black.toArgb(),       VoiceBucket.BISCUIT, com.pawsup.R.string.blurb_biscuit,                "#E07A5F", 1.00f),
    )

    fun find(id: String): Cat = all.firstOrNull { it.id == id } ?: free()
    fun free(): Cat            = all.first { it.tier == CatTier.FREE }
    fun premium(): List<Cat>   = all.filter { it.tier == CatTier.PREMIUM }
}
