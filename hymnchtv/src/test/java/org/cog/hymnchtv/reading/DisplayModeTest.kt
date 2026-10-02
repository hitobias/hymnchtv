package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.DisplayMode.LYRICS_ONLY
import org.cog.hymnchtv.reading.DisplayMode.SCORE_AND_LYRICS
import org.cog.hymnchtv.reading.DisplayMode.SCORE_ONLY
import org.junit.Test

class DisplayModeTest {
    @Test
    fun parsesStoredNames() {
        DisplayMode.entries.forEach { assertThat(DisplayMode.fromPref(it.name)).isEqualTo(it) }
    }

    @Test
    fun invalidOrMissingFallsBackToScoreAndLyrics() {
        listOf(null, "", "score_only", "BOTH").forEach {
            assertThat(DisplayMode.fromPref(it)).isEqualTo(SCORE_AND_LYRICS)
        }
    }

    @Test
    fun nextCyclesThroughAllModes() {
        assertThat(SCORE_AND_LYRICS.next()).isEqualTo(SCORE_ONLY)
        assertThat(SCORE_ONLY.next()).isEqualTo(LYRICS_ONLY)
        assertThat(LYRICS_ONLY.next()).isEqualTo(SCORE_AND_LYRICS)
    }

    @Test
    fun visibilityFlags() {
        assertThat(SCORE_AND_LYRICS.showScore && SCORE_AND_LYRICS.showLyrics).isTrue()
        assertThat(SCORE_ONLY.showScore && !SCORE_ONLY.showLyrics).isTrue()
        assertThat(!LYRICS_ONLY.showScore && LYRICS_ONLY.showLyrics).isTrue()
    }

    @Test
    fun sessionOverrideWinsOverStoredDefault() {
        assertThat(DisplayModePolicy.resolve(LYRICS_ONLY, SCORE_ONLY)).isEqualTo(LYRICS_ONLY)
        assertThat(DisplayModePolicy.resolve(null, SCORE_ONLY)).isEqualTo(SCORE_ONLY)
    }

    @Test
    fun lyricsOnlyWithoutTextShowsBoth() {
        assertThat(DisplayModePolicy.effective(LYRICS_ONLY, false)).isEqualTo(SCORE_AND_LYRICS)
        assertThat(DisplayModePolicy.effective(LYRICS_ONLY, true)).isEqualTo(LYRICS_ONLY)
        assertThat(DisplayModePolicy.effective(SCORE_ONLY, false)).isEqualTo(SCORE_ONLY)
        assertThat(DisplayModePolicy.effective(SCORE_AND_LYRICS, false)).isEqualTo(SCORE_AND_LYRICS)
    }

    @Test
    fun lyricsTextThresholdMatchesJiaoChangHint() {
        assertThat(DisplayModePolicy.hasLyricsText(null)).isFalse()
        assertThat(DisplayModePolicy.hasLyricsText("")).isFalse()
        assertThat(DisplayModePolicy.hasLyricsText("詞".repeat(39))).isFalse()
        assertThat(DisplayModePolicy.hasLyricsText("詞".repeat(40))).isTrue()
    }
}
