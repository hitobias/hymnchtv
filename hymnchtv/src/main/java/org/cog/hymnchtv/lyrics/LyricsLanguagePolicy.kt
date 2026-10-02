package org.cog.hymnchtv.lyrics

import org.cog.hymnchtv.locale.LocaleRules
import java.util.Locale

object LyricsLanguagePolicy {
    const val PREF_LYRICS_DEFAULT = "LyricsDefaultLang"

    private val HK_REGIONS = setOf("HK", "MO")

    @JvmStatic
    fun resolveShowTraditional(pref: LyricsLang, uiLocale: Locale): Boolean = when (pref) {
        LyricsLang.SIMPLIFIED -> false
        LyricsLang.TRADITIONAL -> true
        LyricsLang.FOLLOW_UI -> LocaleRules.isTraditional(uiLocale)
    }

    @JvmStatic
    fun defaultVariant(uiLocale: Locale): HantVariant =
        if (LocaleRules.isChinese(uiLocale) && uiLocale.country in HK_REGIONS) HantVariant.HK else HantVariant.TW

    /** True only for values written by this version ("S2TW"/"S2HK"). */
    @JvmStatic
    fun isCanonical(value: String?): Boolean = HantVariant.entries.any { it.prefValue == value }

    /** Never throws; a missing or invalid value follows the UI region. */
    @JvmStatic
    fun parseVariant(value: String?, uiLocale: Locale): HantVariant =
        HantVariant.entries.firstOrNull { it.prefValue == value } ?: defaultVariant(uiLocale)
}
