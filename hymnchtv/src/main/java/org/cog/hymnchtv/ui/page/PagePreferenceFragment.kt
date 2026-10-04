package org.cog.hymnchtv.ui.page

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.ui.theme.SystemBars

/**
 * A preference page that follows the home background (spec 5a): title bar with the back arrow, page colour, card rows.
 * The palette is rebuilt in onResume and whenever the MAIN background preference changes while the page is on screen.
 *
 * Host integration: the fragment is self-contained (it draws its own title bar and, when the window hands it the insets,
 * pads for the system bars). Show it with replace + addToBackStack; the back arrow calls the activity's back dispatch.
 */
abstract class PagePreferenceFragment : PreferenceFragmentCompat() {
    /** Title shown in the page's title bar. */
    @get:StringRes
    protected abstract val pageTitleRes: Int

    var titleBar: PageTitleBar? = null
        private set

    /** The colours currently painted; null before the view exists. */
    var palette: PagePalette? = null
        private set

    private var page: LinearLayout? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == BackgroundSlot.MAIN.prefKey) view?.post { refreshPalette() }
    }

    private fun settings(): SharedPreferences =
        requireContext().getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val list = super.onCreateView(inflater, container, savedInstanceState)
        val bar = PageTitleBar(inflater.context).apply { setTitle(getString(pageTitleRes)) }
        titleBar = bar
        val column = LinearLayout(inflater.context).apply {
            orientation = LinearLayout.VERTICAL
            addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        page = column
        return column
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(null)
        listView.apply {
            clipToPadding = false
            setPadding(paddingLeft, paddingTop, paddingRight, (LIST_BOTTOM_DP * resources.displayMetrics.density).toInt())
            addItemDecoration(PageCardDecoration())
            titleBar?.trackScroll(this)
        }
        PageInsets.install(view)
        refreshPalette()
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> =
        PagePreferenceAdapter(preferenceScreen) { checkNotNull(palette) { "palette is read before the view exists" } }

    override fun onStart() {
        super.onStart()
        settings().registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onResume() {
        super.onResume()
        refreshPalette()
    }

    override fun onStop() {
        settings().unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onStop()
    }

    override fun onDestroyView() {
        titleBar = null
        page = null
        super.onDestroyView()
    }

    /** Re-reads the home background and repaints the page, the title bar, the system bar icons and every row. */
    fun refreshPalette() {
        val column = page ?: return
        val fresh = PagePalette.fromPrefs(settings())
        if (fresh == palette) return
        palette = fresh
        column.setBackgroundColor(fresh.page)
        titleBar?.apply(fresh)
        activity?.let { SystemBars.styleIcons(it, fresh.isDark, SystemBars.legacyNavColor(fresh.isDark, fresh.page, fresh.onCard)) }
        listView.adapter?.notifyDataSetChanged()
    }

    private companion object {
        const val LIST_BOTTOM_DP = 24
    }
}
