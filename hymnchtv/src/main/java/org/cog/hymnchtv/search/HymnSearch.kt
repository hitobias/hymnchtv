package org.cog.hymnchtv.search

import org.cog.hymnchtv.hymn.HymnNumberRules
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.titles.AssetHymnTitles

sealed interface SearchScope {
    object All : SearchScope

    data class Book(val book: String) : SearchScope
}

/** One hit. The shown number (and 附) is always derived from [ref]; the book is never lost when two books share a number. */
data class SearchResult(val ref: HymnRef, val title: String, val snippet: String)

data class SearchPage(val results: List<SearchResult>, val hasMore: Boolean)

fun interface LyricsSource {
    /** Simplified lyrics text (first line is the hymn number); null when the hymn has no file. Line breaks are \n or \r\n. */
    fun simplified(ref: HymnRef): String?

    /** The Traditional text for display when the user reads lyrics in Traditional; null otherwise. Never used for matching. */
    fun traditional(ref: HymnRef): String? = null
}

/** Full-text search over the bundled lyrics (title and body), built on [SearchPattern] and [T2sMap]. */
class HymnSearch(private val lyrics: LyricsSource, private val t2s: T2sMap = T2sMap.EMPTY) {

    /**
     * @param limit results returned at most; [SearchPage.hasMore] is true when there were more (probed with limit+1)
     * @param isCancelled polled before each file; when true the search stops and returns what it has
     */
    fun search(query: String, scope: SearchScope, limit: Int = DEFAULT_LIMIT, isCancelled: () -> Boolean = { false }): SearchPage {
        require(limit >= 1) { "limit must be >= 1" }
        val pattern = SearchPattern.build(query, t2s) ?: return SearchPage(emptyList(), false)
        val out = ArrayList<SearchResult>()
        for (book in booksOf(scope)) for (no in HymnNumberRules.storedNumbers(book)) {
            if (isCancelled()) return SearchPage(out.take(limit), false)
            val ref = HymnRef(book, no)
            val simplified = lyrics.simplified(ref) ?: continue
            val hit = SnippetExtractor.find(simplified, pattern) ?: continue
            out += toResult(ref, simplified, hit)
            if (out.size > limit) return SearchPage(out.subList(0, limit).toList(), true)
        }
        return SearchPage(out.toList(), false)
    }

    private fun booksOf(scope: SearchScope): List<String> = when (scope) {
        SearchScope.All -> BOOK_ORDER
        is SearchScope.Book -> if (scope.book in BOOK_ORDER) listOf(scope.book) else emptyList()
    }

    private fun toResult(ref: HymnRef, simplified: String, hit: SnippetHit): SearchResult {
        val simplifiedLines = SnippetExtractor.lines(simplified)
        val traditional = lyrics.traditional(ref)?.let { SnippetExtractor.lines(it) }?.takeIf { it.size == simplifiedLines.size }
        val shownLines = traditional ?: simplifiedLines
        val title = AssetHymnTitles.titleOf(shownLines.joinToString("\n")).orEmpty()
        val snippet = if (traditional != null) SnippetExtractor.snippetFrom(traditional, hit.lineIndex) else hit.snippet
        return SearchResult(ref, title, snippet)
    }

    companion object {
        const val DEFAULT_LIMIT = 200

        /** The order the old search walked the books in. */
        val BOOK_ORDER = listOf(HymnTypes.DB, HymnTypes.BB, HymnTypes.XB, HymnTypes.XG, HymnTypes.YB, HymnTypes.ER)
    }
}
