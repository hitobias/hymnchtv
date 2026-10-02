package org.cog.hymnchtv.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HtmlTextTest {
    @Test
    fun escapesMarkupCharacters() {
        assertThat(HtmlText.escape("""<a href="x">Tom & Jerry's</a>"""))
            .isEqualTo("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&#39;s&lt;/a&gt;")
    }

    @Test
    fun leavesChineseAndPlainTextUntouched() {
        assertThat(HtmlText.escape("詩歌 Hymnal 1.0.0")).isEqualTo("詩歌 Hymnal 1.0.0")
    }

    @Test
    fun emptyStaysEmpty() {
        assertThat(HtmlText.escape("")).isEmpty()
    }
}
