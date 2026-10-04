package org.cog.hymnchtv.ui.host

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.snackbar.Snackbar
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HistoryFragment
import org.cog.hymnchtv.ui.home.HomeFragment
import org.cog.hymnchtv.ui.search.SearchFragment
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment

/**
 * Hosts the screens of MainActivity (spec 1, 9). The home page is the only top-level page and is added once. Contents and
 * settings replace it full-page on the same back stack; search and recent history are overlays above the page container.
 * Back order: overlay, full page, home. All screens come back from the FragmentManager after rotation or process death,
 * so nothing here is saved by the host itself.
 *
 * The top bar is the activity's toolbar: on the home page it holds the app name and the contents and settings buttons. Full
 * pages bring their own title bar and system-bar padding, so the toolbar is hidden while one is shown.
 */
class MainHost(private val activity: AppCompatActivity) {
    private val fm = activity.supportFragmentManager
    private val exitGuard = BackExitGuard()
    private var updateBadge = false

    /** Adds the home page (first start only) and wires the top bar; call once from onCreate. */
    fun attach(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            fm.beginTransaction().setReorderingAllowed(true)
                .add(R.id.fragment_container, HomeFragment(), TAG_HOME)
                .commit()
        }
        activity.findViewById<View>(R.id.btn_home_toc)?.setOnClickListener { openToc(null) }
        activity.findViewById<View>(R.id.btn_home_settings)?.setOnClickListener { openSettings() }
        fm.addOnBackStackChangedListener { updateChrome() }
        updateChrome()
    }

    /**
     * Back handling: the top page of the back stack closes first; on the bare home page the first press only asks for a
     * second one.
     *
     * @return true when the press was consumed; false when the activity should finish
     */
    fun onBackPressed(): Boolean {
        // A state-saved or still-executing transaction must neither pop twice nor count as the home page's first press
        if (fm.isStateSaved) return true
        try {
            fm.executePendingTransactions()
        } catch (e: IllegalStateException) {
            return true
        }
        if (fm.backStackEntryCount > 0) {
            exitGuard.reset()
            fm.popBackStack()
            return true
        }
        if (exitGuard.onBackOnHome(SystemClock.elapsedRealtime())) return false
        showExitHint()
        return true
    }

    /** Opens the contents page with [book] and index kind [page] preselected; null keeps the page's own default. */
    fun openToc(request: Pair<String, String>?) {
        val top = topFragment()
        if (top is TocFragment) {
            if (request != null) top.select(request.first, request.second)
            return
        }
        val fragment = TocFragment().apply { if (request != null) arguments = TocFragment.args(request.first, request.second) }
        showFullPage(fragment, TAG_TOC)
    }

    fun openToc(book: String, page: String) = openToc(book to page)

    fun openSettings() {
        if (topFragment() is SettingsFragment) return
        showFullPage(SettingsFragment(), TAG_SETTINGS)
    }

    fun openHistory() = showOverlay(HistoryFragment(), TAG_HISTORY)

    /** [book] is the book to search in first; null searches all books. */
    fun openSearch(book: String?) = showOverlay(SearchFragment.newInstance(book), TAG_SEARCH)

    /** Shows [fragment] full-screen over the home page; the back key closes it. */
    fun showOverlay(fragment: Fragment, tag: String) {
        if (fm.findFragmentByTag(tag) != null) return
        fm.beginTransaction().setReorderingAllowed(true)
            .add(R.id.overlay_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    /** Flags the settings button when an update was found (its entry "check for updates" lives there). */
    fun setUpdateAvailable(available: Boolean) {
        updateBadge = available
        updateButtons()
    }

    /** Closes every overlay and full page, leaving the bare home page. */
    fun popAll() {
        if (fm.backStackEntryCount > 0) fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun showFullPage(fragment: Fragment, tag: String) {
        // Overlays only open from home; whatever is open closes first so the page replaces the home page itself
        popAll()
        fm.beginTransaction().setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun topFragment(): Fragment? = fm.findFragmentById(R.id.fragment_container)

    private fun topName(): String? = fm.backStackEntryCount.takeIf { it > 0 }?.let { fm.getBackStackEntryAt(it - 1).name }

    private fun updateChrome() {
        val name = topName()
        val open = fm.backStackEntryCount > 0
        val fullPage = isFullPage(name)
        // A full page has its own title bar (PageTitleBar) and takes the whole screen; the toolbar is the home page's top bar
        activity.findViewById<View>(R.id.toolbar)?.visibility = if (fullPage) View.GONE else View.VISIBLE
        if (!open) activity.supportActionBar?.setTitle(R.string.app_title_main)
        activity.findViewById<View>(R.id.overlay_container)?.visibility = if (isOverlay(name)) View.VISIBLE else View.GONE
        // What is behind an overlay must not be reachable by TalkBack or the keyboard
        activity.findViewById<ViewGroup>(R.id.fragment_container)?.let { group ->
            val hidden = isOverlay(name)
            group.importantForAccessibility =
                if (hidden) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            group.descendantFocusability = if (hidden) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
        activity.findViewById<View>(R.id.viewMain)?.let { MainChrome.setFullPageShown(it, fullPage) }
        updateButtons()
        MainChrome.apply(activity, activity.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE))
    }

    private fun updateButtons() {
        val onFullPage = isFullPage(topName())
        val buttons = activity.findViewById<View>(R.id.home_top_buttons) ?: return
        buttons.visibility = if (onFullPage) View.GONE else View.VISIBLE
        activity.findViewById<View>(R.id.home_settings_badge)?.visibility = if (updateBadge) View.VISIBLE else View.GONE
        activity.findViewById<View>(R.id.btn_home_settings)?.contentDescription = activity.getString(
            if (updateBadge) R.string.c_settings_update_desc else R.string.c_nav_settings,
        )
    }

    private fun showExitHint() {
        val anchor = activity.findViewById<View>(R.id.viewMain) ?: return
        Snackbar.make(anchor, R.string.c_press_back_again, EXIT_HINT_MS).show()
    }

    companion object {
        const val TAG_HOME = "home"
        const val TAG_TOC = "toc"
        const val TAG_SETTINGS = "settings"
        const val TAG_HISTORY = "history"
        const val TAG_SEARCH = "search"

        /** How long the "press again" hint stays; the same window the second press has. */
        private const val EXIT_HINT_MS = BackExitGuard.WINDOW_MS.toInt()

        /** Pages that replace the home page (shown with a back arrow). */
        @JvmStatic
        fun isFullPage(name: String?): Boolean = name == TAG_TOC || name == TAG_SETTINGS

        /** Pages shown above the home page, in the overlay container. */
        @JvmStatic
        fun isOverlay(name: String?): Boolean = name == TAG_HISTORY || name == TAG_SEARCH
    }
}
