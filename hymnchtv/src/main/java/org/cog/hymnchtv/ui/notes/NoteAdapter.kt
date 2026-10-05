package org.cog.hymnchtv.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.page.PagePalette

/** The notes of a hymn, newest first; tap opens the editor. User text keeps the system font (never HymnalKai). */
class NoteAdapter(private val onOpen: (NoteRow) -> Unit) : ListAdapter<NoteRow, NoteAdapter.Holder>(DIFF) {
    /** Set on every resume; rows repaint when it changes. */
    var palette: PagePalette? = null
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.note_date)
        val body: TextView = view.findViewById(R.id.note_body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_note, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val ctx = holder.itemView.context
        val date = NoteDates.label(ctx, System.currentTimeMillis(), row)
        holder.date.text = date
        holder.body.text = row.body
        palette?.let {
            holder.date.setTextColor(it.muted)
            holder.body.setTextColor(it.onCard)
        }
        holder.itemView.contentDescription = ctx.getString(R.string.c_when_desc, row.body, date)
        holder.itemView.setOnClickListener { onOpen(row) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NoteRow>() {
            override fun areItemsTheSame(a: NoteRow, b: NoteRow) = a.id == b.id

            override fun areContentsTheSame(a: NoteRow, b: NoteRow) = a == b
        }
    }
}
