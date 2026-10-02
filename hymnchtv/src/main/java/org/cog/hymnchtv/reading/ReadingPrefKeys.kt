package org.cog.hymnchtv.reading

/** SharedPreferences keys (file MainActivity.PREF_SETTINGS) owned by the reading settings (plan A2). */
object ReadingPrefKeys {
    const val DISPLAY_MODE = "DisplayMode"
    const val LYRICS_FONT_SIZE = "LyricsFontSize"
    const val LYRICS_FONT = "LyricsFont"
    /** Shared with sub-project B-11 (low-RAM phones will default it off). */
    const val PAGE_ANIMATION = "PageAnimation"
    const val KEEP_SCREEN_ON = "KeepScreenOn"
    /** Player shown by default; MainActivity.PREF_MENU_SHOW aliases this key. */
    const val MENU_SHOW = "MenuShow"
    /** Pinch-zoom scale per orientation; ContentView.PREF_LYRICS_SCALE_P / _L alias these keys. */
    const val LYRICS_SCALE_P = "LyricsScaleP"
    const val LYRICS_SCALE_L = "LyricsScaleL"
}
