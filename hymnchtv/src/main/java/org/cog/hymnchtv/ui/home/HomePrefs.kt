package org.cog.hymnchtv.ui.home

import android.content.SharedPreferences
import org.cog.hymnchtv.MainActivity

/** Home-screen preferences (file [MainActivity.PREF_SETTINGS]); the settings tab writes them, the home tab reads them. */
object HomePrefs {
    const val TEXT_SIZE = MainActivity.PREF_TEXT_SIZE
    const val TEXT_COLOR = MainActivity.PREF_TEXT_COLOR

    /** The hymn book selected last, so the home tab opens with it highlighted (direction C). */
    const val LAST_HYMN_TYPE = "LastHymnType"

    const val TEXT_SIZE_DEFAULT = 35
    const val TEXT_SIZE_MIN = 25
    const val TEXT_SIZE_MAX = 50

    fun textSize(prefs: SharedPreferences): Int =
        runCatching { prefs.getInt(TEXT_SIZE, TEXT_SIZE_DEFAULT) }.getOrDefault(TEXT_SIZE_DEFAULT)
            .coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX)

    fun textColor(prefs: SharedPreferences, fallback: Int): Int =
        runCatching { prefs.getInt(TEXT_COLOR, fallback) }.getOrDefault(fallback)
}
