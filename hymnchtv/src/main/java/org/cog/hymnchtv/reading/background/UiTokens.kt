package org.cog.hymnchtv.reading.background

/**
 * What [UiTokens.from] needs to know about a background (visual redesign spec section 4).
 * [isDark] is the brightness of the background actually shown, not the app's DayNight mode.
 * [swatches] are the opaque colours a reader can see behind a card: see [BackgroundPreset.swatches]
 * and [BackgroundPolicy.tokenInput].
 */
data class TokenInput(
    val textColor: Int,
    val accentColor: Int,
    val baseColor: Int,
    val isDark: Boolean,
    val isPhoto: Boolean,
    val swatches: List<Int>,
) {
    init {
        require(swatches.isNotEmpty()) { "TokenInput needs at least one swatch" }
    }
}

/** How the player glass is drawn: blurred (API 31+), a flat translucent fallback, or opaque (high-contrast text). */
enum class GlassMode { BLUR, FALLBACK, OPAQUE }

/**
 * Surface, text and accent colours derived from one background (visual redesign spec section 4). All values are
 * 0xAARRGGBB; [surface], [surfaceTone], [disabledSurface], [onSurfaceMuted] and [outline] are translucent and must be
 * drawn over the background (use [over]), the others are opaque. [surfaceRaised] is translucent too.
 */
