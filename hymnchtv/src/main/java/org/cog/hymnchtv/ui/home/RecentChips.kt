package org.cog.hymnchtv.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.ui.picker.HymnLabels
import org.cog.hymnchtv.ui.picker.HymnPickerViews

/**
 * The newest history entries as a row under the home picker: short name over the time opened.
 * Tap opens, long press (or the TalkBack action) removes.
 */
class RecentChips(
    private val views: HymnPickerViews,
    private val onOpen: (HistoryRecord) -> Unit,
) {
    private val context: Context = views.root.context
    private var released = false

    fun release() {
        released = true
    }

    /** Reads the history in the background and rebuilds the items. */
    fun reload() {
        HistoryActions.load(context) { records -> if (!released) show(records.take(MAX_CHIPS)) }
    }

    private fun show(records: List<HistoryRecord>) {
        views.recentChips.removeAllViews()
        views.recentArea.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        val now = System.currentTimeMillis()
        records.forEach { views.recentChips.addView(itemOf(it, now)) }
    }

    private fun itemOf(record: HistoryRecord, now: Long): View {
        val ref = HistoryActions.refOf(record)
        val item = LayoutInflater.from(context).inflate(R.layout.item_recent, views.recentChips, false)
        item.findViewById<TextView>(R.id.tv_recent_label_item).text = HymnLabels.chip(context, ref)
        item.findViewById<TextView>(R.id.tv_recent_when).text = HistoryTimeText.short(context, now, record.timeStamp)
        item.contentDescription = context.getString(
            R.string.c_when_desc,
            HymnLabels.spoken(context, ref, record.hymnTitle),
            HistoryTimeText.spoken(context, record.timeStamp),
        )
        item.setOnClickListener { onOpen(record) }
        item.setOnLongClickListener {
            confirmDelete(record)
            true
        }
        ViewCompat.addAccessibilityAction(item, context.getString(R.string.c_chip_remove_action)) { _, _ ->
            confirmDelete(record)
            true
        }
        views.styleRecent(item)
        return item
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
    }
}
