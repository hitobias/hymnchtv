package org.cog.hymnchtv.ui.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import org.cog.hymnchtv.reading.background.GlassMode

/** Which glass the player layer gets: blurred on API 31+, a flat translucent fallback below, opaque for high-contrast text. */
object GlassPolicy {
    /** `Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED` (hidden constant, stable key). */
    const val HIGH_TEXT_CONTRAST_KEY = "high_text_contrast_enabled"

    @JvmStatic
    fun highTextContrast(context: Context): Boolean =
        Settings.Secure.getInt(context.contentResolver, HIGH_TEXT_CONTRAST_KEY, 0) == 1

    @JvmStatic
    fun highTextContrastUri(): Uri = Settings.Secure.getUriFor(HIGH_TEXT_CONTRAST_KEY)

    @JvmStatic
    fun mode(context: Context): GlassMode = mode(Build.VERSION.SDK_INT, highTextContrast(context))

    @JvmStatic
    fun mode(sdk: Int, highContrast: Boolean): GlassMode = when {
        highContrast -> GlassMode.OPAQUE
        sdk >= Build.VERSION_CODES.S -> GlassMode.BLUR
        else -> GlassMode.FALLBACK
    }
}
