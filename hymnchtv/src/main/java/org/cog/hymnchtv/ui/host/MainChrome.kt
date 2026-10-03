package org.cog.hymnchtv.ui.host

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.TokenInput
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.theme.SystemBars

/** Colours of the frame around the tabs (status bar, toolbar, bottom navigation, gesture area) for one background. */
data class ChromeColors(
    val bar: Int,
    val onBar: Int,
    val onBarMuted: Int,
    val accent: Int,
    val indicator: Int,
    val isDark: Boolean,
)

/** Space the frame leaves for the system bars and the keyboard, in pixels. */
data class FrameInsets(val left: Int, val top: Int, val right: Int, val bottomPadding: Int, val navPadding: Int)

/**
 * The frame of the main screen follows the home background's [UiTokens] (visual redesign spec 4): toolbar and bottom
 * navigation sit on the card colour (`surface` over the background's base), text is `onSurface`, the selected tab
 * `accent`. The window draws behind the system bars, so the same colour reaches the top and bottom edge of the screen.
 */
object MainChrome {
    @JvmStatic
    fun colors(input: TokenInput, tokens: UiTokens): ChromeColors {
        val bar = UiTokens.over(opaque(input.baseColor), tokens.surface)
        return ChromeColors(
            bar = bar,
            onBar = tokens.onSurface,
            onBarMuted = tokens.onSurfaceMuted,
            accent = tokens.accent,
            indicator = UiTokens.over(bar, tokens.surfaceTone),
            isDark = input.isDark,
        )
    }

    /** The keyboard lifts the whole frame; without it the bottom navigation pads for the gesture area. */
    @JvmStatic
    fun frameInsets(barsLeft: Int, barsTop: Int, barsRight: Int, barsBottom: Int, imeBottom: Int): FrameInsets {
        val keyboardUp = imeBottom > barsBottom
        return FrameInsets(
            left = barsLeft.coerceAtLeast(0),
            top = barsTop.coerceAtLeast(0),
            right = barsRight.coerceAtLeast(0),
            bottomPadding = if (keyboardUp) imeBottom else 0,
            navPadding = if (keyboardUp) 0 else barsBottom.coerceAtLeast(0),
        )
    }

    /** Paints the frame from the MAIN slot's background; call again whenever that background or the theme may have changed. */
    @JvmStatic
    fun apply(activity: Activity, prefs: SharedPreferences) {
        val input = BackgroundPolicy.tokenInput(BackgroundPrefs.resolve(prefs, BackgroundSlot.MAIN))
        val tokens = UiTokens.from(input)
        val c = colors(input, tokens)
        activity.findViewById<MaterialToolbar>(R.id.toolbar)?.apply {
            setBackgroundColor(c.bar)
            setTitleTextColor(c.onBar)
            navigationIcon?.setTint(c.onBar)
        }
        activity.findViewById<BottomNavigationView>(R.id.bottom_nav)?.apply {
            setBackgroundColor(c.bar)
            val tint = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                intArrayOf(c.accent, c.onBarMuted),
            )
            itemIconTintList = tint
            itemTextColor = tint
            itemActiveIndicatorColor = ColorStateList.valueOf(c.indicator)
        }
        SystemBars.styleIcons(activity, c.isDark, SystemBars.legacyNavColor(c.isDark, c.bar, c.onBar))
    }

    /** Toolbar and navigation reach behind the system bars and pad by their size; installs once per activity. */
    @JvmStatic
    fun installInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            applyInsets(root, frameInsets(bars.left, bars.top, bars.right, bars.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun applyInsets(root: View, insets: FrameInsets) {
        root.setPadding(insets.left, 0, insets.right, insets.bottomPadding)
        root.findViewById<View>(R.id.toolbar)?.let { toolbar ->
            val bar = TypedValue()
            toolbar.context.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, bar, true)
            val height = TypedValue.complexToDimensionPixelSize(bar.data, toolbar.resources.displayMetrics) + insets.top
            toolbar.setPadding(toolbar.paddingLeft, insets.top, toolbar.paddingRight, 0)
            if (toolbar.layoutParams.height != height) {
                toolbar.layoutParams = (toolbar.layoutParams as ViewGroup.LayoutParams).apply { this.height = height }
            }
        }
        root.findViewById<View>(R.id.bottom_nav)?.let { it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, insets.navPadding) }
        root.findViewById<View>(R.id.overlay_container)?.let { it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, insets.navPadding) }
    }

    private fun opaque(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)
}
