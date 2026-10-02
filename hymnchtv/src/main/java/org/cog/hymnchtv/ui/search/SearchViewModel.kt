package org.cog.hymnchtv.ui.search

import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import androidx.lifecycle.ViewModel
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.search.AssetLyricsSource
import org.cog.hymnchtv.search.HymnSearch
import org.cog.hymnchtv.search.SearchPage
import org.cog.hymnchtv.search.SearchScope
import org.cog.hymnchtv.search.T2sMap
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** What the search page shows. */
sealed interface SearchUiState {
    /** Nothing typed yet. */
    object Idle : SearchUiState

    object Loading : SearchUiState

    data class Done(val page: SearchPage) : SearchUiState
}

/**
 * Query, scope and result of the search page. It lives across rotation and across opening a hymn and coming back.
 * The search reads about two thousand lyrics files, so it runs on its own thread: the shared IO thread
 * ([AppExecutors]) is a single one and must stay free for history writes and title lookups.
 */
class SearchViewModel : ViewModel() {
    var query: String = ""
        private set

    var scope: SearchScope = SearchScope.All
        private set

    /** The book to offer as "current source"; null (English source) means only "all books". */
    var currentBook: String? = null
        private set

    var state: SearchUiState = SearchUiState.Idle
        private set

    /** Set by the fragment while its view exists; called on the main thread. */
    var onChanged: (() -> Unit)? = null

    private var initialized = false
    private var requestId = 0
    private var t2s: T2sMap? = null

    /** Applies the arguments once; later calls (rotation) keep the live scope. */
    fun init(book: String?) {
        if (initialized) return
        initialized = true
        currentBook = book
        scope = if (book != null) SearchScope.Book(book) else SearchScope.All
    }

    fun setScope(next: SearchScope, context: Context, prefs: SharedPreferences) {
        if (next == scope) return
        scope = next
        search(query, context, prefs)
    }

    /** Starts (or restarts) a search; a blank query clears the result. Main thread. */
    fun search(text: String, context: Context, prefs: SharedPreferences) {
        query = text
        val id = ++requestId
        if (text.isBlank()) {
            publish(SearchUiState.Idle)
            return
        }
        publish(SearchUiState.Loading)
        val appContext = context.applicationContext
        val usedScope = scope
        val source = AssetLyricsSource.forPrefs(appContext, prefs)
        executor.execute {
            val table = t2s ?: AssetLyricsSource.loadT2s(appContext).also { t2s = it }
            val page = try {
                HymnSearch(source, table).search(text, usedScope, isCancelled = { id != requestId })
            } catch (e: RuntimeException) {
                Timber.e(e, "Search failed for '%s'", text)
                SearchPage(emptyList(), false)
            }
            AppExecutors.MAIN.post { if (id == requestId) publish(SearchUiState.Done(page)) }
        }
    }

    private fun publish(next: SearchUiState) {
        state = next
        onChanged?.invoke()
    }

    override fun onCleared() {
        requestId++
        onChanged = null
    }

    private companion object {
        val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
            Thread({
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            }, "hymn-search").apply { isDaemon = true }
        }
    }
}
