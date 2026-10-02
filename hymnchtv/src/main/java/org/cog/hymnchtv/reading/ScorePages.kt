package org.cog.hymnchtv.reading

/** Score image names: the hymn itself, then the a–d suffixes for pages 2–5. */
object ScorePages {
    const val MAX_PAGES = 5
    private val SUFFIXES = listOf("", "a", "b", "c", "d")

    @JvmStatic
    fun fileNames(prefix: String, pages: Int): List<String> =
        SUFFIXES.take(pages.coerceIn(1, MAX_PAGES)).map { "$prefix$it.png" }
}
