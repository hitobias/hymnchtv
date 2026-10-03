package org.cog.hymnchtv.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import org.cog.hymnchtv.R

/**
 * The player card's root. A downward drag that starts on the handle, the title row's empty space or any other
 * non-control area is reported to [dragListener]; a drag that starts on a control (progress bar, buttons, fields)
 * belongs to that control.
 */
class SheetDragLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    interface DragListener {
        fun onDragStart()

        /** [dy] is the downward distance, never negative. */
        fun onDrag(dy: Float)

        /** [velocityY] is px/s, positive downwards; [cancelled] when the system took the gesture away. */
        fun onDragEnd(dy: Float, velocityY: Float, cancelled: Boolean)
    }

    var dragListener: DragListener? = null

    /** Off under TalkBack: its exploration gestures must not turn into drags. */
    var dragEnabled = true

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var tracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var startedOnControl = false
    private var dragging = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!dragEnabled || dragListener == null) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(ev)
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(ev)
                val dy = ev.rawY - downY
                if (!startedOnControl && !dragging && dy > slop && dy > Math.abs(ev.rawX - downX)) {
                    startDrag()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reset()
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragEnabled || dragListener == null) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                begin(ev)
                return !startedOnControl
            }
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(ev)
                val dy = ev.rawY - downY
                if (!dragging && dy > slop && dy > Math.abs(ev.rawX - downX)) startDrag()
                if (dragging) dragListener?.onDrag(dy.coerceAtLeast(0f))
            }
            MotionEvent.ACTION_UP -> finish(ev, cancelled = false)
            MotionEvent.ACTION_CANCEL -> finish(ev, cancelled = true)
        }
        return true
    }

    private fun finish(ev: MotionEvent, cancelled: Boolean) {
        tracker?.addMovement(ev)
        tracker?.computeCurrentVelocity(1000)
        val velocity = tracker?.yVelocity ?: 0f
        val wasDragging = dragging
        val dy = (ev.rawY - downY).coerceAtLeast(0f)
        reset()
        if (wasDragging) dragListener?.onDragEnd(dy, velocity, cancelled)
    }

    private fun startDrag() {
        dragging = true
        parent?.requestDisallowInterceptTouchEvent(true)
        dragListener?.onDragStart()
    }

    private fun begin(ev: MotionEvent) {
        reset()
        tracker = VelocityTracker.obtain().also { it.addMovement(ev) }
        downX = ev.rawX
        downY = ev.rawY
        startedOnControl = hitsControl(this, ev.x, ev.y)
    }

    private fun reset() {
        tracker?.recycle()
        tracker = null
        dragging = false
    }

    /** True when the point lands on a control: the deepest view under it, or one of its parents, takes touches. */
    private fun hitsControl(group: ViewGroup, x: Float, y: Float): Boolean {
        for (i in group.childCount - 1 downTo 0) {
            val child = group.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            val cx = x + group.scrollX - child.left
            val cy = y + group.scrollY - child.top
            if (cx < 0 || cy < 0 || cx >= child.width || cy >= child.height) continue
            if (isControl(child)) return true
            return child is ViewGroup && hitsControl(child, cx, cy)
        }
        return false
    }

    private fun isControl(view: View): Boolean =
        view is SeekBar || (view.isClickable && view.id != R.id.hymn_info)
}
