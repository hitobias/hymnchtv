package org.cog.hymnchtv.ui.picker

import android.view.HapticFeedbackConstants
import android.view.View

/** Key-tap feedback of the number keys and the hymn book buttons (spec 5b-4). */
object KeyHaptics {
    /**
     * A light keyboard tick. It uses no override flags, so the system's "touch feedback" setting (and the view's own
     * hapticFeedbackEnabled) decide whether anything is felt.
     *
     * @return whether the system performed it (false when the user switched touch feedback off)
     */
    @JvmStatic
    fun keyTap(view: View): Boolean = view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
