package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReadingPrefsTest {
    @Test
    fun emptyPrefsGiveTheDefaults() {
        val sp = FakeSharedPreferences()
        assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.LYRICS_ONLY)
        assertThat(ReadingPrefs.fontSize(sp)).isEqualTo(LyricsFontSize.MEDIUM)
        assertThat(ReadingPrefs.lyricsFont(sp)).isEqualTo(LyricsFont.KAI)
        assertThat(ReadingPrefs.pageAnimation(sp)).isTrue()
        assertThat(ReadingPrefs.keepScreenOn(sp)).isTrue()
        assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.MEDIUM.scale)
    }

    @Test
    fun storedValuesAreRead() {
        val sp = FakeSharedPreferences(
            mapOf(
                ReadingPrefKeys.DISPLAY_MODE to "LYRICS_ONLY",
                ReadingPrefKeys.LYRICS_FONT_SIZE to "XLARGE",
                ReadingPrefKeys.LYRICS_FONT to "SYSTEM",
                ReadingPrefKeys.PAGE_ANIMATION to false,
                ReadingPrefKeys.KEEP_SCREEN_ON to false,
                ReadingPrefKeys.LYRICS_SCALE_L to 2.5f,
            )
        )
        assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.LYRICS_ONLY)
        assertThat(ReadingPrefs.fontSize(sp)).isEqualTo(LyricsFontSize.XLARGE)
        assertThat(ReadingPrefs.lyricsFont(sp)).isEqualTo(LyricsFont.SYSTEM)
        assertThat(ReadingPrefs.pageAnimation(sp)).isFalse()
        assertThat(ReadingPrefs.keepScreenOn(sp)).isFalse()
        assertThat(ReadingPrefs.lyricsScale(sp, false)).isEqualTo(2.5f)
        assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.XLARGE.scale)
    }

    @Test
    fun wrongTypesFallBackInsteadOfCrashing() {
        val sp = FakeSharedPreferences(
            mapOf(
                ReadingPrefKeys.DISPLAY_MODE to 3,
                ReadingPrefKeys.PAGE_ANIMATION to "yes",
                ReadingPrefKeys.LYRICS_SCALE_P to "big",
            )
        )
        assertThat(ReadingPrefs.displayMode(sp)).isEqualTo(DisplayMode.LYRICS_ONLY)
        assertThat(ReadingPrefs.pageAnimation(sp)).isTrue()
        assertThat(ReadingPrefs.lyricsScale(sp, true)).isEqualTo(LyricsFontSize.MEDIUM.scale)
    }

    @Test
    fun choosingASizeResetsBothOrientations() {
        val sp = FakeSharedPreferences(mapOf(ReadingPrefKeys.LYRICS_SCALE_P to 4f, ReadingPrefKeys.LYRICS_SCALE_L to 3f))
        ReadingPrefs.resetLyricsScale(sp.edit(), LyricsFontSize.SMALL).apply()
        assertThat(sp.values[ReadingPrefKeys.LYRICS_SCALE_P]).isEqualTo(1.0f)
        assertThat(sp.values[ReadingPrefKeys.LYRICS_SCALE_L]).isEqualTo(1.0f)
    }
}
