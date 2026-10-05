package org.cog.hymnchtv.ui.playlist

/**
 * Plain text of a playlist for ACTION_SEND: the name, then one numbered line per hymn. It is built only from the labels and
 * titles passed in, so personal data (notes, sing times) never reaches it.
 */
object PlaylistShareText {
    fun line(headline: String, title: String?): String = if (title.isNullOrBlank()) headline else "$headline $title"

    fun format(name: String, lines: List<String>): String =
        (listOf(name) + lines.mapIndexed { index, line -> "${index + 1}. $line" }).joinToString("\n")
}
