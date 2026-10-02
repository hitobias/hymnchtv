package org.cog.hymnchtv.lyrics

import com.google.common.truth.Truth.assertThat
import com.zqc.opencc.android.lib.ConversionType
import org.junit.Test
import java.util.Locale

class LyricsLanguagePolicyTest {
    private val hans = Locale.forLanguageTag("zh-Hans-CN")
    private val hantTw = Locale.forLanguageTag("zh-Hant-TW")
    private val hantHk = Locale.forLanguageTag("zh-Hant-HK")
    private val en = Locale.forLanguageTag("en-US")

    @Test
    fun lyricsLangFromPrefIsTotal() {
        assertThat(LyricsLang.fromPref("TRADITIONAL")).isEqualTo(LyricsLang.TRADITIONAL)
        assertThat(LyricsLang.fromPref("SIMPLIFIED")).isEqualTo(LyricsLang.SIMPLIFIED)
        listOf(null, "", "bogus", "traditional").forEach {
            assertThat(LyricsLang.fromPref(it)).isEqualTo(LyricsLang.FOLLOW_UI)
        }
    }

    @Test
    fun explicitChoiceIgnoresUi() {
        listOf(hans, hantTw, en).forEach {
            assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.TRADITIONAL, it)).isTrue()
            assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.SIMPLIFIED, it)).isFalse()
        }
    }

    @Test
    fun followUi() {
        assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hantTw)).isTrue()
        assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hantHk)).isTrue()
        assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, hans)).isFalse()
        assertThat(LyricsLanguagePolicy.resolveShowTraditional(LyricsLang.FOLLOW_UI, en)).isFalse()
    }

    @Test
    fun hantVariantMetadata() {
        assertThat(HantVariant.TW.conversion).isEqualTo(ConversionType.S2TW)
        assertThat(HantVariant.HK.conversion).isEqualTo(ConversionType.S2HK)
        assertThat(HantVariant.TW.prefValue).isEqualTo("S2TW")
        assertThat(HantVariant.HK.dirSuffix).isEqualTo("_hant_hk")
    }

    @Test
    fun defaultVariant() {
        assertThat(LyricsLanguagePolicy.defaultVariant(hantHk)).isEqualTo(HantVariant.HK)
        assertThat(LyricsLanguagePolicy.defaultVariant(Locale.forLanguageTag("zh-MO"))).isEqualTo(HantVariant.HK)
        listOf(hantTw, hans, en).forEach {
            assertThat(LyricsLanguagePolicy.defaultVariant(it)).isEqualTo(HantVariant.TW)
        }
    }

    @Test
    fun parseVariantReadsStoredValues() {
        assertThat(LyricsLanguagePolicy.parseVariant("S2HK", hans)).isEqualTo(HantVariant.HK)
        assertThat(LyricsLanguagePolicy.parseVariant("S2TW", hantHk)).isEqualTo(HantVariant.TW)
    }

    @Test
    fun parseVariantFallsBackToLocaleDefaultWithoutThrowing() {
        listOf(null, "", "S2T", "S2TWP", "T2S", "s2tw", "bogus").forEach {
            assertThat(LyricsLanguagePolicy.parseVariant(it, hantHk)).isEqualTo(HantVariant.HK)
            assertThat(LyricsLanguagePolicy.parseVariant(it, hantTw)).isEqualTo(HantVariant.TW)
        }
    }

    @Test
    fun canonicalValues() {
        assertThat(LyricsLanguagePolicy.isCanonical("S2TW")).isTrue()
        assertThat(LyricsLanguagePolicy.isCanonical("S2HK")).isTrue()
        listOf(null, "S2T", "S2TWP", "bogus").forEach {
            assertThat(LyricsLanguagePolicy.isCanonical(it)).isFalse()
        }
    }
}
