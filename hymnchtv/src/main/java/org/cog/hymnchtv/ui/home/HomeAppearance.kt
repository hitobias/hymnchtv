package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundDrawables
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.MainScreenColors
import org.cog.hymnchtv.reading.background.ReadingPalette
import org.cog.hymnchtv.ui.picker.HymnPickerViews

/**
 * Applies the user's main-screen background (plan A2), font size and font colour to the home picker.
 * Logic moved from MainActivity.applyMainBackground/setFontSize/setFontColor.
 */
class HomeAppearance(private val context: Context, private val views: HymnPickerViews) {
    /** The search field's stock background, kept so a photo panel can be undone. */
    private val searchDefaultBg: Drawable? = views.searchField.background

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
        views.searchField.background = BackgroundDrawables.backdrop(context, palette) ?: searchDefaultBg
    }

    private fun applyFontSize(size: Int) {
        val small = (size - SMALL_KEY_DELTA).toFloat()
        views.digits.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat()) }
        views.fu.setTextSize(TypedValue.COMPLEX_UNIT_SP, small)
        // The source buttons share a row of four: keep them between a readable minimum and what fits
        views.books.values.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, small.coerceIn(SOURCE_MIN_SP, SOURCE_MAX_SP)) }
        views.toc.setTextSize(TypedValue.COMPLEX_UNIT_SP, small.coerceIn(SOURCE_MIN_SP, SOURCE_MAX_SP))
    }

    private fun applyFontColor(color: Int, palette: ReadingPalette) {
        views.entry.setTextColor(color)
        views.title.setTextColor(color)
        views.alsoLabel.setTextColor(color)
        views.recentLabel.setTextColor(color)
        views.searchField.setTextColor(color)
        views.searchField.setHintTextColor(MainScreenColors.hintColor(color))
        // a photo panel keeps its own colour; the default field keeps its outline
        if (palette.backdropColor != 0) ViewCompat.setBackgroundTintList(views.searchField, null)
        views.coloredButtons.forEach { it.setTextColor(color) }
        views.recentMore.setTextColor(color)
        views.books.values.forEach { it.strokeColor = ColorStateList.valueOf(color) }
        val icon = ColorStateList.valueOf(color)
        views.delete.iconTint = icon
        views.toc.iconTint = icon
        views.recentMore.iconTint = icon
    }

    private companion object {
        const val SMALL_KEY_DELTA = 10
        const val SOURCE_MIN_SP = 14f
        const val SOURCE_MAX_SP = 18f
    }
}
