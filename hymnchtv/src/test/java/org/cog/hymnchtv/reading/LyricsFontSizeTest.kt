package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LyricsFontSizeTest {
    @Test
    fun presetsGrowAndMediumKeepsTheOldDefaults() {
        val scales = LyricsFontSize.entries.map { it.scale }
        assertThat(scales).isInStrictOrder()
        assertThat(LyricsFontSize.MEDIUM.scale * LyricsScale.BASE_SP_PORTRAIT).isEqualTo(20f)
        assertThat(LyricsFontSize.MEDIUM.scale * LyricsScale.BASE_SP_LANDSCAPE).isEqualTo(35f)
        assertThat(LyricsFontSize.SMALL.scale).isEqualTo(LyricsScale.MIN)
    }

    @Test
    fun fontSizeFromPrefNeverThrows() {
        LyricsFontSize.entries.forEach { assertThat(LyricsFontSize.fromPref(it.name)).isEqualTo(it) }
        listOf(null, "", "medium", "HUGE").forEach { assertThat(LyricsFontSize.fromPref(it)).isEqualTo(LyricsFontSize.MEDIUM) }
    }

    @Test
    fun lyricsFontFromPrefDefaultsToKai() {
        assertThat(LyricsFont.fromPref("SYSTEM")).isEqualTo(LyricsFont.SYSTEM)
        assertThat(LyricsFont.fromPref("KAI")).isEqualTo(LyricsFont.KAI)
        listOf(null, "", "kai").forEach { assertThat(LyricsFont.fromPref(it)).isEqualTo(LyricsFont.KAI) }
    }

    @Test
    fun noStoredScaleStartsFromThePreset() {
        assertThat(LyricsScale.resolve(null, LyricsFontSize.LARGE)).isEqualTo(1.5f)
        assertThat(LyricsScale.resolve(Float.NaN, LyricsFontSize.LARGE)).isEqualTo(1.5f)
        assertThat(LyricsScale.resolve(0f, LyricsFontSize.SMALL)).isEqualTo(1.0f)
        assertThat(LyricsScale.resolve(-2f, LyricsFontSize.XLARGE)).isEqualTo(2.0f)
    }

    @Test
    fun storedPinchScaleWinsButIsClamped() {
        assertThat(LyricsScale.resolve(3.0f, LyricsFontSize.SMALL)).isEqualTo(3.0f)
        assertThat(LyricsScale.resolve(9.0f, LyricsFontSize.SMALL)).isEqualTo(LyricsScale.MAX)
        assertThat(LyricsScale.resolve(0.5f, LyricsFontSize.LARGE)).isEqualTo(LyricsScale.MIN)
    }
}
