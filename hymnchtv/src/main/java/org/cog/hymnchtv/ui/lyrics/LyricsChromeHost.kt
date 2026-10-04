package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import org.cog.hymnchtv.R
import java.lang.ref.WeakReference

/** A lyrics page whose toolbars [LyricsChromeHost] shows and hides. */
interface ChromePage {
    fun setChromeVisible(visible: Boolean, animate: Boolean)
}

/** Production [ChromeTimer] on the main looper. */
class HandlerChromeTimer(private val handler: Handler = Handler(Looper.getMainLooper())) : ChromeTimer {
    override fun postDelayed(delayMs: Long, action: Runnable): Any = action.also { handler.postDelayed(it, delayMs) }

    override fun cancel(token: Any) {
        handler.removeCallbacks(token as Runnable)
    }
}

/** Remembers that the "tap the middle to show the toolbar" hint was shown (once, ever). */
object LyricsChromeHint {
    const val PREF_KEY = "LyricsChromeHintShown"

    @JvmStatic
    fun shouldShow(prefs: SharedPreferences): Boolean = runCatching { !prefs.getBoolean(PREF_KEY, false) }.getOrDefault(false)

    @JvmStatic
    fun markShown(prefs: SharedPreferences) {
        prefs.edit().putBoolean(PREF_KEY, true).apply()
    }
}

/**
 * Activity-level owner of the toolbar visibility (plan 6c): one [ChromeController] for all pager pages.
 * Pages register themselves and follow its state, so a page created while swiping starts in the current state.
 */
class LyricsChromeHost(
    private val context: Context,
    private val prefs: SharedPreferences,
    timer: ChromeTimer = HandlerChromeTimer(),
) {
    private val pages = mutableListOf<WeakReference<ChromePage>>()
    private val controller = ChromeController(timer, ::dispatch)
    private val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    private val touchExplorationListener = AccessibilityManager.TouchExplorationStateChangeListener {
        controller.setAlwaysVisible(it)
    }

    val isVisible: Boolean get() = controller.isVisible

    /** @param restoredVisible the state saved before recreation, or null for a fresh start */
    fun start(restoredVisible: Boolean?) {
        accessibility?.addTouchExplorationStateChangeListener(touchExplorationListener)
        controller.setAlwaysVisible(accessibility?.isTouchExplorationEnabled == true)
        if (restoredVisible == null) {
            controller.start()
            if (!controller.isVisible) showHintOnce() // opened with lyrics only: tell the reader once how to bring the toolbars back
        }
        else {
            controller.restore(restoredVisible)
        }
    }

    fun stop() {
        accessibility?.removeTouchExplorationStateChangeListener(touchExplorationListener)
        controller.release()
        pages.clear()
    }

    fun register(page: ChromePage) {
        pages.removeAll { it.get() == null || it.get() === page }
        pages += WeakReference(page)
        page.setChromeVisible(controller.isVisible, false)
    }

    fun unregister(page: ChromePage) {
        pages.removeAll { it.get() == null || it.get() === page }
    }

    fun toggle() = controller.toggle()

    /** A toolbar button was used: restart the idle timer. */
    fun onInteraction() = controller.onInteraction()

    /** What TalkBack turning on does; public so a test can drive it without the screen reader. */
    fun setAlwaysVisible(value: Boolean) = controller.setAlwaysVisible(value)

    /** True while the Aa sheet or the overflow menu is open. */
    fun setHeld(held: Boolean) = controller.setHeld(held)

    private fun dispatch(visible: Boolean) {
        pages.removeAll { it.get() == null }
        pages.forEach { it.get()?.setChromeVisible(visible, true) }
        if (!visible) showHintOnce()
    }

    private fun showHintOnce() {
        if (accessibility?.isTouchExplorationEnabled != true && LyricsChromeHint.shouldShow(prefs)) {
            LyricsChromeHint.markShown(prefs)
            Toast.makeText(context, R.string.c_chrome_hint, Toast.LENGTH_LONG).show()
        }
    }
}
