package org.cog.hymnchtv.ui.lyrics

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

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

    /** Verse numbers ("一", "12", "第二节") and chorus labels ("（副）") that sit on a line of their own. */
    private val VERSE_MARKER = Regex(
        "^(?:[一二三四五六七八九十百零〇\\d]+|第[一二三四五六七八九十百零〇\\d]+[节節]|[（(]副[^）)\\n]{0,3}[）)])$",
    )

    /** The hymn number and the title come first and are never verse numbers, whatever they read like. */
    private const val TITLE_LINES = 2

    /** Character ranges of the verse-marker lines of [text] (the whole trimmed line, end inclusive). */
    @JvmStatic
    fun verseMarkerRanges(text: CharSequence): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var offset = 0
        text.split('\n').forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (index >= TITLE_LINES && VERSE_MARKER.matches(trimmed)) {
                val start = offset + line.indexOf(trimmed)
                ranges += start until start + trimmed.length
            }
            offset += line.length + 1
        }
        return ranges
    }

    /** [text] with every verse marker coloured [color] and bold; the text itself is unchanged. */
    @JvmStatic
    fun applyVerseSpans(text: CharSequence, color: Int): CharSequence {
        val spanned = SpannableString(text)
        verseMarkerRanges(text).forEach { r ->
            val end = r.last + 1
            spanned.setSpan(ForegroundColorSpan(color), r.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanned.setSpan(StyleSpan(Typeface.BOLD), r.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spanned
    }
}
