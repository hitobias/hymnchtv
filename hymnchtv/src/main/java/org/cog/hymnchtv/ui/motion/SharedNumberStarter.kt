package org.cog.hymnchtv.ui.motion

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver

/**
 * Holds a postponed enter transition until the shared target is laid out. [isReady] is asked before every frame and
 * must place the target; [begin] runs once, when it says yes or after [timeoutMs] (so a page that never gets a
 * target cannot stay hidden).
 */
class SharedNumberStarter(
    private val decor: View,
    private val isReady: () -> Boolean,
    private val begin: () -> Unit,
    timeoutMs: Long = Motion.POSTPONE_TIMEOUT_MS,
) {
    private val handler = Handler(Looper.getMainLooper())
    private var done = false

    private val preDraw = ViewTreeObserver.OnPreDrawListener {
        if (!done && isReady()) finish()
        true
    }
    private val timeout = Runnable { finish() }

    init {
        decor.viewTreeObserver.addOnPreDrawListener(preDraw)
        handler.postDelayed(timeout, timeoutMs)
        if (isReady()) finish()
    }

    private fun finish() {
        if (done) return
        done = true
        handler.removeCallbacks(timeout)
        val observer = decor.viewTreeObserver
        if (observer.isAlive) observer.removeOnPreDrawListener(preDraw)
        begin()
    }

    /** Starts at once; used when the activity goes away before either condition. */
    fun cancel() = finish()
}
