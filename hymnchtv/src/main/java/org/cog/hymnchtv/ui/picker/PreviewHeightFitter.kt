package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.LyricsTypefaces

/**
 * Gives the preview card one fixed height: the tallest the card gets in any state (book name over the number, up to two
 * lines of title with the English note, or the English note over the "also in" label and a row of chips). Typing then never moves the keypad.
 * The height is measured with scratch views (so nothing on screen is disturbed) once when the controller is created, and
 * again only when a Kai face arrives (its line height differs from the system font's).
 */
class PreviewHeightFitter(private val views: HymnPickerViews, private val titleTraditional: () -> Boolean) {
    private val context: Context = views.root.context

    fun fit() {
        val height = heightPx()
        val area = views.previewArea
        val lp = area.layoutParams
        if (lp.height != height) area.layoutParams = lp.apply { this.height = height }
        // The Kai faces are loaded off the main thread; once one arrives its metrics are part of the maximum
        for (traditional in listOf(titleTraditional(), KaiText.isTraditionalUi(context.resources.configuration.locales[0])).distinct()) {
            if (LyricsTypefaces.peek(traditional) == null) LyricsTypefaces.request(context, traditional) { if (views.root.isAttachedToWindow) fit() }
        }
    }

    /** The fixed card height in px. */
    fun heightPx(): Int {
        val faces = listOf<Typeface>(Typeface.DEFAULT) + listOfNotNull(LyricsTypefaces.peek(true), LyricsTypefaces.peek(false))
        val area = views.previewArea
        val padding = area.paddingTop + area.paddingBottom
        val titlePx = context.resources.getDimension(R.dimen.type_title)
        val bodyPx = context.resources.getDimension(R.dimen.type_body)
        val note = lineHeight(views.note, null, Typeface.DEFAULT, false, 1)
        val entry = lineHeight(views.entry, null, Typeface.DEFAULT, false, 1)
        val chips = chipBlock()
        val tallest = faces.maxOf { face ->
            val left = lineHeight(views.book, null, face, false, 1) + entry
            // a hymn title (Title size, two lines) with the English note below it, or a message at 16sp
            val titleState = maxOf(left, lineHeight(views.title, titlePx, face, false, 2) + note)
            val messageState = maxOf(left, lineHeight(views.title, bodyPx, Typeface.DEFAULT, false, 2))
            // the chips take the title's place under the (English) note, in the same cell
            val chipState = maxOf(left, note + chips)
            maxOf(titleState, messageState, chipState)
        }
        return tallest + padding
    }

    /** [lines] lines of [like]'s style at [sizePx] (its own size when null) in [face]. */
    private fun lineHeight(like: TextView, sizePx: Float?, face: Typeface, bold: Boolean, lines: Int): Int {
        val scratch = TextView(context)
        scratch.includeFontPadding = like.includeFontPadding
        scratch.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx ?: like.textSize)
        scratch.setTypeface(face, if (bold) Typeface.BOLD else Typeface.NORMAL)
        scratch.text = List(lines) { "體" }.joinToString("\n")
        scratch.maxLines = lines
        scratch.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return scratch.measuredHeight
    }

    /** The "also in" block: its top margin, the one-line label and a single row of chips. */
    private fun chipBlock(): Int {
        val density = context.resources.displayMetrics.density
        val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        column.addView(TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.type_caption))
            maxLines = 1
            text = "體"
        })
        val group = ChipGroup(context).apply { isSingleLine = true }
        group.addView(Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
            text = "體"
            minHeight = (HymnPickerController.MIN_TOUCH_DP * density).toInt()
        })
        column.addView(HorizontalScrollView(context).apply { addView(group) })
        column.measure(View.MeasureSpec.makeMeasureSpec((WIDTH_DP * density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        val marginTop = (views.alsoScroll.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin
        return marginTop + column.measuredHeight
    }

    private companion object {
        const val WIDTH_DP = 300
    }
}
