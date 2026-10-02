package org.cog.hymnchtv.about

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LicenseTextTest {
    @Test
    fun plainStripsBreakTagsAndNormalisesLineEndings() {
        assertThat(LicenseText.plain("1. Intro\n<br />\r\n2. Terms<br/>x")).isEqualTo("1. Intro\n\n2. Terms\nx")
    }

    @Test
    fun titleShowsNameVersionAndLicenses() {
        val row = LicenseRow("a:b", "Lib", "1.0", null, listOf(LicenseInfo("MIT", "MIT License", null, "text")))
        assertThat(LicenseText.title(row)).isEqualTo("Lib 1.0 — MIT License")
    }

    @Test
    fun detailIncludesNoticeTextAndPlainUrlFallback() {
        val row = LicenseRow(
            "a:b", "Lib", null, "Copyright X",
            listOf(LicenseInfo("k", "Some License", "https://example.org/LICENSE", null), LicenseInfo("Apache-2.0", "Apache License 2.0", null, "Apache text")),
        )
        val detail = LicenseText.detail(row, "（未附授權全文）")
        assertThat(detail).contains("Copyright X")
        assertThat(detail).contains("Some License\n（未附授權全文）\nhttps://example.org/LICENSE")
        assertThat(detail).contains("Apache License 2.0\nApache text")
    }
}
