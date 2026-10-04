package org.cog.hymnchtv.ui.page

import android.content.SharedPreferences
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.TokenInput
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag

/**
 * Solid colours of a full page (settings, contents; spec 5a) derived from the home background's [UiTokens]. Preferences and
 * list rows cannot draw translucent colours over a gradient, so every value is opaque: translucent tokens are flattened with
 * [UiTokens.over] onto [page].
 *
 * A dark preset's page is the background's base colour and its cards are the surface over it; a light preset's page is the
 * tone over the base (a touch darker) so the near-white cards stand out even on white. A photo is never drawn behind a page
 * (readability first): the page is the photo's solid surface colour and the cards use the lighter tone over it.
 * Every text colour is at least 4.5:1 on both [page] and [card]; [accent] is at least 3:1 on [card].
 */
data class PagePalette(
    val page: Int,
    val card: Int,
    val onCard: Int,
    val muted: Int,
    val divider: Int,
    val category: Int,
    val accent: Int,
    val onAccent: Int,
    val switchOff: Int,
    val isDark: Boolean,
    val tokens: UiTokens,
) {
    companion object {
        private const val OPAQUE = 0xFF shl 24
        private const val BLACK = OPAQUE
        private const val WHITE = OPAQUE or 0xFFFFFF
        private const val MUTED_START = 0.72f
        private const val MUTED_STEP = 0.04f

        /** The palette of the MAIN (home) background stored in [prefs]; call again whenever it may have changed. */
        @JvmStatic
        fun fromPrefs(prefs: SharedPreferences): PagePalette =
            from(BackgroundPolicy.tokenInput(BackgroundPrefs.resolve(prefs, BackgroundSlot.MAIN)))

        @JvmStatic
        fun from(input: TokenInput): PagePalette {
            val tokens = UiTokens.from(input)
            val base = OPAQUE or (input.baseColor and 0xFFFFFF)
            val page = when {
                input.isPhoto -> UiTokens.over(base, tokens.surface)
                input.isDark -> base
                else -> UiTokens.over(base, tokens.surfaceTone)
            }
            val card = if (input.isPhoto) UiTokens.over(page, tokens.surfaceTone) else UiTokens.over(base, tokens.surface)
            val backs = listOf(page, card)
            val onCard = listOf(OPAQUE or tokens.onSurface, BLACK, WHITE).maxBy { worst(it, backs) }
            val category = listOf(OPAQUE or tokens.onOutlineAction, onCard).first { worst(it, backs) >= UiTokens.MIN_TEXT_CONTRAST }
            val divider = UiTokens.over(card, tokens.outline)
            return PagePalette(
                page = page,
                card = card,
                onCard = onCard,
                muted = mutedOver(onCard, card, backs),
                divider = divider,
                category = category,
                accent = tokens.accent,
                onAccent = tokens.onAccent,
                switchOff = divider,
                isDark = input.isDark,
                tokens = tokens,
            )
        }

        private fun worst(fg: Int, backs: List<Int>): Double = backs.minOf { Wcag.contrast(fg, it) }

        /** [onCard] faded toward the card until it is still 4.5:1 on every background it may sit on. */
        private fun mutedOver(onCard: Int, card: Int, backs: List<Int>): Int {
            var alpha = MUTED_START
            var color = Wcag.blend(card, onCard, alpha)
            while (worst(color, backs) < UiTokens.MIN_TEXT_CONTRAST && alpha < 1f) {
                alpha = (alpha + MUTED_STEP).coerceAtMost(1f)
                color = Wcag.blend(card, onCard, alpha)
            }
            return color
        }
    }
}
