package org.cog.hymnchtv.ui.toc

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ExpandableListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import org.cog.hymnchtv.HymnToc
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.ui.home.HomePrefs

/** TOC tab: pick a hymn book and a kind of index, browse the tree, tap a hymn to open its lyrics. */
class TocFragment : Fragment(R.layout.fragment_toc) {
    private var hymnType: String = MainActivity.HYMN_DB
    private var tocPage: String = HymnToc.TOC_CATEGORY

    private var views: Views? = null
    private var loadRequest = 0

    /** Finished tables by (book, index kind); a table never changes while the app runs. */
    private val cache = HashMap<Pair<String, String>, Map<String, List<String>>>()

    private class Views(root: View) {
        val books: ChipGroup = root.findViewById(R.id.toc_books)
        val pages: TabLayout = root.findViewById(R.id.toc_pages)
        val list: ExpandableListView = root.findViewById(R.id.hymnToc)
        val empty: TextView = root.findViewById(R.id.toc_empty)
        val progress: ProgressBar = root.findViewById(R.id.toc_progress)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
        // Start on the book last used on the home tab unless this fragment restores its own state
        val remembered = runCatching { prefs.getString(HomePrefs.LAST_HYMN_TYPE, null) }.getOrNull()
        // Own saved state first, then what the opener asked for (the home tab's contents button), then the home tab's book
        hymnType = savedInstanceState?.getString(STATE_BOOK) ?: arguments?.getString(STATE_BOOK)?.takeIf { it in BOOK_CHIPS }
            ?: remembered?.takeIf { it in BOOK_CHIPS } ?: hymnType
        tocPage = savedInstanceState?.getString(STATE_PAGE) ?: arguments?.getString(STATE_PAGE)?.takeIf { page -> PAGES.any { it.second == page } }
            ?: tocPage
    }

    /** Preselects [book] and the index kind [page] (one of the values of [PAGES]); shows them at once when the view exists. */
    fun select(book: String, page: String) {
        require(book in BOOK_CHIPS) { "Unknown hymn book: $book" }
        val pageIndex = PAGES.indexOfFirst { it.second == page }
        require(pageIndex >= 0) { "Unknown index kind: $page" }
        // The fields are set first, so the chip and tab listeners below see "no change" and do not load twice
        hymnType = book
        tocPage = page
        val v = views ?: return
        v.books.check(BOOK_CHIPS.getValue(book))
        v.pages.getTabAt(pageIndex)?.select()
        load()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val v = Views(view)
        views = v

        v.books.check(BOOK_CHIPS.getValue(hymnType))
        v.books.setOnCheckedStateChangeListener { _, checkedIds ->
            val type = BOOK_CHIPS.entries.firstOrNull { it.value == checkedIds.firstOrNull() }?.key ?: return@setOnCheckedStateChangeListener
            if (type != hymnType) {
                hymnType = type
                load()
            }
        }

        PAGES.forEach { (labelRes, page) ->
            v.pages.addTab(v.pages.newTab().setText(labelRes).setTag(page), page == tocPage)
        }
        v.pages.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val page = tab.tag as? String ?: return
                if (page != tocPage) {
                    tocPage = page
                    load()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        v.list.setOnChildClickListener { parent, _, group, child, _ ->
            val adapter = parent.expandableListAdapter as? TocAdapter ?: return@setOnChildClickListener true
            val hymnNo = TocBuilder.hymnNoOf(adapter.getChild(group, child))
            if (hymnNo != null) MainActivity.showContent(requireContext(), hymnType, hymnNo, false)
            true
        }
        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_BOOK, hymnType)
        outState.putString(STATE_PAGE, tocPage)
    }

    override fun onDestroyView() {
        views = null
        super.onDestroyView()
    }

    private fun load() {
        val v = views ?: return
        val request = ++loadRequest
        val key = hymnType to tocPage

        // The English cross-reference does not exist for these books (the numbers are the same in both languages)
        if (tocPage == HymnToc.TOC_ENGLISH && (hymnType == MainActivity.HYMN_ER || hymnType == MainActivity.HYMN_XB)) {
            show(v, emptyMap(), R.string.en2ch_hymn_same)
            return
        }
        cache[key]?.let { show(v, it, R.string.c_toc_empty); return }

        v.progress.visibility = View.VISIBLE
        v.list.visibility = View.GONE
        v.empty.visibility = View.GONE
        val appContext = requireContext().applicationContext
        val (type, page) = key
        // Building the category tables reads every lyrics file of the book: keep it off the main thread
        AppExecutors.io("toc-build") {
            val toc = TocBuilder.build(appContext, type, page)
            AppExecutors.MAIN.post {
                cache[key] = toc
                // A newer selection supersedes this result (it is still cached for next time)
                val current = views
                if (request == loadRequest && current != null) show(current, toc, R.string.c_toc_empty)
            }
        }
    }

    private fun show(v: Views, toc: Map<String, List<String>>, emptyMessage: Int) {
        v.progress.visibility = View.GONE
        v.list.setAdapter(TocAdapter(toc))
        val isEmpty = toc.isEmpty()
        v.empty.setText(emptyMessage)
        v.empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        v.list.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    companion object {
        private const val STATE_BOOK = "toc_book"
        private const val STATE_PAGE = "toc_page"

        /** Arguments that open the tab on [book] and the index kind [page]. */
        fun args(book: String, page: String): Bundle = Bundle().apply {
            putString(STATE_BOOK, book)
            putString(STATE_PAGE, page)
        }

        private val BOOK_CHIPS = mapOf(
            MainActivity.HYMN_DB to R.id.toc_book_db,
            MainActivity.HYMN_BB to R.id.toc_book_bb,
            MainActivity.HYMN_XB to R.id.toc_book_xb,
            MainActivity.HYMN_XG to R.id.toc_book_xg,
            MainActivity.HYMN_YB to R.id.toc_book_yb,
            MainActivity.HYMN_ER to R.id.toc_book_er,
        )

        /** Tab label -> the index kind TocBuilder understands (the 目录 tab of the old spinner listed nothing). */
        private val PAGES = listOf(
            R.string.hymn_category to HymnToc.TOC_CATEGORY,
            R.string.hymn_stroke to HymnToc.TOC_STROKE,
            R.string.hymn_pinyin to HymnToc.TOC_PINYIN,
            R.string.hymn_eng2ch to HymnToc.TOC_ENGLISH,
        )
    }
}
