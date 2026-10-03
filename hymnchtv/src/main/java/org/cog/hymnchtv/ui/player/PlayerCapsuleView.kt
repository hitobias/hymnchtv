package org.cog.hymnchtv.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration

/**
 * The floating capsule: a frosted [GlassFrameLayout] holding a row of buttons. Taps go to its buttons; an upward drag
 * anywhere on it asks to expand the player.
 */
class PlayerCapsuleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    GlassFrameLayout(context, attrs) {
    /** Called once when an upward drag or fling is released far or fast enough. */
    var onExpandGesture: (() -> Unit)? = null

    /** Off under TalkBack: its exploration gestures must not turn into drags. */
    var dragEnabled = true

    private val density = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var tracker: VelocityTracker? = null
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!dragEnabled) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> begin(ev)
            MotionEvent.ACTION_MOVE -> {
                tracker?.addMovement(ev)
                val up = downY - ev.rawY
                if (up > slop && up > Math.abs(ev.rawX - downX)) {
                    dragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> end()
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!dragEnabled) return super.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                begin(ev)
                return true
            }
            MotionEvent.ACTION_MOVE -> tracker?.addMovement(ev)
            MotionEvent.ACTION_UP -> {
                tracker?.addMovement(ev)
                val up = downY - ev.rawY
                tracker?.computeCurrentVelocity(1000)
                val fling = -(tracker?.yVelocity ?: 0f)
                val expand = dragging && PlayerSheetState.shouldExpand(up, fling, density)
                end()
                if (expand) onExpandGesture?.invoke()
            }
            MotionEvent.ACTION_CANCEL -> end()
        }
        return true
    }

    private fun begin(ev: MotionEvent) {
        end()
        tracker = VelocityTracker.obtain().also { it.addMovement(ev) }
        downX = ev.rawX
        downY = ev.rawY
    }

    private fun end() {
        tracker?.recycle()
        tracker = null
        dragging = false
    }
}
