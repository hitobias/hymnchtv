package org.cog.hymnchtv.ui.host

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HomeFragment
import org.cog.hymnchtv.ui.myhymns.MyHymnsFragment
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment

/**
 * Hosts the four tabs of MainActivity. Tab fragments are created once and then hidden/shown, so every tab keeps its state
 * while another one is visible. The selected tab is saved by the activity and passed back to [attach].
 */
class MainHost(activity: FragmentActivity) {
    private val fm = activity.supportFragmentManager
    private val tabs: Map<Int, () -> Fragment> = linkedMapOf(
        R.id.nav_home to { HomeFragment() },
        R.id.nav_toc to { TocFragment() },
        R.id.nav_my_hymns to { MyHymnsFragment() },
        R.id.nav_settings to { SettingsFragment() },
    )
    private var nav: BottomNavigationView? = null
    private var current: Int = R.id.nav_home

    /** @param savedTab the tab saved by [onSaveState]; the activity reads it from its savedInstanceState, never from the intent. */
    fun attach(nav: BottomNavigationView, savedTab: Int) {
        this.nav = nav
        val tab = if (tabs.containsKey(savedTab)) savedTab else R.id.nav_home
        nav.selectedItemId = tab
        nav.setOnItemSelectedListener { item ->
            if (tabs.containsKey(item.itemId)) {
                show(item.itemId)
                true
            } else {
                false
            }
        }
        show(tab)
    }

    fun onSaveState(out: Bundle) {
        out.putInt(EXTRA_TAB, current)
    }

    /**
     * Back handling: the home tab closes its history list first; any other tab goes back to home.
     *
     * @return true when the press was consumed; false when the activity should finish
     */
    fun onBackPressed(): Boolean {
        if (current != R.id.nav_home) {
            nav?.selectedItemId = R.id.nav_home
            return true
        }
        return (fm.findFragmentByTag(tag(R.id.nav_home)) as? HomeFragment)?.onBackPressed() ?: false
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
        const val EXTRA_TAB = "c_selected_tab"
    }
}
