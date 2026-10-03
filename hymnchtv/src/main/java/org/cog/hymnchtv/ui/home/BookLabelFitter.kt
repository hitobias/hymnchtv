package org.cog.hymnchtv.ui.home

import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.UniformFit

/**
 * Gives all eight book cells one text size: the largest (up to [maxSp]) at which every label fits its cell on one line.
 * Each cell fitting by itself would leave neighbours in different sizes when the system font is enlarged. The size never
 * falls below [MIN_SP]; text that still did not fit would be cut off, and the tests prove it does not at 320dp.
 */
class BookLabelFitter(private val views: HymnPickerViews, private val maxSp: Float) {
    private val cells: List<MaterialButton> = views.books.values.toList() + views.toc
    private val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fit() }

    fun attach() {
        cells.forEach { it.addOnLayoutChangeListener(listener) }
        fit()
    }

    fun detach() {
        cells.forEach { it.removeOnLayoutChangeListener(listener) }
    }

    private fun available(cell: MaterialButton): Float =
        (cell.width - cell.paddingLeft - cell.paddingRight - (if (cell.icon != null) cell.iconSize + cell.iconPadding else 0)).toFloat()

    private fun needed(cell: TextView, sizePx: Float): Float {
        val paint = Paint(cell.paint).apply { textSize = sizePx }
        return paint.measureText(cell.text.toString())
    }

    private fun fit() {
        if (cells.any { it.width == 0 }) return
        val scaled = views.root.resources.displayMetrics.scaledDensity
        val sizePx = UniformFit.pick(maxSp * scaled, MIN_SP * scaled, STEP_SP * scaled) { px ->
            cells.all { needed(it, px) <= available(it) }
        }
        if (cells.all { it.textSize == sizePx }) return
        cells.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx) }
    }

    private companion object {
        const val MIN_SP = 10f
        const val STEP_SP = 0.5f
    }
}
