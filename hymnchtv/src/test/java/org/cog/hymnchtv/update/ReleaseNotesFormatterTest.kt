package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReleaseNotesFormatterTest {
    @Test
    fun escapesMarkupSoNotesCannotInjectHtml() {
        assertThat(ReleaseNotesFormatter.toHtml("""<script>alert('x')</script> & "q"""", "無更新"))
            .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;q&quot;")
    }

    @Test
    fun convertsAllLineEndingsToBreaks() {
        assertThat(ReleaseNotesFormatter.toHtml("- 一\r\n- 二\n- 三", "無更新")).isEqualTo("- 一<br/>- 二<br/>- 三")
    }

    @Test
    fun blankNotesFallBackToEscapedPlaceholder() {
        assertThat(ReleaseNotesFormatter.toHtml(null, "無更新")).isEqualTo("無更新")
        assertThat(ReleaseNotesFormatter.toHtml("  \r\n ", "<無>")).isEqualTo("&lt;無&gt;")
    }

    @Test
    fun clipsVeryLongNotes() {
        val html = ReleaseNotesFormatter.toHtml("x".repeat(ReleaseNotesFormatter.MAX_CHARS + 1000), "無更新")
        assertThat(html).hasLength(ReleaseNotesFormatter.MAX_CHARS + 1)
        assertThat(html).endsWith("…")
    }
}
