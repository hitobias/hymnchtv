package org.cog.hymnchtv

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies values / values-zh / values-b+zh+Hant are picked as designed (plan A.1.8). Run on API 24 and 34. */
@RunWith(AndroidJUnit4::class)
class ResourceLocaleResolutionTest {
    private fun localeSystemString(tag: String): String {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(ctx.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        return ctx.createConfigurationContext(config).getString(R.string.locale_system)
    }

    @Test
    fun simplifiedChineseLocales() {
        listOf("zh-CN", "zh-SG", "zh-Hans").forEach {
            assertThat(localeSystemString(it)).isEqualTo("跟随系统")
        }
    }

    @Test
    fun traditionalChineseLocales() {
        listOf("zh-TW", "zh-HK", "zh-MO", "zh-Hant").forEach {
            assertThat(localeSystemString(it)).isEqualTo("跟隨系統")
        }
    }

    @Test
    fun otherLocalesFallBackToEnglish() {
        listOf("en-US", "ja-JP", "fr-FR").forEach {
            assertThat(localeSystemString(it)).isEqualTo("Follow system")
        }
    }
}
