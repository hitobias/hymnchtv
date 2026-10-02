package org.cog.hymnchtv.ui.home

import android.view.View
import android.view.ViewGroup
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.HymnPickerViews
import org.cog.hymnchtv.ui.picker.KeypadMetrics

/**
 * Gives the keypad rows the height the screen has left over (spec 5): everything else on the column is measured as it
 * is, the keypad takes the rest between 48dp and 72dp per row. The recent list sits below the first screen and is not
 * counted. The result does not depend on the keypad's current height, so it settles in one pass.
 */
class KeypadSizer(private val views: HymnPickerViews, private val viewport: View) {
    private val rows: List<View> = listOf(R.id.key_row0, R.id.key_row1, R.id.key_row2, R.id.key_row3).map { views.root.findViewById(it) }
    private val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }

    fun attach() {
        viewport.addOnLayoutChangeListener(listener)
        views.previewArea.addOnLayoutChangeListener(listener)
        update()
    }

    fun detach() {
        viewport.removeOnLayoutChangeListener(listener)
        views.previewArea.removeOnLayoutChangeListener(listener)
    }

    fun update() {
        val keypad = views.keypadArea
        val column = keypad.parent as? ViewGroup ?: return
        if (viewport.height == 0 || column.height == 0) return
        val inner = (viewport as? ViewGroup)?.getChildAt(0)
        val chromePx = (inner?.paddingTop ?: 0) + (inner?.paddingBottom ?: 0)
        var fixedPx = column.height - keypad.height
        val recent = views.recentArea
        if (recent.parent === column && recent.visibility == View.VISIBLE) {
            fixedPx -= recent.height + (recent.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        }
        val density = views.root.resources.displayMetrics.density
        val availablePx = viewport.height - chromePx - fixedPx
        val heightPx = (KeypadMetrics.keyHeightDp(availablePx / density) * density).toInt()
        rows.forEach { row ->
            if (row.layoutParams.height != heightPx) {
                row.layoutParams = row.layoutParams.apply { height = heightPx }
            }
        }
    }
}
