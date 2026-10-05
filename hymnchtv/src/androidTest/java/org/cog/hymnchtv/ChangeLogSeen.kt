package org.cog.hymnchtv

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * The "what's new" dialog of ckChangeLog 1.2.2 shows while the default shared preferences hold an older version code
 * under its VERSION_KEY (protected in the library, so repeated here).
 */
object ChangeLogSeen {
    const val KEY = "ckChangeLog_last_version_code"

    /** Marks this build's change log as seen: MainActivity then schedules no dialog. */
    fun mark(ctx: Context) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putInt(KEY, BuildConfig.VERSION_CODE).commit()
    }

    /** For a test of the change log itself; call [mark] again when it ends. */
    fun clear(ctx: Context) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().remove(KEY).commit()
    }
}
