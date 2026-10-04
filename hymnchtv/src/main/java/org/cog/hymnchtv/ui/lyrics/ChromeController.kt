package org.cog.hymnchtv.ui.lyrics

/** Delayed callbacks for [ChromeController]; a fake one makes the idle-hide rule testable without waiting. */
interface ChromeTimer {
    /** @return a token for [cancel] */
    fun postDelayed(delayMs: Long, action: Runnable): Any

    fun cancel(token: Any)
}

/**
 * Show/hide state of the lyrics page toolbars (top bar and the three buttons under it), plan 6c.
 * One instance per lyrics activity; the pages only follow [onVisibilityChanged]. The play card never takes part.
 *
 * Rules: opening starts with the toolbars hidden (lyrics only); a centre tap toggles; shown, it fades after [IDLE_HIDE_MS]
 * without interaction; held (Aa sheet or overflow menu open) it stays; always visible while TalkBack is on.
 */
class ChromeController(
    private val timer: ChromeTimer,
    private val onVisibilityChanged: (visible: Boolean) -> Unit,
) {
    var isVisible: Boolean = false
        private set

    private var held = false
    private var alwaysVisible = false
    private var pending: Any? = null
    private var released = false

    /** The screen opens with the toolbars hidden: nothing is shown and no timer runs (TalkBack keeps them visible). */
    fun start() {
        if (released) return
        cancelPending()
        if (alwaysVisible) show()
    }

    /** After recreation (rotation, theme change): keep what the reader had, without replaying the opening. */
    fun restore(visible: Boolean) {
        if (released) return
        isVisible = visible || alwaysVisible
        if (isVisible) scheduleHide(IDLE_HIDE_MS) else cancelPending()
    }

    /** A single tap in the middle of the page. */
    fun toggle() {
        if (released) return
        if (alwaysVisible) return
        if (isVisible) hide() else show()
    }

    /** Any use of a toolbar button restarts the idle timer. */
    fun onInteraction() {
        if (released) return
        if (isVisible) scheduleHide(IDLE_HIDE_MS)
    }

    /** True while the Aa sheet or the overflow menu is open: no fading underneath it. */
    fun setHeld(value: Boolean) {
        if (released) return
        if (held == value) return
        held = value
        if (isVisible) scheduleHide(IDLE_HIDE_MS)
    }

    /** TalkBack (touch exploration): the toolbars stay, because a hidden control cannot be found by touch. */
    fun setAlwaysVisible(value: Boolean) {
        if (released) return
        if (alwaysVisible == value) return
        alwaysVisible = value
        if (value) show() else if (isVisible) scheduleHide(IDLE_HIDE_MS)
    }

    /** After this the controller ignores every call: a dialog or menu dismissed late must not re-arm the timer and keep the activity alive. */
    fun release() {
        released = true
        cancelPending()
    }

    private fun show() {
        setVisible(true)
        scheduleHide(IDLE_HIDE_MS)
    }

    private fun hide() {
        cancelPending()
        setVisible(false)
    }

    private fun setVisible(value: Boolean) {
        if (isVisible == value) return
        isVisible = value
        onVisibilityChanged(value)
    }

    private fun scheduleHide(delayMs: Long) {
        cancelPending()
        if (alwaysVisible || held || !isVisible) return
        pending = timer.postDelayed(delayMs) {
            pending = null
            hide()
        }
    }

    private fun cancelPending() {
        pending?.let { timer.cancel(it) }
        pending = null
    }

    companion object {
        const val IDLE_HIDE_MS = 4_000L
    }
}
