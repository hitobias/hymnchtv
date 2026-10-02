package org.cog.hymnchtv.lyrics

import android.content.Context
import android.content.SharedPreferences
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import java.util.Locale

/**
 * Which script hymn titles and tables of contents are shown in: the reader's default lyrics language
 * (follow the app language unless Simplified or Traditional is chosen), the same rule the lyrics page uses.
 */
object LyricsScript {
    /** The Traditional variant to show, or null for Simplified. */
    @JvmStatic
    fun hantVariant(prefs: SharedPreferences, locale: Locale): HantVariant? {
        val lang = LyricsLang.fromPref(prefs.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null))
        if (!LyricsLanguagePolicy.resolveShowTraditional(lang, locale)) return null
        return LyricsLanguagePolicy.parseVariant(prefs.getString(ContentView.PREF_CONVERSION_TYPE, null), locale)
    }

    @JvmStatic
    fun hantVariant(context: Context): HantVariant? {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
        return hantVariant(prefs, context.resources.configuration.locales[0])
    }
}
