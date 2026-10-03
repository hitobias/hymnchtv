package org.cog.hymnchtv.ui.lyrics

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.FakeSharedPreferences
import org.cog.hymnchtv.reading.LyricsFont
import org.cog.hymnchtv.reading.LyricsFontSize
import org.cog.hymnchtv.reading.LyricsWeight
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.ReadingPrefs
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.junit.Test

/** The Aa panel writes the reading settings' own keys (spec 6b). */
class ReadingPanelPrefsTest {
    private val prefs = FakeSharedPreferences()

    @Test
    fun fontSizeWritesTheEnumAndResetsBothPinchScales() {
        prefs.edit().putFloat(ReadingPrefKeys.LYRICS_SCALE_P, 3.2f).putFloat(ReadingPrefKeys.LYRICS_SCALE_L, 2.4f).apply()
        ReadingPanelPrefs.setFontSize(prefs, LyricsFontSize.XLARGE)
        assertThat(ReadingPrefs.fontSize(prefs)).isEqualTo(LyricsFontSize.XLARGE)
        assertThat(ReadingPrefs.lyricsScale(prefs, true)).isEqualTo(LyricsFontSize.XLARGE.scale)
        assertThat(ReadingPrefs.lyricsScale(prefs, false)).isEqualTo(LyricsFontSize.XLARGE.scale)
    }

    @Test
    fun fontAndDisplayModeUseTheSettingsKeys() {
        ReadingPanelPrefs.setFont(prefs, LyricsFont.SYSTEM)
        ReadingPanelPrefs.setDisplayMode(prefs, DisplayMode.LYRICS_ONLY)
        assertThat(prefs.getString(ReadingPrefKeys.LYRICS_FONT, null)).isEqualTo("SYSTEM")
        assertThat(prefs.getString(ReadingPrefKeys.DISPLAY_MODE, null)).isEqualTo("LYRICS_ONLY")
        assertThat(ReadingPrefs.lyricsFont(prefs)).isEqualTo(LyricsFont.SYSTEM)
        assertThat(ReadingPrefs.displayMode(prefs)).isEqualTo(DisplayMode.LYRICS_ONLY)
    }

    @Test
    fun weightWritesTheSettingsKey() {
        assertThat(ReadingPrefs.lyricsWeight(prefs)).isEqualTo(LyricsWeight.REGULAR)
        ReadingPanelPrefs.setWeight(prefs, LyricsWeight.BOLD)
        assertThat(prefs.getString("lyrics_font_weight", null)).isEqualTo("bold")
        assertThat(ReadingPrefs.lyricsWeight(prefs)).isEqualTo(LyricsWeight.BOLD)
        ReadingPanelPrefs.setWeight(prefs, LyricsWeight.REGULAR)
        assertThat(prefs.getString(ReadingPrefKeys.LYRICS_FONT_WEIGHT, null)).isEqualTo("regular")
    }

    @Test
    fun themeWritesTheLyricsSlotOnly() {
        ReadingPanelPrefs.setTheme(prefs, BackgroundChoice.Preset(BackgroundPreset.EYE_GREEN))
        assertThat(prefs.getString("LyricsBackground", null)).isEqualTo("eye_green")
        assertThat(prefs.contains("MainBackground")).isFalse()
    }
}
