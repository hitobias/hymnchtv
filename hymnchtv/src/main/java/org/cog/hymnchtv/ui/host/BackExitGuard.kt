package org.cog.hymnchtv.ui.host

/**
 * Double-back to leave (spec 5): the first back press on the home page only asks for a second one; the second press
 * within [WINDOW_MS] lets the app close. The clock is passed in so the rule is testable.
 */
class BackExitGuard(private val windowMs: Long = WINDOW_MS) {
    private var firstPressAt: Long? = null

    /** @return true when this press should close the app, false when the "press again" hint should be shown instead */
    fun onBackOnHome(nowMs: Long): Boolean {
        val first = firstPressAt
        if (first != null && nowMs - first in 0..windowMs) {
            firstPressAt = null
            return true
        }
        firstPressAt = nowMs
        return false
    }

    /** Forgets a first press, for example when the user went to another page in between. */
    fun reset() {
        firstPressAt = null
    }

    companion object {
        const val WINDOW_MS = 2000L
    }
}
