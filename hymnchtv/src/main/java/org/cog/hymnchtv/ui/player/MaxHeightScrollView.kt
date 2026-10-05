package org.cog.hymnchtv.ui.player

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/** A scroll container with a height limit (the player card's rows in landscape, 1.6.0); 0 means no limit. */
class MaxHeightScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : NestedScrollView(context, attrs) {
    var maxHeightPx: Int = 0
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = if (maxHeightPx > 0) {
            val size = MeasureSpec.getSize(heightMeasureSpec)
            val limit = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) maxHeightPx else minOf(size, maxHeightPx)
            MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, spec)
    }
}
