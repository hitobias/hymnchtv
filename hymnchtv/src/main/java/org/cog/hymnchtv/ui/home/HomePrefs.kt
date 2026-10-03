package org.cog.hymnchtv.ui.home

import android.content.SharedPreferences
import org.cog.hymnchtv.MainActivity

/** Home-screen preferences (file [MainActivity.PREF_SETTINGS]; the key names are the ones the old main screen used); the settings tab writes them, the home tab reads them. */
object HomePrefs {
    /** The hymn book selected last, so the home tab opens with it highlighted (direction C). */
    const val LAST_HYMN_TYPE = "LastHymnType"
}
