package org.cog.hymnchtv.locale

import java.util.Locale

/** Pure rules for classifying a [Locale]; no Android dependency. */
object LocaleRules {
    private val TRADITIONAL_REGIONS = setOf("TW", "HK", "MO")

    @JvmStatic
    fun isChinese(locale: Locale): Boolean = locale.language == "zh"

    /** An explicit script wins over region: zh-Hans-TW is simplified, zh-Hant-CN is traditional. */
    @JvmStatic
    fun isTraditional(locale: Locale): Boolean {
        if (!isChinese(locale)) return false
        if (locale.script.isNotEmpty()) return locale.script == "Hant"
        return locale.country in TRADITIONAL_REGIONS
    }
}
