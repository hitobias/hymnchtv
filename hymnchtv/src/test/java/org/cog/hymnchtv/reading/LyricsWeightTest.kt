package org.cog.hymnchtv.reading

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
    fun readsEachStoredValue() {
        val sp = FakeSharedPreferences()
        for (w in LyricsWeight.entries) {
            sp.edit().putString("lyrics_font_weight", w.prefValue).apply()
            assertThat(ReadingPrefs.lyricsWeight(sp)).isEqualTo(w)
        }
        assertThat(LyricsWeight.entries.map { it.prefValue }).containsExactly("regular", "medium", "bold").inOrder()
        assertThat(ReadingPrefKeys.LYRICS_FONT_WEIGHT).isEqualTo("lyrics_font_weight")
    }

    @Test
    fun kaiPicksTheFileAndStrokeOfTheWeightAndScript() {
        fun kai(w: LyricsWeight, trad: Boolean) = LyricsFaceSpec.choose(LyricsFont.KAI, w, trad)
        assertThat(kai(LyricsWeight.REGULAR, false)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_sc, false))
        assertThat(kai(LyricsWeight.REGULAR, true)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_tc, false))
        assertThat(kai(LyricsWeight.MEDIUM, false)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_sc_medium, false))
        assertThat(kai(LyricsWeight.MEDIUM, true)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_tc_medium, false))
        // Bold = the Medium file plus the fake-bold stroke
        assertThat(kai(LyricsWeight.BOLD, false)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_sc_medium, true))
        assertThat(kai(LyricsWeight.BOLD, true)).isEqualTo(LyricsFaceSpec.Kai(R.font.hymnal_kai_tc_medium, true))
    }

    @Test
    fun systemFontUsesCssWeightsForEitherScript() {
        for (trad in listOf(false, true)) {
            assertThat(LyricsFaceSpec.choose(LyricsFont.SYSTEM, LyricsWeight.REGULAR, trad)).isEqualTo(LyricsFaceSpec.System(400))
            assertThat(LyricsFaceSpec.choose(LyricsFont.SYSTEM, LyricsWeight.MEDIUM, trad)).isEqualTo(LyricsFaceSpec.System(500))
            assertThat(LyricsFaceSpec.choose(LyricsFont.SYSTEM, LyricsWeight.BOLD, trad)).isEqualTo(LyricsFaceSpec.System(700))
        }
    }
}
