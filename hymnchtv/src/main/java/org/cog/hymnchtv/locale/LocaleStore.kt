package org.cog.hymnchtv.locale

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/**
 * Single source of truth for the UI language, chosen per API level (plan A.1.2):
 * API 33+ uses the framework per-app locale (shared with Settings > App languages);
 * API < 33 uses PREF_LOCALE and a wrapped context.
 */
object LocaleStore {
    /** Same value as MainActivity.PREF_SETTINGS; PREF_LOCALE is the source of truth that MainActivity refers to. */
    const val PREF_SETTINGS = "Settings"
    const val PREF_LOCALE = "Locale"

    @JvmStatic
    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

    @JvmStatic
    fun current(context: Context): AppLanguage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AppLanguage.fromFrameworkTags(frameworkTags(context))
        } else {
            AppLanguage.fromPref(prefs(context).getString(PREF_LOCALE, null)) ?: AppLanguage.SYSTEM
        }

    /** @return true when the caller must restart the process to apply it (API < 33 only). */
    @JvmStatic
    fun set(context: Context, language: AppLanguage): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setFramework(context, language)
            return false
        }
        prefs(context).edit().putString(PREF_LOCALE, language.prefValue).commit()
        return true
    }

    /** API 33+: never wrap, the framework applies the locale. API < 33: wrap only for an explicit choice. */
    @JvmStatic
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = current(base).toLocale() ?: return base
        // Override ONLY the locale; copying base's configuration would freeze orientation/uiMode.
        val config = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @JvmStatic
    fun frameworkTags(context: Context): List<String> {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return (0 until locales.size()).map { locales[it].toLanguageTag() }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setFramework(context: Context, language: AppLanguage) {
        val locales = language.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
        context.getSystemService(LocaleManager::class.java).applicationLocales = locales
    }
}
