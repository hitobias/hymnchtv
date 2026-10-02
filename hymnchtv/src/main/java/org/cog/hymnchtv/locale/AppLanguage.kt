package org.cog.hymnchtv.locale

import java.util.Locale

/**
 * UI language choice. [tag] is the BCP-47 tag applied to resources; null means follow the system.
 * [prefValue] is what is persisted in PREF_LOCALE (API < 33).
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null), ZH_HANS("zh-Hans-CN"), ZH_HANT("zh-Hant-TW"), EN("en-US");

    val prefValue: String get() = tag ?: SYSTEM_PREF_VALUE

    fun toLocale(): Locale? = tag?.let(Locale::forLanguageTag)

    companion object {
        private const val SYSTEM_PREF_VALUE = "system"

        /** Never throws: null, blank or unknown values return null so callers pick the fallback. */
        @JvmStatic
        fun fromPref(value: String?): AppLanguage? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.prefValue == value }
        }

        /** Maps the framework per-app locale list (API 33+); unsupported languages display as English. */
        @JvmStatic
        fun fromFrameworkTags(tags: List<String>): AppLanguage {
            val first = tags.firstOrNull { it.isNotBlank() } ?: return SYSTEM
            val locale = Locale.forLanguageTag(first)
            return when {
                LocaleRules.isTraditional(locale) -> ZH_HANT
                LocaleRules.isChinese(locale) -> ZH_HANS
                else -> EN
            }
        }
    }
}
