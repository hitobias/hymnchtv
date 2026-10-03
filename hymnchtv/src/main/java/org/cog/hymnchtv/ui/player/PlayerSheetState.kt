package org.cog.hymnchtv.ui.player

/** What the player layer shows: nothing, the full card, or the bottom-right capsule. */
enum class SheetDisplay { HIDDEN, CARD, CAPSULE }

/** The capsule's content: the play/pause key with a progress ring, or a bare note button when nothing is playing. */
enum class CapsuleForm { NOTE, PLAYBACK }

/**
 * Pure state of the collapsible player (spec section 4). Immutable: every change returns a new value.
 *
 * [userHidden] is the overflow-menu "hide the player bar" switch; [collapsed] is the portrait card or capsule choice;
 * [landscapeExpanded] is the temporary expansion in landscape, which never touches [collapsed].
 */
data class PlayerSheetState(
    val userHidden: Boolean = false,
    val collapsed: Boolean = false,
    val landscapeExpanded: Boolean = false,
) {
    /** What to show. A playing video owns the layer, so the card and the capsule give way to it. */
    fun display(portrait: Boolean, videoActive: Boolean = false): SheetDisplay = when {
        userHidden || videoActive -> SheetDisplay.HIDDEN
        portrait -> if (collapsed) SheetDisplay.CAPSULE else SheetDisplay.CARD
        else -> if (landscapeExpanded) SheetDisplay.CARD else SheetDisplay.CAPSULE
    }

    fun collapse(portrait: Boolean): PlayerSheetState =
        if (portrait) copy(collapsed = true) else copy(landscapeExpanded = false)

    fun expand(portrait: Boolean): PlayerSheetState =
        if (portrait) copy(collapsed = false) else copy(landscapeExpanded = true)

    fun toggleUserHidden(): PlayerSheetState = copy(userHidden = !userHidden)

    /** Turning back to portrait drops the temporary landscape expansion. */
    fun onOrientationChanged(portrait: Boolean): PlayerSheetState =
        if (portrait && landscapeExpanded) copy(landscapeExpanded = false) else this

    companion object {
        const val SNAP_DISTANCE_FRACTION = 0.4f
        const val SNAP_VELOCITY_DP_PER_SEC = 1000f
        const val EXPAND_DISTANCE_DP = 40f

        /** Release of a downward drag on the card: collapse past 40% of the card height or faster than 1000dp/s. */
        @JvmStatic
        fun shouldCollapse(dragPx: Float, cardHeightPx: Float, velocityPxPerSec: Float, density: Float): Boolean =
            dragPx > cardHeightPx * SNAP_DISTANCE_FRACTION || velocityPxPerSec > SNAP_VELOCITY_DP_PER_SEC * density

        /** Release of an upward drag on the capsule (the distance is positive upwards). */
        @JvmStatic
        fun shouldExpand(upPx: Float, velocityUpPxPerSec: Float, density: Float): Boolean =
            upPx > EXPAND_DISTANCE_DP * density || velocityUpPxPerSec > SNAP_VELOCITY_DP_PER_SEC * density

        @JvmStatic
        fun capsuleForm(state: PlaybackUiState): CapsuleForm = if (state.active) CapsuleForm.PLAYBACK else CapsuleForm.NOTE

        const val CAPSULE_HEIGHT_DP = 56
        const val CAPSULE_BOTTOM_MARGIN_DP = 16

        /** What the lyrics keep clear for the player layer, in pixels (the "playerReserve" of spec section 4). */
        @JvmStatic
        fun reserve(display: SheetDisplay, cardHeightPx: Int, density: Float): Int = when (display) {
            SheetDisplay.HIDDEN -> 0
            SheetDisplay.CARD -> cardHeightPx.coerceAtLeast(0)
            SheetDisplay.CAPSULE -> ((CAPSULE_HEIGHT_DP + CAPSULE_BOTTOM_MARGIN_DP) * density + 0.5f).toInt()
        }
    }
}
