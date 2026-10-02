package org.cog.hymnchtv.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import org.cog.hymnchtv.utils.ThemeHelper

/** Night-mode preference (follow system / light / dark). */
enum class NightMode(val delegate: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        /** Users who never chose a theme get the light theme (A2 decision, ThemeHelper.DEFAULT_THEME). */
        val DEFAULT = LIGHT

        fun from(name: String?): NightMode = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/** Single source of truth for the theme preference. Persisted in SharedPreferences, so it survives process death. */
object ThemePrefs {
    const val PREF_SETTINGS = "Settings"
    const val PREF_THEME = "Theme" // same key as before; legacy values DARK/LIGHT map directly

    fun current(context: Context): NightMode =
        NightMode.from(context.getSharedPreferences(PREF_SETTINGS, 0).getString(PREF_THEME, null))

    fun apply(context: Context, mode: NightMode) {
        context.getSharedPreferences(PREF_SETTINGS, 0).edit().putString(PREF_THEME, mode.name).apply()
        activate(context, mode)
    }

    /** Applies the stored mode without rewriting it (call from Application.onCreate: restores state after process death). */
    fun applyStored(context: Context) = activate(context, current(context))

    /** SYSTEM mode: re-sync the ThemeHelper cache when the system dark/light state changes while the app is alive. */
    fun resync(context: Context) = ThemeHelper.syncDark(resolveDark(context, current(context)))

    private fun activate(context: Context, mode: NightMode) {
        AppCompatDelegate.setDefaultNightMode(mode.delegate)
        ThemeHelper.syncDark(resolveDark(context, mode))
    }

    private fun resolveDark(context: Context, mode: NightMode): Boolean = when (mode) {
        NightMode.DARK -> true
        NightMode.LIGHT -> false
        NightMode.SYSTEM ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
