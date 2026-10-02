package org.cog.hymnchtv.ui.lyrics

/**
 * Key and time signature ("降A大调 4/4") that most lyrics files carry in their header, on the third or
 * fourth line; the other header lines are number, title, syllable counts and a blank.
 */
object LyricsMeta {
    /** Only the first lines are header; a verse that happens to read like a key must never match. */
    private const val HEADER_LINES = 5

    private val KEY_LINE = Regex(
        "^(?:\\d{3,5}[双雙]?)?([降升]?[A-Ga-gＡ-Ｇａ-ｇ][大小][调調])\\s*(\\d+\\s*/\\s*\\d+)?\\??$",
    )

    private fun match(line: String): String? {
        val m = KEY_LINE.matchEntire(line.trim()) ?: return null
        val meter = m.groupValues[2].replace(" ", "")
        return if (meter.isEmpty()) m.groupValues[1] else "${m.groupValues[1]} $meter"
    }

    /** The key and meter to show above the lyrics, or null when the file has none (e.g. most XB hymns). */
    @JvmStatic
    fun parseMeterKey(lines: List<String>): String? =
        lines.take(HEADER_LINES).firstNotNullOfOrNull { match(it) }

    /** [text] without its key/meter line (it is shown separately); unchanged when there is none. */
    @JvmStatic
    fun removeMeterLine(text: String): String {
        val lines = text.split('\n')
        val index = lines.take(HEADER_LINES).indexOfFirst { match(it) != null }
        if (index < 0) return text
        return lines.filterIndexed { i, _ -> i != index }.joinToString("\n")
    }
}
