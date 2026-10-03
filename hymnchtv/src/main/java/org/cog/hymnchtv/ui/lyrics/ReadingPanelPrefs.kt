package org.cog.hymnchtv.ui.lyrics

import android.content.SharedPreferences
import org.cog.hymnchtv.reading.DisplayMode
import org.cog.hymnchtv.reading.LyricsFont
import org.cog.hymnchtv.reading.LyricsFontSize
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.ReadingPrefs
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot

/**
 * What the Aa quick panel writes: the very same keys as the reading settings screen (spec 6b), so both always agree.
 * Writes are synchronous in memory (apply), so a page that reads them right afterwards sees the new values.
 */
object ReadingPanelPrefs {
    /** The lyrics slot only; the home background is chosen in settings. */
    @JvmStatic
    fun setTheme(prefs: SharedPreferences, choice: BackgroundChoice) {
        prefs.edit().putString(BackgroundSlot.LYRICS.prefKey, BackgroundPolicy.prefValue(choice)).apply()
    }

    /** Choosing a size also restarts pinch zoom from it, in both orientations (otherwise a stored scale hides the change). */
    @JvmStatic
    fun setFontSize(prefs: SharedPreferences, size: LyricsFontSize) {
        ReadingPrefs.resetLyricsScale(prefs.edit().putString(ReadingPrefKeys.LYRICS_FONT_SIZE, size.name), size).apply()
    }

    @JvmStatic
    fun setFont(prefs: SharedPreferences, font: LyricsFont) {
        prefs.edit().putString(ReadingPrefKeys.LYRICS_FONT, font.name).apply()
    }

    @JvmStatic
    fun setDisplayMode(prefs: SharedPreferences, mode: DisplayMode) {
        prefs.edit().putString(ReadingPrefKeys.DISPLAY_MODE, mode.name).apply()
    }
}
