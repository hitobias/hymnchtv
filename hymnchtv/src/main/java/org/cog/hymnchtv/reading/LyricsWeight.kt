package org.cog.hymnchtv.reading

import android.graphics.Typeface
import androidx.annotation.FontRes
import org.cog.hymnchtv.R

/** Lyrics weight: the font's Regular, or its heavier real weight (HymnalKai Medium; the system font's bold). */
enum class LyricsWeight(val prefValue: String) {
    REGULAR("regular"),
    BOLD("bold");

    companion object {
        /** Never throws; unknown or missing values mean REGULAR. */
        @JvmStatic
        fun fromPref(value: String?): LyricsWeight = entries.firstOrNull { it.prefValue == value } ?: REGULAR
    }
}

/** Which face a lyrics view gets for a font and weight; pure, so the 2 x 2 choice is unit-testable. */
sealed interface LyricsFaceSpec {
    /** A bundled HymnalKai file; the weight is in the file, so no synthetic bolding is ever applied. */
    data class Kai(@FontRes val res: Int) : LyricsFaceSpec

    /** The device font in [style] (Typeface.NORMAL or Typeface.BOLD). */
    data class System(val style: Int) : LyricsFaceSpec

    companion object {
        @JvmStatic
        fun choose(font: LyricsFont, weight: LyricsWeight, traditionalScript: Boolean): LyricsFaceSpec {
            val bold = weight == LyricsWeight.BOLD
            return when (font) {
                LyricsFont.SYSTEM -> System(if (bold) Typeface.BOLD else Typeface.NORMAL)
                LyricsFont.KAI -> Kai(
                    when {
                        traditionalScript && bold -> R.font.hymnal_kai_tc_medium
                        traditionalScript -> R.font.hymnal_kai_tc
                        bold -> R.font.hymnal_kai_sc_medium
                        else -> R.font.hymnal_kai_sc
                    },
                )
            }
        }
    }
}
