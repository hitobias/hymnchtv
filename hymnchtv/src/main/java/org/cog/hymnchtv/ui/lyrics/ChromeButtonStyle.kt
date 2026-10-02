package org.cog.hymnchtv.ui.lyrics

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import org.cog.hymnchtv.reading.background.UiTokens

/**
 * Paints the lyrics page toolbars from [UiTokens] (visual redesign spec section 6): a `surface` plate behind each bar,
 * `surfaceTone` rounded buttons (selected: `accent`/`onAccent`; disabled: flat `disabledSurface`, dashed `outline`
 * border, `disabledOnSurface` text). Called from the single propagation point (ContentHandler.applyReadingTheme).
 */
object ChromeButtonStyle {
    private const val RADIUS_DP = 12f
    private const val INSET_V_DP = 4
    private const val INSET_H_DP = 2
    private const val OUTLINE_DP = 1.5f
    private const val DASH_DP = 4f
    private const val RIPPLE_ALPHA = 0.16f

    /** Plate + buttons of one bar (the direct children that are buttons get the style). */
    @JvmStatic
    fun styleBar(bar: ViewGroup, tokens: UiTokens) {
        bar.background = ColorDrawable(tokens.surface)
        for (i in 0 until bar.childCount) {
            styleButton(bar.getChildAt(i), tokens)
        }
    }

    @JvmStatic
    fun styleButton(view: View, tokens: UiTokens) {
        val density = view.resources.displayMetrics.density
        view.background = buttonBackground(tokens, density)
        val text = textColors(tokens)
        when (view) {
            is TextView -> view.setTextColor(text)
            is ImageView -> ImageViewCompat.setImageTintList(view, text)
        }
    }

    @JvmStatic
    fun textColors(tokens: UiTokens): ColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(),
        ),
        intArrayOf(tokens.disabledOnSurface, tokens.onAccent, tokens.onSurface),
    )

    private fun buttonBackground(tokens: UiTokens, density: Float): Drawable {
        val states = StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_enabled), shape(tokens.disabledSurface, tokens, density, dashed = true))
            addState(intArrayOf(android.R.attr.state_selected), shape(tokens.accent, tokens, density, dashed = false))
            addState(intArrayOf(), shape(tokens.surfaceTone, tokens, density, dashed = false))
        }
        val ripple = (tokens.onSurface and 0xFFFFFF) or ((RIPPLE_ALPHA * 255).toInt() shl 24)
        val mask = inset(GradientDrawable().apply { setColor(Color.BLACK); cornerRadius = RADIUS_DP * density }, density)
        return RippleDrawable(ColorStateList.valueOf(ripple), inset(states, density), mask)
    }

    private fun shape(fill: Int, tokens: UiTokens, density: Float, dashed: Boolean): Drawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = RADIUS_DP * density
            if (dashed) setStroke((OUTLINE_DP * density).toInt().coerceAtLeast(1), tokens.outline, DASH_DP * density, DASH_DP * density)
        }

    /** 48dp touch area, 40dp painted: vertical inset 4dp, horizontal 2dp so neighbours do not touch. */
    private fun inset(drawable: Drawable, density: Float): Drawable =
        InsetDrawable(drawable, (INSET_H_DP * density).toInt(), (INSET_V_DP * density).toInt(), (INSET_H_DP * density).toInt(), (INSET_V_DP * density).toInt())
}
