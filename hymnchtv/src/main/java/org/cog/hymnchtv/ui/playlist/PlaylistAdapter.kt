package org.cog.hymnchtv.ui.playlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HistoryTimeText

/** Playlists (tab and add-to-playlist dialog): tap a row to pick it. Names are user text, so they keep the system font. */
class PlaylistAdapter(private val onPick: (PlaylistRow) -> Unit) : ListAdapter<PlaylistRow, PlaylistAdapter.Holder>(DIFF) {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_playlist_name)
        val count: TextView = view.findViewById(R.id.tv_playlist_count)
        val date: TextView = view.findViewById(R.id.tv_playlist_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_playlist, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val ctx = holder.itemView.context
        val count = ctx.getString(R.string.playlist_count, row.count)
        val date = HistoryTimeText.short(ctx, System.currentTimeMillis(), row.updatedAt)
        holder.name.text = row.name
        holder.count.text = count
        holder.date.text = date
        holder.itemView.contentDescription = ctx.getString(R.string.c_when_desc, ctx.getString(R.string.c_when_desc, row.name, count), date)
        holder.itemView.setOnClickListener { onPick(row) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PlaylistRow>() {
            override fun areItemsTheSame(a: PlaylistRow, b: PlaylistRow) = a.id == b.id

            override fun areContentsTheSame(a: PlaylistRow, b: PlaylistRow) = a == b
        }
    }
}
