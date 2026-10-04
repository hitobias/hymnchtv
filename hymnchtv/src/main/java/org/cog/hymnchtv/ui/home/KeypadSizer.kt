package org.cog.hymnchtv.ui.home

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
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
    private val scrollListener = NestedScrollView.OnScrollChangeListener { _, _, _, _, _ -> pinAction() }
    private val gapViews: List<View> = listOf(R.id.booksArea, R.id.previewArea, R.id.keypadArea, R.id.actionArea).map { views.root.findViewById(it) }
    private var pinned = false

    /** The last plan applied; null before the first layout. */
    var lastPlan: HomeFitPlan? = null
        private set

    fun attach() {
        viewport.addOnLayoutChangeListener(listener)
        column?.addOnLayoutChangeListener(listener)
        viewport.setOnScrollChangeListener(scrollListener)
        recent.onChanged = ::update
        update()
    }

    fun detach() {
        viewport.removeOnLayoutChangeListener(listener)
        column?.removeOnLayoutChangeListener(listener)
        viewport.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
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
        // The fixed part is always measured as if the layout were not compact, so the plan never feeds back on itself
        if (compactApplied) fixedPx += (HomeFit.COMPACT_SAVING_DP * density).toInt()
        val plan = HomeFit.plan(
            HomeFit.Input(
                availableDp = (viewport.height - chromePx) / density,
                fixedDp = fixedPx / density,
                recentHeaderDp = headerPx / density,
                recentTotal = recent.total,
                compactSavingDp = HomeFit.COMPACT_SAVING_DP,
                portrait = portrait,
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
        setCompact(plan.compact, density)
        pinned = plan.pinAction
        views.actionArea.post { pinAction() }
    }

    private var compactApplied = false

    /** Tightens the gaps (10 to 6dp) and the preview's padding (14 to 6dp): [HomeFit.COMPACT_SAVING_DP] in all. */
    private fun setCompact(compact: Boolean, density: Float) {
        if (compact == compactApplied) return
        compactApplied = compact
        val gap = ((if (compact) COMPACT_GAP_DP else NORMAL_GAP_DP) * density).toInt()
        gapViews.forEach { v ->
            (v.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp -> lp.topMargin = gap; v.layoutParams = lp }
        }
        val pad = ((if (compact) COMPACT_PAD_DP else NORMAL_PAD_DP) * density).toInt()
        views.previewArea.setPadding(pad, pad, pad, pad)
    }

    /** Last resort: while the page scrolls, the Open row stays at the viewport's bottom edge (above the navigation bar). */
    private fun pinAction() {
        val action = views.actionArea
        val column = column
        val inner = viewport.getChildAt(0)
        if (!pinned || column == null || inner == null || action.height == 0) {
            if (action.translationY != 0f) action.translationY = 0f
            if (action.translationZ != 0f) action.translationZ = 0f
            return
        }
        val wanted = viewport.scrollY + viewport.height - inner.paddingBottom
        val natural = column.top + action.bottom
        action.translationY = minOf(0f, (wanted - natural).toFloat())
        action.translationZ = 1f
    }

    private companion object {
        const val NORMAL_GAP_DP = 10f
        const val COMPACT_GAP_DP = 6f
        const val NORMAL_PAD_DP = 14f
        const val COMPACT_PAD_DP = 6f
    }
}
