package org.cog.hymnchtv.ui.home

import org.cog.hymnchtv.ui.picker.KeypadMetrics

/**
 * The one-page home (spec 2): decides, for the height the screen leaves, how many recent hymns are listed and how tall
 * the keys are. Order of priority: the fixed parts (search, books, preview, open) as measured, the keypad at its 48dp
 * minimum, the recent header (the "all history" entry is always visible), then the keys grow up to 72dp, and recent rows
 * take what is left. When even the 48dp keys do not fit, [HomeFitPlan.scrollable] is true and the page scrolls.
 */
object HomeFit {
    const val MAX_RECENT = 5

    /** One recent row: a 48dp item plus the 4dp gap under it. */
    const val RECENT_ROW_DP = 52f

    /** The empty-history hint (48dp icon, one sentence) with its spacing. */
    const val EMPTY_BLOCK_DP = 96f

    /** Left unused so rounding never pushes the last row out of the viewport. */
    const val SAFETY_DP = 2f

    val KEYPAD_MIN_DP: Float = keypadDp(KeypadMetrics.MIN_KEY_DP)

    /**
     * @property availableDp the viewport height minus the page's own vertical padding (system bars included)
     * @property fixedDp everything except the keypad and the recent area, margins included
     * @property recentHeaderDp the recent area's title row, which always stays
     * @property recentTotal how many history records there are
     */
    data class Input(val availableDp: Float, val fixedDp: Float, val recentHeaderDp: Float, val recentTotal: Int)

    /** Height of a whole keypad (four rows and three gaps) with keys [keyDp] tall. */
    fun keypadDp(keyDp: Float): Float = KeypadMetrics.ROWS * keyDp + (KeypadMetrics.ROWS - 1) * KeypadMetrics.GAP_DP

    fun plan(input: Input): HomeFitPlan {
        val wanted = input.recentTotal.coerceIn(0, MAX_RECENT)
        val base = input.fixedDp + input.recentHeaderDp + KEYPAD_MIN_DP
        var left = input.availableDp - SAFETY_DP - base
        if (left < 0f) {
            return HomeFitPlan(KeypadMetrics.MIN_KEY_DP, wanted, input.recentTotal == 0, scrollable = true)
        }
        // Keys first (up to 72dp), then recent rows from what is left (spec 9)
        val extraPerKey = minOf(left / KeypadMetrics.ROWS, KeypadMetrics.MAX_KEY_DP - KeypadMetrics.MIN_KEY_DP)
        val key = KeypadMetrics.MIN_KEY_DP + extraPerKey
        left -= extraPerKey * KeypadMetrics.ROWS
        val count = minOf(wanted, (left / RECENT_ROW_DP).toInt())
        left -= count * RECENT_ROW_DP
        val showEmpty = input.recentTotal == 0 && left >= EMPTY_BLOCK_DP
        return HomeFitPlan(key, count, showEmpty, scrollable = false)
    }
}

/** Result of [HomeFit.plan]; [recentCount] rows are listed, [showEmpty] shows the empty-history hint instead. */
data class HomeFitPlan(val keyHeightDp: Float, val recentCount: Int, val showEmpty: Boolean, val scrollable: Boolean)
