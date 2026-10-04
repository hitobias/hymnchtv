package org.cog.hymnchtv.ui.host

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageView
import androidx.core.graphics.Insets
import androidx.core.widget.ImageViewCompat
import com.google.android.material.appbar.MaterialToolbar
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPrefs
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.TokenInput
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.ui.theme.SystemBars

/** Colours of the frame around the pages (status bar, toolbar, gesture area) for one background. */
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
 * The frame of the main screen follows the home background's [UiTokens] (visual redesign spec 4): the toolbar sits on the
 * card colour (`surface` over the background's base), text and icons are `onSurface`, the update dot `accent`. The window
 * draws behind the system bars, so the same colour reaches the top edge of the screen.
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

    /** The keyboard lifts the whole frame; without it the page containers pad for the gesture area. */
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
        val iconTint = ColorStateList.valueOf(c.onBar)
        for (id in intArrayOf(R.id.btn_home_toc, R.id.btn_home_settings)) {
            activity.findViewById<ImageView>(id)?.let { ImageViewCompat.setImageTintList(it, iconTint) }
        }
        activity.findViewById<View>(R.id.home_settings_badge)?.let { ViewCompat.setBackgroundTintList(it, ColorStateList.valueOf(c.accent)) }
        SystemBars.styleIcons(activity, c.isDark, SystemBars.legacyNavColor(c.isDark, c.bar, c.onBar))
    }

    /**
     * The toolbar reaches behind the status bar and pads by its size; the page containers pad for the navigation bar
     * (the home page paints its own background behind it instead, see [setHomeOnTop]). Installs once per activity.
     */
    @JvmStatic
    fun installInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val insets = frameInsets(bars.left, bars.top, bars.right, bars.bottom, ime.bottom)
            applyInsets(root, insets)
            // The pages inside may pad for the navigation bar themselves (the home page does); the rest is used up here
            WindowInsetsCompat.Builder(windowInsets)
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 0, 0, insets.navPadding))
                .build()
        }
        ViewCompat.requestApplyInsets(root)
    }

    /**
     * Tells the page container whether the home page is the page shown. The home page pads for the navigation bar inside
     * itself (so its background reaches the screen edge); any other page gets the padding from the container.
     */
    @JvmStatic
    fun setHomeOnTop(container: View, homeOnTop: Boolean) {
        container.setTag(R.id.chrome_home_on_top, homeOnTop)
        refreshContainerPadding(container)
    }

    private fun refreshContainerPadding(container: View) {
        val nav = container.getTag(R.id.chrome_nav_padding) as? Int ?: 0
        val homeOnTop = container.getTag(R.id.chrome_home_on_top) as? Boolean ?: true
        val bottom = if (homeOnTop) 0 else nav
        if (container.paddingBottom != bottom) container.setPadding(container.paddingLeft, container.paddingTop, container.paddingRight, bottom)
    }

    private fun applyInsets(root: View, insets: FrameInsets) {
        root.setPadding(insets.left, 0, insets.right, insets.bottomPadding)
        root.findViewById<View>(R.id.toolbar)?.let { toolbar ->
            val height = toolbar.resources.getDimensionPixelSize(R.dimen.home_top_bar_height) + insets.top
            toolbar.setPadding(toolbar.paddingLeft, insets.top, toolbar.paddingRight, 0)
            if (toolbar.layoutParams.height != height) {
                toolbar.layoutParams = (toolbar.layoutParams as ViewGroup.LayoutParams).apply { this.height = height }
            }
        }
        root.findViewById<View>(R.id.fragment_container)?.let {
            it.setTag(R.id.chrome_nav_padding, insets.navPadding)
            refreshContainerPadding(it)
        }
        root.findViewById<View>(R.id.overlay_container)?.let { it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, insets.navPadding) }
    }

    private fun opaque(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)
}
