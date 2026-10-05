package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.ui.player.CapsuleForm
import org.cog.hymnchtv.ui.player.SheetDisplay

/** What the player layer shows now, as the bottom capsule needs it (see PlayerSheetController.pillAnchor). */
data class PillAnchor(val display: SheetDisplay, val capsuleWidthPx: Int, val form: CapsuleForm)

/** Margins of the bottom capsule inside the lyrics page, in pixels, and whether the mode item keeps its text. */
data class PillPlacement(val startMargin: Int, val endMargin: Int, val bottomMargin: Int, val showModeText: Boolean)

/**
 * Places the bottom toolbar capsule (spec rev 3 section 3) in lyrics page coordinates: the page sits 5dp inside the screen
 * at the sides and reaches the screen bottom; the player capsule is 56dp tall, 16dp from the screen's end and bottom.
 */
object ChromePillLayout {
    private const val PAGE_SIDE_DP = 5
    private const val START_DP = 10 - PAGE_SIDE_DP
    private const val END_DP = 10 - PAGE_SIDE_DP
    private const val PLAYER_CAPSULE_HEIGHT_DP = 56
    private const val PLAYER_CAPSULE_MARGIN_DP = 16
    const val PILL_HEIGHT_DP = 44
    private const val GAP_DP = 8
    private const val HIDDEN_BOTTOM_DP = 12

    @JvmStatic
    fun place(anchor: PillAnchor, reservePx: Int, systemBottomPx: Int, density: Float): PillPlacement {
        fun px(dp: Int) = (dp * density + 0.5f).toInt()
        val system = systemBottomPx.coerceAtLeast(0)
        val start = px(START_DP)
        return when (anchor.display) {
            SheetDisplay.CAPSULE -> PillPlacement(
                startMargin = start,
                endMargin = anchor.capsuleWidthPx.coerceAtLeast(0) + px(PLAYER_CAPSULE_MARGIN_DP - PAGE_SIDE_DP + GAP_DP),
                bottomMargin = system + px(PLAYER_CAPSULE_MARGIN_DP + (PLAYER_CAPSULE_HEIGHT_DP - PILL_HEIGHT_DP) / 2),
                showModeText = anchor.form != CapsuleForm.PLAYBACK,
            )
            SheetDisplay.CARD -> PillPlacement(start, px(END_DP), reservePx.coerceAtLeast(0) + system + px(GAP_DP), true)
            SheetDisplay.HIDDEN -> PillPlacement(start, px(END_DP), system + px(HIDDEN_BOTTOM_DP), true)
        }
    }
}
