package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.MainScreenColors
import org.cog.hymnchtv.reading.background.ReadingPalette
import org.cog.hymnchtv.reading.background.Wcag
import org.cog.hymnchtv.ui.picker.HymnPickerViews

/**
 * Applies the user's main-screen background (plan A2), font size and font colour to the home picker.
 * Logic moved from MainActivity.applyMainBackground/setFontSize/setFontColor.
 */
class HomeAppearance(private val context: Context, private val views: HymnPickerViews) {
    /** What [apply] put on screen. */
    data class Result(val palette: ReadingPalette, val textColor: Int)

    fun apply(prefs: SharedPreferences): Result {
        val background = checkNotNull(views.background) { "HomeAppearance needs the home layout's background view" }
        val palette = BackgroundPrefs.applyTo(background, prefs, BackgroundSlot.MAIN)
        applyBackground(palette)
        val size = HomePrefs.textSize(prefs)
        applyFontSize(size)
        val chosen = HomePrefs.textColor(prefs, ContextCompat.getColor(context, R.color.grey900))
        // What is drawn must be readable on the background: the user's choice only if it is
        val color = MainScreenColors.textColor(chosen, palette)
        applyFontColor(color, palette)
        return Result(palette, color)
    }

    private fun applyBackground(palette: ReadingPalette) {
        // Photo backgrounds: preview, search field, keys and recent chips sit on the same contrast-tested panel as the lyrics
        // (the backdrop is null otherwise). Each view needs its own drawable instance.
        views.previewArea.background = BackgroundDrawables.backdrop(context, palette)
        views.keypadArea.background = BackgroundDrawables.backdrop(context, palette)
        views.actionArea.background = BackgroundDrawables.backdrop(context, palette)
        views.recentArea.background = BackgroundDrawables.backdrop(context, palette)
        views.searchField.background = BackgroundDrawables.backdrop(context, palette) ?: fieldBackground(palette)
    }

    private fun applyFontSize(size: Int) {
        val small = (size - SMALL_KEY_DELTA).toFloat()
        views.digits.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat()) }
        views.fu.setTextSize(TypedValue.COMPLEX_UNIT_SP, small)
        // The source buttons share a row of four: keep them between a readable minimum and what fits
        views.books.values.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, small.coerceIn(SOURCE_MIN_SP, SOURCE_MAX_SP)) }
        views.toc.setTextSize(TypedValue.COMPLEX_UNIT_SP, small.coerceIn(SOURCE_MIN_SP, SOURCE_MAX_SP))
    }

    /** The search field on a plain background: a soft panel in the background's own paper colour, never the theme's surface. */
    private fun fieldBackground(palette: ReadingPalette): Drawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            cornerRadius = FIELD_RADIUS_DP * density
            setColor(ColorUtils.setAlphaComponent(palette.paperColor, FIELD_FILL_ALPHA))
            setStroke(density.toInt().coerceAtLeast(1), ColorUtils.setAlphaComponent(palette.textColor, STROKE_ALPHA))
        }
    }

    /** Text colour that dims when disabled (a key that leads nowhere must look it). */
    private fun stateText(color: Int) = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf(ColorUtils.setAlphaComponent(color, DISABLED_ALPHA), color),
    )

    /**
     * Everything drawn over the background takes its colours from the background and the user's text colour, not from
     * the app theme: a light background in the dark theme (or the reverse) must stay readable.
     */
    private fun applyFontColor(color: Int, palette: ReadingPalette) {
        views.textColor = color
        views.entry.setTextColor(color)
        views.title.setTextColor(color)
        views.alsoLabel.setTextColor(color)
        views.recentLabel.setTextColor(color)
        views.searchField.setTextColor(color)
        views.searchField.setHintTextColor(MainScreenColors.hintColor(color))
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(views.searchField, ColorStateList.valueOf(color))
        val text = stateText(color)
        views.coloredButtons.forEach { it.setTextColor(text) }
        views.recentMore.setTextColor(color)
        val soft = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, SOFT_ALPHA))
        val ripple = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, RIPPLE_ALPHA))
        val checkedFill = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ColorUtils.setAlphaComponent(color, SOFT_ALPHA), Color.TRANSPARENT),
        )
        views.books.values.forEach {
            it.strokeColor = ColorStateList.valueOf(color)
            it.backgroundTintList = checkedFill
            it.rippleColor = ripple
        }
        views.toc.backgroundTintList = soft
        views.toc.rippleColor = ripple
        // The open button is the one filled button: the palette's accent with whichever of black/white reads best on it
        val accent = palette.accentColor
        val onAccent = if (Wcag.contrast(Color.WHITE, accent) >= Wcag.contrast(Color.BLACK, accent)) Color.WHITE else Color.BLACK
        views.open.backgroundTintList = ColorStateList(
            arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
            intArrayOf(ColorUtils.setAlphaComponent(color, DISABLED_FILL_ALPHA), accent),
        )
        views.open.setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(color, onAccent)))
        views.open.iconTint = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(color, onAccent))
        val icon = ColorStateList.valueOf(color)
        views.delete.iconTint = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(ColorUtils.setAlphaComponent(color, DISABLED_ALPHA), color))
        views.recentMore.iconTint = icon
    }

    private companion object {
        const val SMALL_KEY_DELTA = 10
        const val SOURCE_MIN_SP = 14f
        const val SOURCE_MAX_SP = 18f
        const val FIELD_RADIUS_DP = 28f
        const val FIELD_FILL_ALPHA = 0xCC
        const val STROKE_ALPHA = 0x66
        const val DISABLED_ALPHA = 0x61
        const val DISABLED_FILL_ALPHA = 0x1F
        const val SOFT_ALPHA = 0x2E
        const val RIPPLE_ALPHA = 0x1F
    }
}
