package org.cog.hymnchtv.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.picker.HymnLabels

/** The favourites tab: tap a row to open it, the trailing star removes it. No long press and no swipe, by design. */
class FavoriteAdapter(
    private val onOpen: (FavoriteRow) -> Unit,
    private val onUnfavorite: (FavoriteRow) -> Unit,
) : ListAdapter<FavoriteRow, FavoriteAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val headline: TextView = view.findViewById(R.id.tv_fav_headline)
        val title: TextView = view.findViewById(R.id.tv_fav_title)
        val date: TextView = view.findViewById(R.id.tv_fav_date)
        val unfavorite: View = view.findViewById(R.id.b_unfavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_favorite, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val ctx = holder.itemView.context
        val dateText = HistoryTimeText.short(ctx, System.currentTimeMillis(), row.dateMillis)
        holder.headline.text = HymnLabels.headline(ctx, row.ref)
        holder.title.text = row.title.orEmpty()
        holder.title.visibility = if (row.title.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.date.text = dateText
        holder.itemView.contentDescription = ctx.getString(R.string.c_when_desc, HymnLabels.spoken(ctx, row.ref, row.title), dateText)
        holder.itemView.setOnClickListener { onOpen(row) }
        holder.unfavorite.setOnClickListener { onUnfavorite(row) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavoriteRow>() {
            override fun areItemsTheSame(a: FavoriteRow, b: FavoriteRow) = a.key == b.key

            override fun areContentsTheSame(a: FavoriteRow, b: FavoriteRow) = a == b
        }
    }
}
