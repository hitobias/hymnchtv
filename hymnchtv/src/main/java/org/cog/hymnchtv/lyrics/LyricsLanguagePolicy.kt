package org.cog.hymnchtv.lyrics

import org.cog.hymnchtv.locale.LocaleRules
import java.util.Locale

object LyricsLanguagePolicy {
    const val PREF_LYRICS_DEFAULT = "LyricsDefaultLang"

    /** Hong Kong glyphs are hidden for now (user decision 2026-10-02); assets stay so this can be re-enabled. */
    const val HK_VARIANT_ENABLED = false

    private val HK_REGIONS = setOf("HK", "MO")

    @JvmStatic
    fun resolveShowTraditional(pref: LyricsLang, uiLocale: Locale): Boolean = when (pref) {
        LyricsLang.SIMPLIFIED -> false
        LyricsLang.TRADITIONAL -> true
        LyricsLang.FOLLOW_UI -> LocaleRules.isTraditional(uiLocale)
    }

    @JvmStatic
    fun defaultVariant(uiLocale: Locale): HantVariant =
        if (HK_VARIANT_ENABLED && LocaleRules.isChinese(uiLocale) && uiLocale.country in HK_REGIONS) HantVariant.HK else HantVariant.TW

    /** True only for values written by this version ("S2TW"/"S2HK"). */
    @JvmStatic
    fun isCanonical(value: String?): Boolean = HantVariant.entries.any { it.prefValue == value }

    /** Never throws; a missing or invalid value follows the UI region. */
    @JvmStatic
    fun parseVariant(value: String?, uiLocale: Locale): HantVariant =
        HantVariant.entries.firstOrNull { it.prefValue == value && (HK_VARIANT_ENABLED || it != HantVariant.HK) }
            ?: defaultVariant(uiLocale)
}
