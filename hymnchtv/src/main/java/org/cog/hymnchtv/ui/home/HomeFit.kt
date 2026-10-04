package org.cog.hymnchtv.ui.home

import org.cog.hymnchtv.ui.picker.KeypadMetrics

/**
 * The one-page home (spec 2): decides, for the height the screen leaves, how many recent hymns are listed and how tall
 * the keys are. Order of priority: the fixed parts (search, books, preview, open) as measured, the keypad at its 48dp
 * minimum, the recent header (the "all history" entry is always visible), then the first [TARGET_RECENT] recent rows (spec
 * 5b: a phone must show at least three), then the keys grow up to [MAX_KEY_DP], and the remaining recent rows take what is left. When even the 48dp keys do not fit, the primary action (Open) still has to stay on screen, so the page gives up, in this
 * order: key height down to [MIN_KEY_FLOOR_DP] (recent rows already gone), then a compact preview and tighter gaps
 * ([HomeFitPlan.compact], worth [COMPACT_SAVING_DP]), then the recent header (it and the recent rows move below the fold and the page
 * scrolls, [HomeFitPlan.scrollable]), and only as the last resort the page scrolls with Open pinned to the viewport's
 * bottom ([HomeFitPlan.pinAction]). Landscape keeps the old behaviour (48dp keys, scrolling).
 */
object HomeFit {
    const val MAX_RECENT = 5

    /** Recent rows that are placed before the keys grow beyond their minimum. */
    const val TARGET_RECENT = 3

    /** Tallest a key grows on the home page; more height goes to the recent list. */
    const val MAX_KEY_DP = 64f

    /** One recent row: a 48dp item plus the 4dp gap under it. */
    const val RECENT_ROW_DP = 52f

    /** The empty-history hint (48dp icon, one sentence) with its spacing. */
    const val EMPTY_BLOCK_DP = 96f

    /** Left unused so rounding never pushes the last row out of the viewport. */
    const val SAFETY_DP = 2f

    /** Shortest a key gets when the screen is too short for 48dp keys (still a 40dp-tall, full-width touch target). */
    const val MIN_KEY_FLOOR_DP = 40f

    /** What [HomeFitPlan.compact] saves in portrait: preview padding 14 to 6dp (both sides) and four gaps of 10 to 6dp. */
    const val COMPACT_SAVING_DP = 32f

    val KEYPAD_MIN_DP: Float = keypadDp(KeypadMetrics.MIN_KEY_DP)

    /**
     * @property availableDp the viewport height minus the page's own vertical padding (system bars included)
     * @property fixedDp everything except the keypad and the recent area, margins included
     * @property recentHeaderDp the recent area's title row, which always stays
     * @property recentTotal how many history records there are
     * @property compactSavingDp what the compact layout saves; [fixedDp] is always measured as if it were not compact
     * @property portrait false keeps the old fallback (48dp keys, the page scrolls)
     */
    data class Input(
        val availableDp: Float,
        val fixedDp: Float,
        val recentHeaderDp: Float,
        val recentTotal: Int,
        val compactSavingDp: Float = 0f,
        val portrait: Boolean = true,
    )

    /** Height of a whole keypad (four rows and three gaps) with keys [keyDp] tall. */
    fun keypadDp(keyDp: Float): Float = KeypadMetrics.ROWS * keyDp + (KeypadMetrics.ROWS - 1) * KeypadMetrics.GAP_DP

    fun plan(input: Input): HomeFitPlan {
        val wanted = input.recentTotal.coerceIn(0, MAX_RECENT)
        val base = input.fixedDp + input.recentHeaderDp + KEYPAD_MIN_DP
        var left = input.availableDp - SAFETY_DP - base
        if (left < 0f) return squeeze(input, wanted)
        val firstRows = minOf(wanted, TARGET_RECENT, (left / RECENT_ROW_DP).toInt())
        left -= firstRows * RECENT_ROW_DP
        val extraPerKey = minOf(left / KeypadMetrics.ROWS, MAX_KEY_DP - KeypadMetrics.MIN_KEY_DP)
        val key = KeypadMetrics.MIN_KEY_DP + extraPerKey
        left -= extraPerKey * KeypadMetrics.ROWS
        val moreRows = minOf(wanted - firstRows, (left / RECENT_ROW_DP).toInt())
        val count = firstRows + moreRows
        left -= moreRows * RECENT_ROW_DP
        val showEmpty = input.recentTotal == 0 && left >= EMPTY_BLOCK_DP
        return HomeFitPlan(key, count, showEmpty, scrollable = false)
    }

    /** The keys at 48dp and the recent header do not both fit: give up the least important thing first. */
    private fun squeeze(input: Input, wanted: Int): HomeFitPlan {
        if (!input.portrait) {
            return HomeFitPlan(KeypadMetrics.MIN_KEY_DP, wanted, input.recentTotal == 0, scrollable = true)
        }
        val avail = input.availableDp - SAFETY_DP
        val floorPad = keypadDp(MIN_KEY_FLOOR_DP)
        for (compact in listOf(false, true)) {
            val room = avail + (if (compact) input.compactSavingDp else 0f) - input.fixedDp - input.recentHeaderDp - floorPad
            if (room >= 0f) return HomeFitPlan(keyFor(room), 0, false, scrollable = false, compact = compact)
        }
        val room = avail + input.compactSavingDp - input.fixedDp - floorPad
        if (room >= 0f) return HomeFitPlan(keyFor(room), wanted, input.recentTotal == 0, scrollable = true, compact = true)
        return HomeFitPlan(MIN_KEY_FLOOR_DP, wanted, input.recentTotal == 0, scrollable = true, compact = true, pinAction = true)
    }

    /** Key height when [spare] dp is left over with the keys at the floor, never above the normal 48dp minimum. */
    private fun keyFor(spare: Float): Float = minOf(KeypadMetrics.MIN_KEY_DP, MIN_KEY_FLOOR_DP + spare / KeypadMetrics.ROWS)
}

/**
 * Result of [HomeFit.plan]; [recentCount] rows are listed, [showEmpty] shows the empty-history hint instead. [compact]
 * tightens the preview and the gaps, [pinAction] keeps the Open row at the viewport's bottom while the page scrolls.
 */
data class HomeFitPlan(
    val keyHeightDp: Float,
    val recentCount: Int,
    val showEmpty: Boolean,
    val scrollable: Boolean,
    val compact: Boolean = false,
    val pinAction: Boolean = false,
)
