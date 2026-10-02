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

/** The home tab's recent-hymns list: tap opens, the trailing button or a swipe deletes, a long press asks first. */
class HistoryAdapter(
    private val onOpen: (HistoryRecord) -> Unit,
    private val onDelete: (HistoryRecord) -> Unit,
    private val onLongPress: (HistoryRecord) -> Unit,
) : ListAdapter<HistoryRecord, HistoryAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_item_name)
        val delete: View = view.findViewById(R.id.b_delete_in_list)
    }

    /** The record at [position]; the swipe callback uses it. */
    fun recordAt(position: Int): HistoryRecord = getItem(position)

    /** Colour of the row text (the user's main-screen font colour). */
    var textColor: Int? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_history, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val record = getItem(position)
        holder.name.text = record.toString()
        textColor?.let(holder.name::setTextColor)
        holder.itemView.setOnClickListener { onOpen(record) }
        holder.itemView.setOnLongClickListener { onLongPress(record); true }
        holder.delete.setOnClickListener { onDelete(record) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<HistoryRecord>() {
            override fun areItemsTheSame(a: HistoryRecord, b: HistoryRecord) =
                a.hymnType == b.hymnType && a.hymnNo == b.hymnNo

            override fun areContentsTheSame(a: HistoryRecord, b: HistoryRecord) =
                a.isFu == b.isFu && a.hymnTitle == b.hymnTitle && a.timeStamp == b.timeStamp
        }
    }
}
