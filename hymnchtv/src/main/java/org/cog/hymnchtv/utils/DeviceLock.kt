package org.cog.hymnchtv.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

/** Lock state from any Context (the application one is enough), so it works with no activity alive. */
object DeviceLock {
    /** True if the keyguard is showing, or the screen is off (a device without a lock screen). */
    @JvmStatic
    fun isLocked(context: Context): Boolean {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard?.isKeyguardLocked == true) return true
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return power?.isInteractive == false
    }
}
