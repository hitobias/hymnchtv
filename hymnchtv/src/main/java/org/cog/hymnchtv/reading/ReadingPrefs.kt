package org.cog.hymnchtv.reading

import android.content.SharedPreferences

/** Typed, never-throwing reads of the reading settings (plan A2). */
object ReadingPrefs {
    @JvmStatic
    fun displayMode(sp: SharedPreferences): DisplayMode = DisplayMode.fromPref(string(sp, ReadingPrefKeys.DISPLAY_MODE))

    @JvmStatic
    fun fontSize(sp: SharedPreferences): LyricsFontSize = LyricsFontSize.fromPref(string(sp, ReadingPrefKeys.LYRICS_FONT_SIZE))

    @JvmStatic
    fun lyricsFont(sp: SharedPreferences): LyricsFont = LyricsFont.fromPref(string(sp, ReadingPrefKeys.LYRICS_FONT))

    @JvmStatic
    fun pageAnimation(sp: SharedPreferences): Boolean = bool(sp, ReadingPrefKeys.PAGE_ANIMATION, true)

    @JvmStatic
    fun keepScreenOn(sp: SharedPreferences): Boolean = bool(sp, ReadingPrefKeys.KEEP_SCREEN_ON, true)

    /** Pinch scale for the orientation, or the chosen preset's scale when there is no usable stored value. */
    @JvmStatic
    fun lyricsScale(sp: SharedPreferences, portrait: Boolean): Float {
        val key = if (portrait) ReadingPrefKeys.LYRICS_SCALE_P else ReadingPrefKeys.LYRICS_SCALE_L
        val stored = if (sp.contains(key)) runCatching { sp.getFloat(key, 0f) }.getOrNull() else null
        return LyricsScale.resolve(stored, fontSize(sp))
    }

    /** Choosing a size restarts pinch zoom from that size in both orientations. */
    @JvmStatic
    fun resetLyricsScale(editor: SharedPreferences.Editor, size: LyricsFontSize): SharedPreferences.Editor =
        editor.putFloat(ReadingPrefKeys.LYRICS_SCALE_P, size.scale).putFloat(ReadingPrefKeys.LYRICS_SCALE_L, size.scale)

    private fun string(sp: SharedPreferences, key: String): String? = runCatching { sp.getString(key, null) }.getOrNull()

    private fun bool(sp: SharedPreferences, key: String, default: Boolean): Boolean =
        runCatching { sp.getBoolean(key, default) }.getOrDefault(default)
}
