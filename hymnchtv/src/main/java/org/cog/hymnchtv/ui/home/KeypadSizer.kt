package org.cog.hymnchtv.ui.home

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.HymnPickerViews

/**
 * Fits the home page to the screen (spec 2, 9): everything except the keypad and the recent list is measured as it is,
 * then [HomeFit] decides the key height (48dp to 72dp) and how many recent rows (0 to 5) the rest allows. If even 48dp
 * keys do not fit, or in landscape, the page may scroll instead of cutting a control off. The result never depends on the
 * keypad's or the recent list's current height, so it settles in one pass.
 */
class KeypadSizer(
    private val views: HymnPickerViews,
    private val viewport: HomeScrollView,
    private val recent: RecentChips,
) {
    private val rows: List<View> = listOf(R.id.key_row0, R.id.key_row1, R.id.key_row2, R.id.key_row3).map { views.root.findViewById(it) }
    private val column: ViewGroup? = views.keypadArea.parent as? ViewGroup
    private val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }

    /** The last plan applied; null before the first layout. */
    var lastPlan: HomeFitPlan? = null
        private set

    fun attach() {
        viewport.addOnLayoutChangeListener(listener)
        column?.addOnLayoutChangeListener(listener)
        recent.onChanged = ::update
        update()
    }

    fun detach() {
        viewport.removeOnLayoutChangeListener(listener)
        column?.removeOnLayoutChangeListener(listener)
        recent.onChanged = null
    }

    fun update() {
        val column = column ?: return
        if (viewport.height == 0 || column.height == 0) return
        val density = views.root.resources.displayMetrics.density
        val inner = viewport.getChildAt(0)
        val chromePx = (inner?.paddingTop ?: 0) + (inner?.paddingBottom ?: 0)
        var fixedPx = column.height - views.keypadArea.height
        var headerPx = 0
        val area = views.recentArea
        if (area.parent === column) {
            fixedPx -= area.height
            headerPx = (area as? ViewGroup)?.getChildAt(0)?.height ?: 0
        }
        val portrait = views.root.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val plan = HomeFit.plan(
            HomeFit.Input(
                availableDp = (viewport.height - chromePx) / density,
                fixedDp = fixedPx / density,
                recentHeaderDp = headerPx / density,
                recentTotal = recent.total,
            ),
        )
        apply(plan, portrait, density)
    }

    private fun apply(plan: HomeFitPlan, portrait: Boolean, density: Float) {
        lastPlan = plan
        val heightPx = (plan.keyHeightDp * density).toInt()
        rows.forEach { row ->
            if (row.layoutParams.height != heightPx) {
                row.layoutParams = row.layoutParams.apply { height = heightPx }
            }
        }
        // Landscape has its own column for the recent list: it shows all it can and the page may scroll
        recent.fit(if (portrait) plan.recentCount else HomeFit.MAX_RECENT, plan.showEmpty || (!portrait && recent.total == 0))
        viewport.scrollingAllowed = plan.scrollable || !portrait
    }
}
