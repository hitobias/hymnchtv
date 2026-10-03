package org.cog.hymnchtv.reading

import androidx.annotation.FontRes
import org.cog.hymnchtv.R

/**
 * Lyrics weight in three steps: the font's Regular, its real Medium, and Bold = Medium with the paint's fake-bold stroke
 * added on top (clearer than stroking Regular). The stroke applies to lyrics text only, never to score images.
 */
enum class LyricsWeight(val prefValue: String) {
    REGULAR("regular"),
    MEDIUM("medium"),
    BOLD("bold");

    companion object {
        /** Never throws; unknown or missing values mean REGULAR. */
        @JvmStatic
        fun fromPref(value: String?): LyricsWeight = entries.firstOrNull { it.prefValue == value } ?: REGULAR
    }
}

/** Which face a lyrics view gets for a font and weight; pure, so the 2 x 3 choice is unit-testable. */
sealed interface LyricsFaceSpec {
    /** A bundled HymnalKai file; [fakeBold] adds the paint's stroke on top (Bold uses the Medium file). */
    data class Kai(@FontRes val res: Int, val fakeBold: Boolean) : LyricsFaceSpec

    /** The device font at CSS-like [weight] 400 / 500 / 700; see [LyricsTypefaces.systemFace] for the API fallbacks. */
    data class System(val weight: Int) : LyricsFaceSpec

    companion object {
        @JvmStatic
        fun choose(font: LyricsFont, weight: LyricsWeight, traditionalScript: Boolean): LyricsFaceSpec = when (font) {
            LyricsFont.SYSTEM -> System(when (weight) {
                LyricsWeight.REGULAR -> 400
                LyricsWeight.MEDIUM -> 500
                LyricsWeight.BOLD -> 700
            })
            LyricsFont.KAI -> {
                val medium = weight != LyricsWeight.REGULAR
                Kai(
                    when {
                        traditionalScript && medium -> R.font.hymnal_kai_tc_medium
                        traditionalScript -> R.font.hymnal_kai_tc
                        medium -> R.font.hymnal_kai_sc_medium
                        else -> R.font.hymnal_kai_sc
                    },
                    fakeBold = weight == LyricsWeight.BOLD,
                )
            }
        }
    }
}
