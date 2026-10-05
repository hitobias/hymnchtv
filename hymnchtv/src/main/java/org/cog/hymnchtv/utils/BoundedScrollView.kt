package org.cog.hymnchtv.utils

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

/** A ScrollView that never grows taller than [maxHeightPx]: keeps the dialog buttons on screen under long content. */
class BoundedScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {
    var maxHeightPx: Int = Int.MAX_VALUE
        set(value) {
            field = value.coerceAtLeast(0)
            requestLayout()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(heightMeasureSpec)
        val bounded = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.UNSPECIFIED -> MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
            else -> MeasureSpec.makeMeasureSpec(minOf(size, maxHeightPx), MeasureSpec.getMode(heightMeasureSpec))
        }
        super.onMeasure(widthMeasureSpec, bounded)
    }
}
