package org.cog.hymnchtv.ui.page

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Spacing of the card layout (spec 5a): 16dp at both sides, 12dp between a card and the next heading. */
class PageCardDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val density = view.resources.displayMetrics.density
        val side = (SIDE_DP * density).toInt()
        outRect.left = side
        outRect.right = side
        val adapter = parent.adapter as? PagePreferenceAdapter ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        if (!adapter.isCategory(position) && adapter.isLastInCard(position)) outRect.bottom = (GAP_DP * density).toInt()
    }

    private companion object {
        const val SIDE_DP = 16
        const val GAP_DP = 12
    }
}
