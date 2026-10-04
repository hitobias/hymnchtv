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

    /** Verse numbers ("一", "12", "第二节") that sit on a line of their own; a long digit run is a syllable count ("887887887"), not a number. */
    private val VERSE_NUMBER = Regex(
        "^(?:[一二三四五六七八九十百零〇]{1,4}|\\d{1,3}|第[一二三四五六七八九十百零〇\\d]{1,4}[节節])$",
    )

    /**
     * A chorus label on a line of its own: "（副）", "(副歌)", "（副3）" (up to three more characters), "副", "副歌", "副：".
     * Inline notes such as "（接上面副歌）" or "（第三节无副歌）" never match: the whole line must be the label.
     */
    private val CHORUS_MARKER = Regex("^(?:[（(]副歌?[^）)\\n]{0,3}[）)]|副歌?[:：]?)$")

    /** The hymn number and the title come first and are never verse numbers, whatever they read like. */
    private const val TITLE_LINES = 2

    private data class Line(val text: String, val start: Int) {
        val trimmed: String = text.trim()
        val range: IntRange get() = (start + text.indexOf(trimmed)).let { it until it + trimmed.length }
    }

    private fun lines(text: CharSequence): List<Line> {
        var offset = 0
        return text.split('\n').map { Line(it, offset).also { l -> offset += l.text.length + 1 } }
    }

    private fun isNumber(line: Line) = VERSE_NUMBER.matches(line.trimmed)

    private fun isChorus(line: Line) = CHORUS_MARKER.matches(line.trimmed)

    /** Character ranges of the verse-number lines of [text] (the whole trimmed line, end inclusive). */
    @JvmStatic
    fun verseNumberRanges(text: CharSequence): List<IntRange> =
        lines(text).filterIndexed { i, l -> i >= TITLE_LINES && isNumber(l) }.map { it.range }

    /** Character ranges of the verse-number and chorus-label lines of [text] (the whole trimmed line, end inclusive). */
    @JvmStatic
    fun verseMarkerRanges(text: CharSequence): List<IntRange> =
        lines(text).filterIndexed { i, l -> i >= TITLE_LINES && (isNumber(l) || isChorus(l)) }.map { it.range }

    /**
     * Character ranges of the chorus bodies: the lines after a chorus label up to the next verse number, chorus
     * label or blank line. A label followed by a blank line has no body.
     */
    @JvmStatic
    fun chorusBodyRanges(text: CharSequence): List<IntRange> {
        val all = lines(text)
        val ranges = mutableListOf<IntRange>()
        var i = TITLE_LINES
        while (i < all.size) {
            if (!isChorus(all[i])) {
                i++
                continue
            }
            var end = i + 1
            while (end < all.size && all[end].trimmed.isNotEmpty() && !isNumber(all[end]) && !isChorus(all[end])) end++
            if (end > i + 1) ranges += all[i + 1].start until (all[end - 1].start + all[end - 1].text.trimEnd().length)
            i = end
        }
        return ranges
    }

    /** Character ranges of the blank lines after the title lines (the single "\n" of an empty line is the range). */
    @JvmStatic
    fun blankLineRanges(text: CharSequence): List<IntRange> =
        lines(text).filterIndexed { i, l -> i >= TITLE_LINES && l.trimmed.isEmpty() && l.start < text.length }
            .map { it.start..it.start }
}
