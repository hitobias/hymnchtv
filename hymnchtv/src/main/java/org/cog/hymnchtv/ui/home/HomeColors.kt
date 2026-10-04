package org.cog.hymnchtv.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.ReadingPalette
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.KaiText

/**
 * The home screen's colours (visual redesign spec 4 and 5): every surface, button and text takes its colour from the MAIN
 * slot's [UiTokens], never from the app theme, so a light background in the dark theme (or the reverse) stays readable.
 * Immutable; a new one is built whenever the background changes.
 */
class HomeColors(
    private val context: Context,
    private val tokens: UiTokens,
    private val choice: BackgroundChoice,
    private val palette: ReadingPalette,
) {
    private val density = context.resources.displayMetrics.density
    private val isPhoto = choice is BackgroundChoice.Photo

    /**
     * Text drawn straight on the background rather than on a card ("Recent" row): the background's own reading colour,
     * which is contrast-tested on every preset swatch. A photo has unknown pixels, so there it sits on a card instead.
     */
    private val onBackground = if (isPhoto) tokens.onSurface else palette.textColor

    private fun dp(value: Float) = value * density

    private fun states(disabled: Int?, selected: Int?, normal: Int): ColorStateList = ColorStateList(
        listOfNotNull(
            disabled?.let { intArrayOf(-android.R.attr.state_enabled) },
            selected?.let { intArrayOf(android.R.attr.state_selected) },
            intArrayOf(),
        ).toTypedArray(),
        listOfNotNull(disabled, selected, normal).toIntArray(),
    )

    private fun card(fill: Int, radiusDp: Float): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp)
    }

    private fun ripple(): ColorStateList = ColorStateList.valueOf((tokens.onSurface and 0xFFFFFF) or (RIPPLE_ALPHA shl 24))

    fun apply(views: HymnPickerViews) {
        views.theme = this
        applySurfaces(views)
        applyPreviewTexts(views)
        applyBooks(views)
        applyKeys(views)
        applyOpen(views)
        applyRecent(views)
        applyTypography(views)
    }

    private fun applySurfaces(views: HymnPickerViews) {
        views.previewArea.background = card(tokens.surface, CARD_RADIUS_DP)
        views.searchField.background = card(tokens.surface, CARD_RADIUS_DP)
        views.searchField.setTextColor(tokens.onSurface)
        views.searchField.setHintTextColor(tokens.onSurfaceMuted)
        TextViewCompat.setCompoundDrawableTintList(views.searchField, ColorStateList.valueOf(tokens.onSurfaceMuted))
    }

    private fun applyPreviewTexts(views: HymnPickerViews) {
        views.book.setTextColor(tokens.onSurfaceMuted)
        views.entry.setTextColor(tokens.onSurface)
        views.title.setTextColor(tokens.onSurface)
        views.note.setTextColor(tokens.onSurfaceMuted)
        views.alsoLabel.setTextColor(tokens.onSurfaceMuted)
    }

    private fun applyBooks(views: HymnPickerViews) {
        views.books.values.forEach {
            it.backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(tokens.accent, tokens.surfaceTone),
            )
            it.setTextColor(
                ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(tokens.onAccent, tokens.onSurface)),
            )
            it.rippleColor = ripple()
        }
        // A colour block instead of an outline (spec 5b-2): the raised surface marks the contents button
        views.toc.backgroundTintList = ColorStateList.valueOf(tokens.surfaceRaised)
        views.toc.setTextColor(tokens.onOutlineAction)
        views.toc.iconTint = ColorStateList.valueOf(tokens.onOutlineAction)
        views.toc.rippleColor = ColorStateList.valueOf((tokens.onOutlineAction and 0xFFFFFF) or (RIPPLE_ALPHA shl 24))
    }

    private fun applyKeys(views: HymnPickerViews) {
        val fill = states(tokens.surfaceRaised, tokens.accent, tokens.surfaceTone)
        val text = states(tokens.disabledOnSurface, tokens.onAccent, tokens.onSurface)
        (views.digits + views.fu + views.delete).forEach {
            it.backgroundTintList = fill
            it.setTextColor(text)
            it.iconTint = text
            it.rippleColor = ripple()
        }
    }

    private fun applyOpen(views: HymnPickerViews) {
        val fill = states(tokens.surfaceTone, null, tokens.accent)
        val text = states(tokens.onSurfaceMuted, null, tokens.onAccent)
        views.open.backgroundTintList = fill
        views.open.setTextColor(text)
        views.open.iconTint = text
        views.open.rippleColor = ColorStateList.valueOf((tokens.onAccent and 0xFFFFFF) or (RIPPLE_ALPHA shl 24))
        listOf(views.addPlaylist, views.setNext).forEach {
            it.backgroundTintList = ColorStateList.valueOf(tokens.surfaceTone)
            it.setTextColor(tokens.onSurface)
            it.rippleColor = ripple()
        }
    }

    private fun applyRecent(views: HymnPickerViews) {
        views.recentLabel.setTextColor(onBackground)
        views.recentMore.setTextColor(onBackground)
        views.recentEmptyText.setTextColor(onBackground)
        androidx.core.widget.ImageViewCompat.setImageTintList(views.recentEmptyIcon, ColorStateList.valueOf(onBackground))
        views.recentMore.iconTint = ColorStateList.valueOf(onBackground)
        views.recentMore.rippleColor = ColorStateList.valueOf((onBackground and 0xFFFFFF) or (RIPPLE_ALPHA shl 24))
        val pad = if (isPhoto) dp(PLATE_PAD_DP).toInt() else 0
        views.recentArea.setPadding(pad, pad, pad, pad)
        views.recentArea.background = if (isPhoto) card(tokens.surface, CARD_RADIUS_DP) else null
    }

    private fun applyTypography(views: HymnPickerViews) {
        views.books.values.plus(views.toc).forEach { KaiText.applyForUi(it, context, bold = true) }
        // Book labels are Title size (kept large on purpose since 1.1.1; Kai in a Chinese interface); they shrink together
        // only when a label would not fit, e.g. a large system font on a narrow screen
        views.bookFitter?.detach()
        val titleSp = context.resources.getDimension(R.dimen.type_title) / context.resources.displayMetrics.scaledDensity
        views.bookFitter = BookLabelFitter(views, titleSp).also { it.attach() }
        KaiText.applyForUi(views.fu, context, bold = true)
    }

    /** One recent hymn: a surface card with the short name over the time. */
    fun styleRecent(item: View) {
        item.background = android.graphics.drawable.RippleDrawable(ripple(), card(tokens.surface, CARD_RADIUS_DP), null)
        item.findViewById<TextView>(R.id.tv_recent_label_item).setTextColor(tokens.onSurface)
        item.findViewById<TextView>(R.id.tv_recent_when).setTextColor(tokens.onSurfaceMuted)
    }

    /** An "also in" chip inside the preview card: a tone fill on the card, no outline. */
    fun styleChip(chip: Chip) {
        chip.setTextColor(tokens.onSurface)
        chip.chipBackgroundColor = ColorStateList.valueOf(tokens.surfaceTone)
        chip.chipStrokeWidth = 0f
    }

    private companion object {
        const val CARD_RADIUS_DP = 16f
        const val PLATE_PAD_DP = 8f
        const val RIPPLE_ALPHA = 0x1F
    }
}