data class UiTokens(
    val surface: Int,
    val surfaceTone: Int,
    val onSurface: Int,
    val onSurfaceMuted: Int,
    val accent: Int,
    val onAccent: Int,
    val onOutlineAction: Int,
    val disabledSurface: Int,
    val disabledOnSurface: Int,
    val outline: Int,
    /** [surface] tinted 4% toward [accent] (alpha kept): selected and pressed states on a card. Draw it over the background like [surface]. */
    val surfaceRaised: Int = surface,
) {
    companion object {
        const val MIN_TEXT_CONTRAST = 4.5
        const val MIN_DISABLED_CONTRAST = 3.0
        const val MIN_GRAPHIC_CONTRAST = 3.0

        private const val WHITE = 0xFFFFFF
        private const val BLACK = 0x000000
        private const val PHOTO_SURFACE = 0x1E1E1E
        private const val PHOTO_ALPHA = 0.88f
        private const val PLAIN_ALPHA = 0.92f
        private const val ALPHA_STEP = 0.02f
        const val GLASS_BLUR_ALPHA = 0.72f
        const val GLASS_MAX_ALPHA = 0.94f
        private const val BLUR_TEXT_MIX = 0.5f
        private const val MUTED_START = 0.72f
        private const val MUTED_STEP = 0.04f
        private const val OUTLINE_START = 0.24f
        private const val OUTLINE_STEP = 0.04f
        private const val SHIFT_STEP = 0.04f
        private const val DISABLED_MIX_START = 0.5f
        private const val DISABLED_MIX_STEP = 0.05f
        private const val LIGHT_WHITE_MIX = 0.92f
        private const val DARK_WHITE_MIX = 0.10f
        private const val LIGHT_TONE_MIX = 0.08f
        private const val DARK_TONE_MIX = 0.12f

        /** [fg] (any alpha) painted over the opaque [bg]; the result is opaque. Uses the 8-bit alpha the GPU would. */
        @JvmStatic
        fun over(bg: Int, fg: Int): Int = Wcag.blend(bg, fg, (fg ushr 24) / 255f)

        private fun withAlpha(rgb: Int, alpha: Float): Int =
            ((alpha * 255f + 0.5f).toInt().coerceIn(0, 255) shl 24) or (rgb and 0xFFFFFF)

        private fun opaque(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)

        /**
         * Every opaque colour [UiTokens]' surfaces can show over [swatch]: the card, a tone key beside it, and a tone
         * button sitting on a card (the lyrics top bar). Used by the derivation and by the tests.
         */
        @JvmStatic
        fun backdropsOver(swatch: Int, surface: Int, surfaceTone: Int): List<Int> {
            val card = over(swatch, surface)
            return listOf(card, over(swatch, surfaceTone), over(card, surfaceTone))
        }

        private fun worstContrast(fg: Int, backdrops: List<Int>): Double = backdrops.minOf { Wcag.contrast(fg, it) }

        private fun worstOver(fg: Int, backdrops: List<Int>): Double = backdrops.minOf { Wcag.contrast(over(it, fg), it) }

        /** Moves [color] toward [target] one step at a time while [ok] is false; stops at the pure target. */
        private fun shiftUntil(color: Int, target: Int, ok: (Int) -> Boolean): Int {
            var t = 0f
            var c = opaque(color)
            while (!ok(c) && t < 1f) {
                t = (t + SHIFT_STEP).coerceAtMost(1f)
                c = Wcag.blend(opaque(color), opaque(target), t)
            }
            return c
        }

        @JvmStatic
        fun from(input: TokenInput): UiTokens =
            derive(input, if (input.isPhoto) PHOTO_ALPHA else PLAIN_ALPHA, 1f)

        /**
         * `(surface & 0x00FFFFFF) | (alpha << 24)`: the player glass colour. [alpha] is the final ARGB alpha; the
         * alpha already in [surface] is not multiplied in.
         */
        @JvmStatic
        fun glassTint(surface: Int, alpha: Float): Int = withAlpha(surface and 0xFFFFFF, alpha)

        /** The glass colour with the alpha forced to 1: used when the system asks for high-contrast text. */
        @JvmStatic
        fun opaqueGlassTint(surface: Int): Int = opaque(surface)

        /**
         * Tokens for the frosted player card and capsule (frosted-player spec sections 3 and 6): same colours as
         * [from], but `surface` is the glass tint and every other colour is derived against what can show through it.
         * Blurred ([GlassMode.BLUR]) the worst case behind the glass is the swatch mixed 50% with the lyrics text colour;
         * unblurred ([GlassMode.FALLBACK]) it is the swatch or the bare text colour. The tint starts at
         * [GLASS_BLUR_ALPHA] and rises to at most [GLASS_MAX_ALPHA] until the contrast holds.
         */
        @JvmStatic
        fun glass(input: TokenInput, mode: GlassMode): UiTokens = when (mode) {
            GlassMode.OPAQUE -> derive(input, 1f, 1f)
            GlassMode.BLUR -> derive(input.copy(swatches = withBlurredText(input)), GLASS_BLUR_ALPHA, GLASS_MAX_ALPHA)
            GlassMode.FALLBACK -> derive(
                input.copy(swatches = (input.swatches + opaque(input.textColor)).distinct()), GLASS_MAX_ALPHA, GLASS_MAX_ALPHA)
        }

        private fun withBlurredText(input: TokenInput): List<Int> =
            (input.swatches + input.swatches.map { Wcag.blend(it, opaque(input.textColor), BLUR_TEXT_MIX) }).distinct()

        private fun derive(input: TokenInput, startAlpha: Float, maxAlpha: Float): UiTokens {
            val surfaceRgb = when {
                input.isPhoto -> PHOTO_SURFACE
                input.isDark -> Wcag.blend(input.baseColor, opaque(WHITE), DARK_WHITE_MIX)
                else -> Wcag.blend(input.baseColor, opaque(WHITE), LIGHT_WHITE_MIX)
            }
            val toneRgb = Wcag.blend(surfaceRgb, input.textColor, if (input.isDark) DARK_TONE_MIX else LIGHT_TONE_MIX)
            var alpha = startAlpha
            while (true) {
                val surface = withAlpha(surfaceRgb, alpha)
                val tone = withAlpha(toneRgb, alpha)
                val backdrops = input.swatches.flatMap { backdropsOver(it, surface, tone) }
                val candidates = listOf(input.textColor, opaque(BLACK), opaque(WHITE)).map(::opaque)
                val onSurface = candidates.maxBy { worstContrast(it, backdrops) }
                if (worstContrast(onSurface, backdrops) >= MIN_TEXT_CONTRAST || alpha >= maxAlpha) {
                    return finish(input, surface, tone, onSurface, backdrops)
                }
                alpha = (alpha + ALPHA_STEP).coerceAtMost(maxAlpha)
            }
        }

        private fun finish(input: TokenInput, surface: Int, tone: Int, onSurface: Int, backdrops: List<Int>): UiTokens {
            var mutedAlpha = MUTED_START
            while (worstOver(withAlpha(onSurface, mutedAlpha), backdrops) < MIN_TEXT_CONTRAST && mutedAlpha < 1f) {
                mutedAlpha = (mutedAlpha + MUTED_STEP).coerceAtMost(1f)
            }
            val towardAccent = if (input.isDark) WHITE else BLACK
            val accent = shiftUntil(input.accentColor, towardAccent) { worstContrast(it, backdrops) >= MIN_GRAPHIC_CONTRAST }
            val onAccent = listOf(opaque(BLACK), opaque(WHITE)).maxBy { Wcag.contrast(it, accent) }
            val cards = input.swatches.map { over(it, surface) }
            val onOutline = shiftUntil(accent, towardAccent) { c -> cards.all { Wcag.contrast(c, it) >= MIN_TEXT_CONTRAST } }
            val disabledBackdrops = input.swatches.flatMap { s ->
                val card = over(s, surface)
                listOf(card, over(card, surface))
            }
            var mix = DISABLED_MIX_START
            var disabled = disabledText(onSurface, surface, mix)
            while (worstContrast(disabled, disabledBackdrops) < MIN_DISABLED_CONTRAST && mix > 0f) {
                mix = (mix - DISABLED_MIX_STEP).coerceAtLeast(0f)
                disabled = disabledText(onSurface, surface, mix)
            }
            var outlineAlpha = OUTLINE_START
            val edges = backdrops
            while (worstOver(withAlpha(onSurface, outlineAlpha), edges) < MIN_GRAPHIC_CONTRAST && outlineAlpha < 1f) {
                outlineAlpha = (outlineAlpha + OUTLINE_STEP).coerceAtMost(1f)
            }
            return UiTokens(
                surface = surface,
                surfaceTone = tone,
                onSurface = onSurface,
                onSurfaceMuted = withAlpha(onSurface, mutedAlpha),
                accent = accent,
                onAccent = onAccent,
                onOutlineAction = onOutline,
                disabledSurface = surface,
                disabledOnSurface = disabled,
                outline = withAlpha(onSurface, outlineAlpha),
                surfaceRaised = raised(surface, accent),
            )
        }

        private const val RAISED_ACCENT_MIX = 0.04f

        /** [accent] at 4% painted over the translucent [surface]: the colour moves, the alpha stays. */
        private fun raised(surface: Int, accent: Int): Int =
            (surface and (0xFF shl 24)) or (Wcag.blend(opaque(surface), opaque(accent), RAISED_ACCENT_MIX) and 0xFFFFFF)

        private fun disabledText(onSurface: Int, surface: Int, mix: Float): Int =
            Wcag.blend(onSurface, opaque(surface), mix)
    }
}
