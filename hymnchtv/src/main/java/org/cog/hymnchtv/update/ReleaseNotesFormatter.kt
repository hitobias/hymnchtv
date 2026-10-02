package org.cog.hymnchtv.update

import org.cog.hymnchtv.utils.HtmlText

/** Release notes are untrusted text: escape them and keep line breaks for the WebView dialog. */
object ReleaseNotesFormatter {
    const val MAX_CHARS = 4000

    @JvmStatic
    fun toHtml(notes: String?, emptyText: String): String {
        val text = notes?.replace("\r\n", "\n")?.trim().orEmpty()
        if (text.isEmpty()) return HtmlText.escape(emptyText)
        val clipped = if (text.length > MAX_CHARS) text.take(MAX_CHARS) + "…" else text
        return clipped.split('\n').joinToString("<br/>") { HtmlText.escape(it) }
    }
}
