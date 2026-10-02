package org.cog.hymnchtv.ui.picker

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class KaiTextTest {
    @Test fun traditionalUiIsHantOrTaiwanHongKongMacau() {
        assertThat(KaiText.isTraditionalUi(Locale.forLanguageTag("zh-Hant-TW"))).isTrue()
        assertThat(KaiText.isTraditionalUi(Locale.forLanguageTag("zh-HK"))).isTrue()
        assertThat(KaiText.isTraditionalUi(Locale.forLanguageTag("zh-Hans-CN"))).isFalse()
        assertThat(KaiText.isTraditionalUi(Locale.ENGLISH)).isFalse()
    }

    @Test fun canDrawChecksEveryPrintableCodePointAndSkipsSpaces() {
        val known = setOf("神", "愛")
        assertThat(KaiText.canDraw("神 愛") { it in known }).isTrue()
        assertThat(KaiText.canDraw("神愛𠀋") { it in known }).isFalse()
        assertThat(KaiText.canDraw("") { false }).isTrue()
    }

    @Test fun aMissingGlyphMakesTheWholeTitleFallBackToTheSystemFont() {
        // a face that lacks one code point must not be used for any of the text (no tofu boxes)
        val kai = "face"
        assertThat(KaiText.faceFor("神愛世人", kai) { _, g -> g != "愛" }).isNull()
        assertThat(KaiText.faceFor("神愛世人", kai) { _, _ -> true }).isSameInstanceAs(kai)
        assertThat(KaiText.faceFor<String>("神愛世人", null) { _, _ -> true }).isNull()
    }
}
