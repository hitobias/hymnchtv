package org.cog.hymnchtv.ui.host

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HistoryFragment
import org.cog.hymnchtv.ui.home.HomeFragment
import org.cog.hymnchtv.ui.myhymns.MyHymnsFragment
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment

/**
 * Hosts the tabs (home, toc, settings; plus my hymns when [UiFlags.NOTEBOOK_UI_ENABLED]) of MainActivity. Tab fragments are created once and then hidden/shown, so every tab keeps its state
 * while another one is visible. The selected tab is saved by the activity and passed back to [attach].
 */
class MainHost(private val activity: AppCompatActivity) {
    private val fm = activity.supportFragmentManager
    private val tabs: Map<Int, () -> Fragment> = tabIds(UiFlags.NOTEBOOK_UI_ENABLED).associateWith(::createTab)

    private fun createTab(tabId: Int): () -> Fragment = when (tabId) {
        R.id.nav_home -> ({ HomeFragment() })
        R.id.nav_toc -> ({ newToc() })
        R.id.nav_my_hymns -> ({ MyHymnsFragment() })
        else -> ({ SettingsFragment() })
    }

    private fun newToc(): TocFragment {
        val requested = pendingToc
        pendingToc = null
        return if (requested == null) TocFragment() else TocFragment().apply { arguments = TocFragment.args(requested.first, requested.second) }
    }
    private var nav: BottomNavigationView? = null
    private var current: Int = R.id.nav_home

    /** The contents tab's preselection requested before that tab exists. */
    private var pendingToc: Pair<String, String>? = null

    /** @param savedTab the tab saved by [onSaveState]; the activity reads it from its savedInstanceState, never from the intent. */
    fun attach(nav: BottomNavigationView, savedTab: Int) {
        this.nav = nav
        nav.menu.findItem(R.id.nav_my_hymns)?.isVisible = UiFlags.NOTEBOOK_UI_ENABLED
        val tab = if (tabs.containsKey(savedTab)) savedTab else R.id.nav_home
        nav.selectedItemId = tab
        nav.setOnItemSelectedListener { item ->
            if (tabs.containsKey(item.itemId)) {
                // Any tab chosen from the bottom navigation closes the pages shown over the tabs
                popOverlays()
                show(item.itemId)
                true
            } else {
                false
            }
        }
        show(tab)
        // The back stack is restored with the activity; its container must be visible again
        fm.addOnBackStackChangedListener { updateOverlay() }
        updateOverlay()
    }

    fun onSaveState(out: Bundle) {
        out.putInt(EXTRA_TAB, current)
    }

    /**
     * Back handling: a page shown over the tabs closes first; any other tab than home goes back to home.
     *
     * @return true when the press was consumed; false when the activity should finish
     */
    fun onBackPressed(): Boolean {
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            return true
        }
        if (current != R.id.nav_home) {
            nav?.selectedItemId = R.id.nav_home
            return true
        }
        return false
    }

    /** Selects the contents tab with [book] and the index kind [page] preselected. */
    fun openToc(book: String, page: String) {
        val existing = fm.findFragmentByTag(tag(R.id.nav_toc)) as? TocFragment
        if (existing != null) existing.select(book, page) else pendingToc = book to page
        popOverlays()
        nav?.selectedItemId = R.id.nav_toc
    }

    fun openHistory() = showOverlay(HistoryFragment(), TAG_HISTORY)

    /** Shows [fragment] full-screen over the tabs; the back key closes it. */
    fun showOverlay(fragment: Fragment, tag: String) {
        fm.beginTransaction().setReorderingAllowed(true)
            .add(R.id.overlay_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    fun popOverlays() {
        if (fm.backStackEntryCount > 0) fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun updateOverlay() {
        val open = fm.backStackEntryCount > 0
        activity.findViewById<View>(R.id.overlay_container)?.visibility = if (open) View.VISIBLE else View.GONE
        if (!open) activity.supportActionBar?.setTitle(R.string.app_title_main)
    }

    private fun show(tabId: Int) {
        val shown = fm.findFragmentByTag(tag(tabId))
        if (tabId == current && shown != null && !shown.isHidden) return
        val tx = fm.beginTransaction().setReorderingAllowed(true)
        if (shown != null) tx.show(shown) else tx.add(R.id.fragment_container, tabs.getValue(tabId)(), tag(tabId))
        if (tabId != current) fm.findFragmentByTag(tag(current))?.let { tx.hide(it) }
        tx.commit()
        current = tabId
    }

    private fun tag(tabId: Int) = "tab:$tabId"

    companion object {
        /** The tabs shown, in bottom-navigation order. */
        fun tabIds(notebookEnabled: Boolean): List<Int> = buildList {
            add(R.id.nav_home)
            add(R.id.nav_toc)
            if (notebookEnabled) add(R.id.nav_my_hymns)
            add(R.id.nav_settings)
        }

        private const val TAG_HISTORY = "history"

        const val EXTRA_TAB = "c_selected_tab"
    }
}
