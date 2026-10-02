package org.cog.hymnchtv.search

import java.util.regex.Pattern

/** [snippet] starts at the matching line ([lineIndex], 0 = the hymn-number line, which is never searched). */
data class SnippetHit(val lineIndex: Int, val snippet: String)

object SnippetExtractor {
    const val MAX_LENGTH = 64

    private val LINE_BREAK = Regex("\r\n|\n")

    fun lines(text: String): List<String> = text.split(LINE_BREAK)

    /** First line after the number line that matches [pattern]; null when none does. */
    fun find(text: String, pattern: Pattern): SnippetHit? {
        val lines = lines(text)
        for (i in 1 until lines.size) {
            if (pattern.matcher(lines[i]).find()) return SnippetHit(i, snippetFrom(lines, i))
        }
        return null
    }

    /** Lines from [lineIndex] on, blank ones dropped, joined by single spaces, at most [MAX_LENGTH] characters. */
    fun snippetFrom(lines: List<String>, lineIndex: Int): String {
        val sb = StringBuilder()
        for (i in lineIndex until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(line)
            if (sb.length >= MAX_LENGTH) break
        }
        return sb.take(MAX_LENGTH).toString()
    }
}
