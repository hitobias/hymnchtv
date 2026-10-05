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
 * The newest history entries as a short list under the home picker: short name and the time opened on one row. How many
 * rows show is decided by [HomeFit] through [fit]; an empty history shows an icon and a sentence instead. Tap opens, long
 * press (or the TalkBack action) removes.
 */
class RecentChips(
    private val views: HymnPickerViews,
    private val onOpen: (HistoryRecord) -> Unit,
) : RecentFit {
    private val context: Context = views.root.context
    private var released = false
    private var records: List<HistoryRecord> = emptyList()
    private var loaded = false
    private var capacity = HomeFit.MAX_RECENT
    private var showEmpty = false

    /** Called when the number of records is known or changed, so the page can be fitted again. */
    override var onChanged: (() -> Unit)? = null

    /** How many history records there are (at most [HomeFit.MAX_RECENT] are kept). */
    override val total: Int get() = records.size

    fun release() {
        released = true
        onChanged = null
    }

    /** Reads the history in the background and rebuilds the items. */
    fun reload() {
        HistoryActions.load(context) { loadedRecords ->
            if (released) return@load
            records = loadedRecords.take(HomeFit.MAX_RECENT)
            loaded = true
            render()
            onChanged?.invoke()
        }
    }

    /** Shows the first [count] records, or the empty hint when there are none and [empty] says it fits. */
    override fun fit(count: Int, empty: Boolean) {
        if (count == capacity && empty == showEmpty) return
        capacity = count
        showEmpty = empty
        render()
    }

    private fun render() {
        views.recentChips.removeAllViews()
        val now = System.currentTimeMillis()
        records.take(capacity).forEach { views.recentChips.addView(itemOf(it, now)) }
        views.recentEmpty.visibility = if (loaded && records.isEmpty() && showEmpty) View.VISIBLE else View.GONE
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
}
