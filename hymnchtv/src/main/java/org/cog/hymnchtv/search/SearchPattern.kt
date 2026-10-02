package org.cog.hymnchtv.search

import java.util.regex.Pattern

/**
 * Builds a literal search pattern from user input. Only "他" is widened to also match "祂";
 * every other character, including regex meta characters, is matched literally.
 * Matching is case-sensitive (same as the previous ContentSearch behavior).
 */
object SearchPattern {
    private const val HE = "他"
    private const val HE_OR_HIM = "[祂他]"

    /**
     * @param t2s Traditional->Simplified candidates; each mapped code point matches any of its candidates
     * @return null when the query is blank; never throws PatternSyntaxException.
     */
    @JvmStatic
    @JvmOverloads
    fun build(query: String?, t2s: T2sMap = T2sMap.EMPTY): Pattern? {
        val q = query?.trim().orEmpty()
        if (q.isEmpty()) return null
        val regex = StringBuilder()
        var i = 0
        while (i < q.length) {
            val cp = q.codePointAt(i)
            val ch = String(Character.toChars(cp))
            i += Character.charCount(cp)
            val candidates = t2s.candidates(ch)
            when {
                ch == HE -> regex.append(HE_OR_HIM)
                candidates != null -> regex.append(alternatives(candidates))
                else -> regex.append(Pattern.quote(ch))
            }
        }
        return Pattern.compile(regex.toString())
    }

    private fun alternatives(candidates: String): String {
        val parts = mutableListOf<String>()
        var i = 0
        while (i < candidates.length) {
            val cp = candidates.codePointAt(i)
            parts += Pattern.quote(String(Character.toChars(cp)))
            i += Character.charCount(cp)
        }
        return parts.joinToString("|", prefix = "(?:", postfix = ")")
    }
}
