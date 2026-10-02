package org.cog.hymnchtv.ui.toc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.TextView
import org.cog.hymnchtv.R

/** Groups (categories / index headings) with their hymn items; rows are at least 48dp tall. */
class TocAdapter(private val toc: Map<String, List<String>>) : BaseExpandableListAdapter() {
    private val groups: List<String> = toc.keys.toList()

    override fun getGroupCount() = groups.size

    override fun getChildrenCount(groupPosition: Int) = toc.getValue(groups[groupPosition]).size

    override fun getGroup(groupPosition: Int): String = groups[groupPosition]

    override fun getChild(groupPosition: Int, childPosition: Int): String = toc.getValue(groups[groupPosition])[childPosition]

    override fun getGroupId(groupPosition: Int) = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()

    override fun hasStableIds() = false

    override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        val row = convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.toc_group_row, parent, false)
        row.findViewById<TextView>(R.id.hymnCategory).text = getGroup(groupPosition)
        row.findViewById<ImageView>(R.id.groupExpandState)
            .setImageResource(if (isExpanded) R.drawable.ic_toc_expanded else R.drawable.ic_toc_collapsed)
        return row
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        val row = (convertView as? TextView) ?: LayoutInflater.from(parent.context).inflate(R.layout.toc_item_row, parent, false) as TextView
        row.text = getChild(groupPosition, childPosition)
        return row
    }
}
