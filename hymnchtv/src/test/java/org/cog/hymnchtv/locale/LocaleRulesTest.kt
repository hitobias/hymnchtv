package org.cog.hymnchtv.locale

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class LocaleRulesTest {
    private fun t(tag: String) = Locale.forLanguageTag(tag)

    @Test
    fun traditionalByRegion() {
        listOf("zh-TW", "zh-HK", "zh-MO").forEach {
            assertThat(LocaleRules.isTraditional(t(it))).isTrue()
        }
    }

    @Test
    fun traditionalByScript() {
        listOf("zh-Hant", "zh-Hant-CN").forEach {
            assertThat(LocaleRules.isTraditional(t(it))).isTrue()
        }
    }

    @Test
    fun simplified() {
        listOf("zh-CN", "zh-SG", "zh", "zh-Hans-TW").forEach {
            assertThat(LocaleRules.isTraditional(t(it))).isFalse()
        }
    }

    @Test
    fun nonChineseIsNeverTraditional() {
        listOf("en-US", "ja-JP", "fr").forEach {
            assertThat(LocaleRules.isTraditional(t(it))).isFalse()
        }
    }

    @Test
    fun isChinese() {
        assertThat(LocaleRules.isChinese(t("zh-TW"))).isTrue()
        assertThat(LocaleRules.isChinese(t("en"))).isFalse()
    }
}
