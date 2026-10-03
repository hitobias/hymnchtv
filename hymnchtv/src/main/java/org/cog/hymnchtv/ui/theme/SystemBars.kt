package org.cog.hymnchtv.ui.theme

import android.app.Activity
import android.graphics.Color
import android.os.Build
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * The window draws behind the status and navigation bars (edge to edge); the bars are transparent and only their
 * icon colour follows the background that sits behind them. API 24-25 cannot tint the navigation icons, so
 * there the bar gets a solid colour that white icons stay readable on.
 */
object SystemBars {
    /** Content runs behind the bars; the caller applies the insets itself. Call once, before the content view is used. */
    @JvmStatic
    fun enable(activity: Activity) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    /**
     * @param darkBackground whether the background behind the bars is dark (light icons) or light (dark icons)
     * @param legacyNavColor opaque colour of the navigation bar below API 26, where its icons are always white
     */
    @JvmStatic
    fun styleIcons(activity: Activity, darkBackground: Boolean, legacyNavColor: Int) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkBackground
        controller.isAppearanceLightNavigationBars = !darkBackground
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Color.TRANSPARENT else legacyNavColor
    }

    /** Below API 26 the white navigation icons need a dark bar: the card colour on dark backgrounds, else the text colour. */
    @JvmStatic
    fun legacyNavColor(darkBackground: Boolean, surfaceOverBackground: Int, onSurface: Int): Int =
        (0xFF shl 24) or ((if (darkBackground) surfaceOverBackground else onSurface) and 0xFFFFFF)
}
