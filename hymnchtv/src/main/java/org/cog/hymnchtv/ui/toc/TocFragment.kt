package org.cog.hymnchtv.ui.toc

import android.content.Context
import android.os.Bundle
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.content.SharedPreferences
import com.google.android.material.chip.Chip
import android.widget.ExpandableListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import org.cog.hymnchtv.ui.toc.TocConstants
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.lyrics.HantVariant
import org.cog.hymnchtv.lyrics.LyricsScript
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.ui.home.HomePrefs
import org.cog.hymnchtv.ui.page.PageInsets
import org.cog.hymnchtv.ui.page.PagePalette
import org.cog.hymnchtv.ui.page.PageTitleBar
import org.cog.hymnchtv.ui.theme.SystemBars

/** Contents page (follows the home background, see [PagePalette]): pick a hymn book and a kind of index, browse the tree, tap a hymn to open its lyrics. */
class TocFragment : Fragment(R.layout.fragment_toc) {
    private var hymnType: String = MainActivity.HYMN_DB
    private var tocPage: String = TocConstants.TOC_CATEGORY

    private var views: Views? = null
    private var loadRequest = 0

    /** The script the list on screen was requested in; a change in the reading settings reloads it on return. */
    private var requestedVariant: HantVariant? = null

    /** Finished tables by (book, index kind); a table never changes while the app runs. */
    private val cache = HashMap<Triple<String, String, HantVariant?>, Map<String, List<String>>>()

    /** The colours on screen; rebuilt in onResume and when the MAIN background changes. Null before the view exists. */
    var palette: PagePalette? = null
        private set

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == BackgroundSlot.MAIN.prefKey) views?.page?.post { applyPalette() }
    }

    private fun settings(): SharedPreferences =
        requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private class Views(root: View) {
        val page: View = root.findViewById(R.id.toc_page)
        val titleBar: PageTitleBar = root.findViewById(R.id.toc_title_bar)
        val listFrame: View = root.findViewById(R.id.toc_list_frame)
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

        PageInsets.install(v.page)
        v.listFrame.clipToOutline = true
        applyPalette()

        v.list.setOnChildClickListener { parent, _, group, child, _ ->
            val adapter = parent.expandableListAdapter as? TocAdapter ?: return@setOnChildClickListener true
            val hymnNo = TocBuilder.hymnNoOf(adapter.getChild(group, child))
            if (hymnNo != null) MainActivity.showContent(requireContext(), hymnType, hymnNo, false)
            true
        }
        load()
    }

    override fun onStart() {
        super.onStart()
        settings().registerOnSharedPreferenceChangeListener(prefsListener)
        if (views != null && LyricsScript.hantVariant(requireContext()) != requestedVariant) load()
    }

    override fun onResume() {
        super.onResume()
        applyPalette()
    }

    override fun onStop() {
        settings().unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onStop()
    }

    /** Re-reads the home background and repaints the page, title bar, chips, tabs and rows. */
    fun applyPalette() {
        val v = views ?: return
        val pal = PagePalette.fromPrefs(settings())
        if (pal == palette) return
        palette = pal
        v.page.setBackgroundColor(pal.page)
        v.titleBar.apply(pal)
        paintChips(v, pal)
        v.pages.setBackgroundColor(pal.page)
        v.pages.setTabTextColors(pal.muted, pal.onCard)
        v.pages.setSelectedTabIndicatorColor(pal.accent)
        v.pages.tabRippleColor = ColorStateList.valueOf((pal.onCard and 0xFFFFFF) or RIPPLE_ALPHA)
        v.listFrame.background = GradientDrawable().apply {
            setColor(pal.card)
            cornerRadius = resources.getDimension(R.dimen.shape_radius_medium)
        }
        v.empty.setTextColor(pal.muted)
        v.progress.indeterminateTintList = ColorStateList.valueOf(pal.accent)
        (v.list.expandableListAdapter as? TocAdapter)?.let {
            it.palette = pal
            it.notifyDataSetChanged()
        }
        activity?.let { SystemBars.styleIcons(it, pal.isDark, SystemBars.legacyNavColor(pal.isDark, pal.page, pal.onCard)) }
    }

    private fun paintChips(v: Views, pal: PagePalette) {
        val checked = intArrayOf(android.R.attr.state_checked)
        val fill = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(pal.accent, pal.card))
        val text = ColorStateList(arrayOf(checked, intArrayOf()), intArrayOf(pal.onAccent, pal.onCard))
        for (i in 0 until v.books.childCount) {
            (v.books.getChildAt(i) as? Chip)?.apply {
                chipBackgroundColor = fill
                setTextColor(text)
                chipStrokeColor = ColorStateList.valueOf(pal.divider)
                chipStrokeWidth = resources.displayMetrics.density
                checkedIconTint = ColorStateList.valueOf(pal.onAccent)
                rippleColor = ColorStateList.valueOf((pal.onCard and 0xFFFFFF) or RIPPLE_ALPHA)
            }
        }
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
        val variant = LyricsScript.hantVariant(requireContext())
        requestedVariant = variant
        // A finished table is only reused in the script it was built for
        val key = Triple(hymnType, tocPage, variant)

        // The English cross-reference does not exist for these books (the numbers are the same in both languages)
        if (tocPage == TocConstants.TOC_ENGLISH && (hymnType == MainActivity.HYMN_ER || hymnType == MainActivity.HYMN_XB)) {
            show(v, emptyMap(), R.string.en2ch_hymn_same)
            return
        }
        cache[key]?.let { show(v, it, R.string.c_toc_empty); return }

        v.progress.visibility = View.VISIBLE
        v.list.visibility = View.GONE
        v.empty.visibility = View.GONE
        val appContext = requireContext().applicationContext
        val (type, page, _) = key
        // Building the category tables reads every lyrics file of the book: keep it off the main thread
        AppExecutors.io("toc-build") {
            val toc = TocBuilder.build(appContext, type, page, variant)
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
        v.list.setAdapter(TocAdapter(toc, palette))
        val isEmpty = toc.isEmpty()
        v.empty.setText(emptyMessage)
        v.empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        v.list.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    companion object {
        private const val RIPPLE_ALPHA = 0x1F shl 24
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
            R.string.hymn_category to TocConstants.TOC_CATEGORY,
            R.string.hymn_stroke to TocConstants.TOC_STROKE,
            R.string.hymn_pinyin to TocConstants.TOC_PINYIN,
            R.string.hymn_eng2ch to TocConstants.TOC_ENGLISH,
        )
    }
}
