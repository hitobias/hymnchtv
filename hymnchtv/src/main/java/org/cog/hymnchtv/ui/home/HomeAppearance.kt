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

/**
 * Applies the user's main-screen background (plan A2), font size and font colour to the home views.
 * Logic moved from MainActivity.applyMainBackground/setFontSize/setFontColor.
 */
class HomeAppearance(private val context: Context, private val views: HomeViews) {
    /** The search box's stock underline background, kept so a photo panel can be undone. */
    private val searchDefaultBg: Drawable? = views.search.background

    /** What [apply] put on screen. */
    data class Result(val palette: ReadingPalette, val textColor: Int)

    fun apply(prefs: SharedPreferences): Result {
        val palette = BackgroundPrefs.applyTo(views.background, prefs, BackgroundSlot.MAIN)
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
        views.hint.setTextColor(palette.accentColor)
        // Photo backgrounds: hint, entry, search box and keys sit on the same contrast-tested panel as the lyrics
        // (the backdrop is null otherwise). Each view needs its own drawable instance.
        views.hint.background = BackgroundDrawables.backdrop(context, palette)
        views.entry.background = BackgroundDrawables.backdrop(context, palette)
        views.keypadArea.background = BackgroundDrawables.backdrop(context, palette)
        views.actionArea.background = BackgroundDrawables.backdrop(context, palette)
        views.search.background = BackgroundDrawables.backdrop(context, palette) ?: searchDefaultBg
    }

    private fun applyFontSize(size: Int) {
        val small = (size - SMALL_KEY_DELTA).toFloat()
        views.digits.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat()) }
        listOf(views.fu, views.delete, views.english, views.searchButton)
            .forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, small) }
        // The enlarged hymn books never get smaller than their layout size
        views.books.values.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, small.coerceAtLeast(BOOK_MIN_SP)) }
    }

    private fun applyFontColor(color: Int, palette: ReadingPalette) {
        views.entry.setTextColor(color)
        views.entry.setHintTextColor(MainScreenColors.hintColor(color))
        views.preview.setTextColor(color)
        views.search.setTextColor(color)
        views.search.setHintTextColor(MainScreenColors.hintColor(color))
        // the underline follows the text colour; the photo-mode panel must keep its own colour
        ViewCompat.setBackgroundTintList(views.search, if (palette.backdropColor == 0) ColorStateList.valueOf(color) else null)
        views.coloredButtons.forEach { it.setTextColor(color) }
        views.books.values.forEach { it.strokeColor = ColorStateList.valueOf(color) }
    }

    private companion object {
        const val SMALL_KEY_DELTA = 10
        const val BOOK_MIN_SP = 26f
    }
}
