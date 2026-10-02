package org.cog.hymnchtv.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import java.util.TimeZone

/** One line of the history page: a day heading or a record. */
sealed interface HistoryItem {
    data class Header(val key: Long, val text: String) : HistoryItem

    data class Row(val record: HistoryRecord, val time: String, val spoken: String) : HistoryItem
}

/** The history page: records grouped under day headings; tap opens, the trailing button or a swipe deletes, a long press asks first. */
class HistoryAdapter(
    private val onOpen: (HistoryRecord) -> Unit,
    private val onDelete: (HistoryRecord) -> Unit,
    private val onLongPress: (HistoryRecord) -> Unit,
) : ListAdapter<HistoryItem, RecyclerView.ViewHolder>(DIFF) {

    class RowHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_item_name)
        val time: TextView = view.findViewById(R.id.tv_item_time)
        val delete: View = view.findViewById(R.id.b_delete_in_list)
    }

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tv_history_header)
    }

    /** The record at [position], or null for a heading (headings cannot be swiped away). */
    fun recordAt(position: Int): HistoryRecord? = (getItem(position) as? HistoryItem.Row)?.record

    override fun getItemViewType(position: Int) = if (getItem(position) is HistoryItem.Header) TYPE_HEADER else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.row_history_header, parent, false))
        } else {
            RowHolder(inflater.inflate(R.layout.row_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryItem.Header -> (holder as HeaderHolder).text.text = item.text
            is HistoryItem.Row -> {
                holder as RowHolder
                holder.name.text = item.record.toString()
                holder.time.text = item.time
                holder.itemView.contentDescription =
                    holder.itemView.context.getString(R.string.c_when_desc, item.record.toString(), item.spoken)
                holder.itemView.setOnClickListener { onOpen(item.record) }
                holder.itemView.setOnLongClickListener { onLongPress(item.record); true }
                holder.delete.setOnClickListener { onDelete(item.record) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1

        /** Newest-first [records] under one heading per calendar day. */
        fun group(ctx: android.content.Context, records: List<HistoryRecord>, now: Long, zone: TimeZone = TimeZone.getDefault()): List<HistoryItem> {
            val out = ArrayList<HistoryItem>()
            var lastDay: Long? = null
            for (r in records) {
                val day = HistoryTime.dayKey(r.timeStamp, zone)
                if (day != lastDay) {
                    out += HistoryItem.Header(day, HistoryTimeText.dayHeader(ctx, now, r.timeStamp))
                    lastDay = day
                }
                out += HistoryItem.Row(r, HistoryTimeText.clock(ctx, r.timeStamp), HistoryTimeText.spoken(ctx, r.timeStamp))
            }
            return out
        }

        private val DIFF = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(a: HistoryItem, b: HistoryItem) = when {
                a is HistoryItem.Header && b is HistoryItem.Header -> a.key == b.key
                a is HistoryItem.Row && b is HistoryItem.Row -> a.record.hymnType == b.record.hymnType && a.record.hymnNo == b.record.hymnNo
                else -> false
            }

            override fun areContentsTheSame(a: HistoryItem, b: HistoryItem) = a == b ||
                (a is HistoryItem.Row && b is HistoryItem.Row && a.record.timeStamp == b.record.timeStamp && a.time == b.time && a.record.hymnTitle == b.record.hymnTitle)
        }
    }
}
