package org.cog.hymnchtv.reading

import android.graphics.Typeface
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.junit.Test

class LyricsWeightTest {
    @Test
    fun defaultsToRegularAndNeverThrows() {
        val sp = FakeSharedPreferences()
        assertThat(ReadingPrefs.lyricsWeight(sp)).isEqualTo(LyricsWeight.REGULAR)
        sp.edit().putString(ReadingPrefKeys.LYRICS_FONT_WEIGHT, "nonsense").apply()
        assertThat(ReadingPrefs.lyricsWeight(sp)).isEqualTo(LyricsWeight.REGULAR)
        sp.edit().putInt(ReadingPrefKeys.LYRICS_FONT_WEIGHT, 7).apply()
        assertThat(ReadingPrefs.lyricsWeight(sp)).isEqualTo(LyricsWeight.REGULAR)
    }

    @Test
    fun readsStoredValue() {
        val sp = FakeSharedPreferences()
        sp.edit().putString("lyrics_font_weight", "bold").apply()
        assertThat(ReadingPrefs.lyricsWeight(sp)).isEqualTo(LyricsWeight.BOLD)
        assertThat(ReadingPrefKeys.LYRICS_FONT_WEIGHT).isEqualTo("lyrics_font_weight")
    }

    @Test
    fun kaiPicksTheFileOfTheWeightAndScript() {
        fun kai(w: LyricsWeight, trad: Boolean) = LyricsFaceSpec.choose(LyricsFont.KAI, w, trad)
        assertThat(kai(LyricsWeight.REGULAR, false)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_sc))
        assertThat(kai(LyricsWeight.REGULAR, true)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_tc))
        assertThat(kai(LyricsWeight.BOLD, false)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_sc_medium))
        assertThat(kai(LyricsWeight.BOLD, true)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_tc_medium))
    }

    @Test
    fun systemFontUsesNormalOrBoldStyleForEitherScript() {
        for (trad in listOf(false, true)) {
            assertThat(LyricsFaceSpec.choose(LyricsFont.SYSTEM, LyricsWeight.REGULAR, trad)).isEqualTo(LyricsFaceSpec.System(Typeface.NORMAL))
            assertThat(LyricsFaceSpec.choose(LyricsFont.SYSTEM, LyricsWeight.BOLD, trad)).isEqualTo(LyricsFaceSpec.System(Typeface.BOLD))
        }
    }
}
