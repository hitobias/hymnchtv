package org.cog.hymnchtv.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

/**
 * The home page's frame. The page is one screen that does not scroll (spec 2): [scrollingAllowed] is false while
 * [HomeFit] says everything fits. It is true only in the accessibility exception (landscape, very small screens, the
 * largest system font), where the controls must not be cut off.
 */
class HomeScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    NestedScrollView(context, attrs, defStyleAttr) {

    var scrollingAllowed: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            isVerticalScrollBarEnabled = value
            overScrollMode = if (value) OVER_SCROLL_IF_CONTENT_SCROLLS else OVER_SCROLL_NEVER
            if (!value) scrollTo(0, 0)
        }

    init {
        overScrollMode = OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = scrollingAllowed && super.onInterceptTouchEvent(ev)

    override fun onTouchEvent(ev: MotionEvent): Boolean = scrollingAllowed && super.onTouchEvent(ev)

    override fun canScrollVertically(direction: Int): Boolean = scrollingAllowed && super.canScrollVertically(direction)

    override fun onStartNestedScroll(child: android.view.View, target: android.view.View, axes: Int, type: Int): Boolean =
        scrollingAllowed && super.onStartNestedScroll(child, target, axes, type)

    override fun requestChildRectangleOnScreen(child: android.view.View, rectangle: android.graphics.Rect, immediate: Boolean): Boolean =
        scrollingAllowed && super.requestChildRectangleOnScreen(child, rectangle, immediate)
}
