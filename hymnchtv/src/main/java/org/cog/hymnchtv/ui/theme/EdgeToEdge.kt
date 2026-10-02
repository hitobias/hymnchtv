package org.cog.hymnchtv.ui.theme

import android.app.Activity
import androidx.core.view.WindowCompat

/** Opt-in edge-to-edge: only activities that call enable() are affected. */
object EdgeToEdge {
    fun enable(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    }
}
