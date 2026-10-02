package org.cog.hymnchtv.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.search.SearchResult
import org.cog.hymnchtv.ui.picker.HymnLabels

/** One row per hit: "補充本 第 37 首", the title and where the match is. */
class SearchAdapter(private val onOpen: (SearchResult) -> Unit) : ListAdapter<SearchResult, SearchAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.tv_result_label)
        val title: TextView = view.findViewById(R.id.tv_result_title)
        val snippet: TextView = view.findViewById(R.id.tv_result_snippet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_search_result, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val result = getItem(position)
        val ctx = holder.itemView.context
        val headline = HymnLabels.headline(ctx, result.ref)
        holder.label.text = headline
        holder.title.text = result.title
        holder.snippet.text = result.snippet
        holder.itemView.contentDescription = HymnLabels.spoken(ctx, result.ref, result.title)
        holder.itemView.setOnClickListener { onOpen(result) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(a: SearchResult, b: SearchResult) = a.ref == b.ref

            override fun areContentsTheSame(a: SearchResult, b: SearchResult) = a == b
        }
    }
}
