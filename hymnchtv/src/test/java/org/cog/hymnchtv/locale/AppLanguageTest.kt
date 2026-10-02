package org.cog.hymnchtv.locale

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun fromPrefAcceptsPersistedValues() {
        assertThat(AppLanguage.fromPref("system")).isEqualTo(AppLanguage.SYSTEM)
        assertThat(AppLanguage.fromPref("zh-Hans-CN")).isEqualTo(AppLanguage.ZH_HANS)
        assertThat(AppLanguage.fromPref("zh-Hant-TW")).isEqualTo(AppLanguage.ZH_HANT)
        assertThat(AppLanguage.fromPref("en-US")).isEqualTo(AppLanguage.EN)
    }

    @Test
    fun fromPrefReturnsNullForMissingOrIllegal() {
        listOf(null, "", "  ", "ja-JP", "garbage").forEach {
            assertThat(AppLanguage.fromPref(it)).isNull()
        }
    }

    @Test
    fun prefValueRoundTrips() {
        AppLanguage.values().forEach {
            assertThat(AppLanguage.fromPref(it.prefValue)).isEqualTo(it)
        }
    }

    @Test
    fun fromFrameworkTagsEmptyIsSystem() {
        assertThat(AppLanguage.fromFrameworkTags(emptyList())).isEqualTo(AppLanguage.SYSTEM)
    }

    @Test
    fun fromFrameworkTagsMapsChineseVariants() {
        listOf("zh-TW", "zh-HK", "zh-MO", "zh-Hant-TW").forEach {
            assertThat(AppLanguage.fromFrameworkTags(listOf(it))).isEqualTo(AppLanguage.ZH_HANT)
        }
        listOf("zh-CN", "zh-Hans-CN", "zh").forEach {
            assertThat(AppLanguage.fromFrameworkTags(listOf(it))).isEqualTo(AppLanguage.ZH_HANS)
        }
    }

    @Test
    fun fromFrameworkTagsUsesFirstAndFallsBackToEnglish() {
        assertThat(AppLanguage.fromFrameworkTags(listOf("en-GB", "zh-TW"))).isEqualTo(AppLanguage.EN)
        assertThat(AppLanguage.fromFrameworkTags(listOf("ja-JP"))).isEqualTo(AppLanguage.EN)
    }

    @Test
    fun toLocale() {
        assertThat(AppLanguage.SYSTEM.toLocale()).isNull()
        assertThat(AppLanguage.ZH_HANT.toLocale()).isEqualTo(Locale.forLanguageTag("zh-Hant-TW"))
    }

    @Test
    fun fromFrameworkTagsIgnoresBlankTags() {
        assertThat(AppLanguage.fromFrameworkTags(listOf(""))).isEqualTo(AppLanguage.SYSTEM)
        assertThat(AppLanguage.fromFrameworkTags(listOf("", " "))).isEqualTo(AppLanguage.SYSTEM)
    }
}
