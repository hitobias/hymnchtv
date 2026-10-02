package org.cog.hymnchtv.ui.home

import android.content.Context
import androidx.core.view.ViewCompat
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.HymnPickerViews

/** The newest history entries as a row of chips under the home picker: tap opens, long press (or the TalkBack action) removes. */
class RecentChips(
    private val views: HymnPickerViews,
    private val onOpen: (HistoryRecord) -> Unit,
) {
    private val context: Context = views.root.context
    private var released = false

    fun release() {
        released = true
    }

    /** Reads the history in the background and rebuilds the chips. */
    fun reload() {
        HistoryActions.load(context) { records -> if (!released) show(records.take(MAX_CHIPS)) }
    }

    private fun show(records: List<HistoryRecord>) {
        views.recentChips.removeAllViews()
        views.recentArea.visibility = if (records.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        records.forEach { record -> views.recentChips.addView(chipOf(record)) }
    }

    private fun chipOf(record: HistoryRecord): Chip {
        val ref = HistoryActions.refOf(record)
        return Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
            text = HymnLabels.chip(context, ref)
            contentDescription = HymnLabels.spoken(context, ref, record.hymnTitle)
            minHeight = (MIN_TOUCH_DP * resources.displayMetrics.density).toInt()
            setOnClickListener { onOpen(record) }
            setOnLongClickListener {
                confirmDelete(record)
                true
            }
            ViewCompat.addAccessibilityAction(this, context.getString(R.string.c_chip_remove_action)) { _, _ ->
                confirmDelete(record)
                true
            }
        }.also(views::styleChip)
    }

    private fun confirmDelete(record: HistoryRecord) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete)
            .setMessage(context.getString(R.string.delete_history, record.toString()))
            .setPositiveButton(R.string.delete) { _, _ ->
                HistoryActions.delete(context, record) { reload() }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val MAX_CHIPS = 8
        private const val MIN_TOUCH_DP = 48
    }
}
