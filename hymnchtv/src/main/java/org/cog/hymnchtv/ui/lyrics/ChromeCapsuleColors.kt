package org.cog.hymnchtv.ui.lyrics

import org.cog.hymnchtv.reading.background.UiTokens

/** Colours of the two lyrics toolbar capsules (spec rev 3 section 4). [fill] is translucent; draw it over the background. */
data class CapsuleColors(
    val fill: Int,
    val content: Int,
    val active: Int,
    val disabled: Int,
    val ripple: Int,
)

/**
 * Derives [CapsuleColors] from the reading background's [UiTokens]: `surfaceRaised` plate (the token alpha, 92% plain and
 * 88% photo), `onSurface` icons and text, `onOutlineAction` for the side in use (the accent nudged until it is AA on the
 * card; plain `accent` only promises 3:1), `disabledOnSurface` for an unusable item.
 */
object ChromeCapsuleColors {
    private const val RIPPLE_ALPHA = 0x29 // 0.16

    @JvmStatic
    fun from(tokens: UiTokens): CapsuleColors = CapsuleColors(
        fill = tokens.surfaceRaised,
        content = tokens.onSurface,
        active = tokens.onOutlineAction,
        disabled = tokens.disabledOnSurface,
        ripple = (tokens.onSurface and 0xFFFFFF) or (RIPPLE_ALPHA shl 24),
    )
}
