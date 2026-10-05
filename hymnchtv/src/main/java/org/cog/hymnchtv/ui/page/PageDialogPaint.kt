package org.cog.hymnchtv.ui.page

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/** Paints an AlertDialog of a full page with its [PagePalette] (1.6.0): card background, text, buttons and check marks. */
object PageDialogPaint {
    private const val RADIUS_DP = 28f
    private const val INSET_DP = 16f

    @JvmStatic
    fun apply(dialog: AlertDialog, palette: PagePalette) {
        val density = dialog.context.resources.displayMetrics.density
        val shape = GradientDrawable().apply {
            setColor(palette.card)
            cornerRadius = RADIUS_DP * density
        }
        dialog.window?.setBackgroundDrawable(InsetDrawable(shape, (INSET_DP * density).toInt()))
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.setTextColor(palette.onCard)
        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(palette.onCard)
        for (which in intArrayOf(DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL)) {
            dialog.getButton(which)?.setTextColor(palette.category)
        }
        dialog.listView?.let { list ->
            for (i in 0 until list.childCount) paintRow(list.getChildAt(i), palette)
            list.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    child?.let { paintRow(it, palette) }
                }

                override fun onChildViewRemoved(parent: View?, child: View?) = Unit
            })
        }
    }

    private fun paintRow(row: View, palette: PagePalette) {
        val text = row as? CheckedTextView ?: return
        text.setTextColor(palette.onCard)
        text.checkMarkTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(palette.accent, palette.muted),
        )
    }
}
