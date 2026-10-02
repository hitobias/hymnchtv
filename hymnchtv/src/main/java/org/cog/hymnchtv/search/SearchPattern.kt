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

    /** @return null when the query is blank; never throws PatternSyntaxException. */
    @JvmStatic
    fun build(query: String?): Pattern? {
        val q = query?.trim().orEmpty()
        if (q.isEmpty()) return null
        // Quote each segment separately; quoting the whole string first would also quote HE_OR_HIM.
        val regex = q.split(HE).joinToString(HE_OR_HIM) { if (it.isEmpty()) "" else Pattern.quote(it) }
        return Pattern.compile(regex)
    }
}
