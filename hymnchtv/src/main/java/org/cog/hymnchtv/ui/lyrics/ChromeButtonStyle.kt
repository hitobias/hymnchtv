package org.cog.hymnchtv.ui.lyrics

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import org.cog.hymnchtv.reading.background.UiTokens

/**
 * Paints the two lyrics toolbar capsules from [UiTokens] via [ChromeCapsuleColors] (spec rev 3 sections 2-4): a rounded
 * translucent plate with a 2dp shadow, borderless buttons with a round ripple, `onSurface` icons and text, the disabled
 * colour for an unusable item. Called from the single propagation point (ContentHandler.applyReadingTheme).
 */
object ChromeButtonStyle {
    private const val TOP_RADIUS_DP = 24f
    private const val PILL_RADIUS_DP = 22f
    private const val ELEVATION_DP = 2f
    private const val RIPPLE_RADIUS_DP = 22

    @JvmStatic
    fun styleTopBar(bar: ViewGroup, tokens: UiTokens) = style(bar, ChromeCapsuleColors.from(tokens), TOP_RADIUS_DP)

    @JvmStatic
    fun styleBottomBar(bar: ViewGroup, tokens: UiTokens) = style(bar, ChromeCapsuleColors.from(tokens), PILL_RADIUS_DP)

    private fun style(bar: ViewGroup, colors: CapsuleColors, radiusDp: Float) {
        val density = bar.resources.displayMetrics.density
        bar.background = GradientDrawable().apply {
            setColor(colors.fill)
            cornerRadius = radiusDp * density
        }
        bar.elevation = ELEVATION_DP * density
        for (i in 0 until bar.childCount) {
            styleButton(bar.getChildAt(i), colors, density)
        }
    }

    private fun styleButton(view: View, colors: CapsuleColors, density: Float) {
        view.background = RippleDrawable(ColorStateList.valueOf(colors.ripple), null, null).apply {
            radius = (RIPPLE_RADIUS_DP * density).toInt()
        }
        val tint = contentColors(colors)
        when (view) {
            is TextView -> {
                view.setTextColor(tint)
                TextViewCompat.setCompoundDrawableTintList(view, tint)
            }
            is ImageView -> ImageViewCompat.setImageTintList(view, tint)
        }
    }

    @JvmStatic
    fun contentColors(colors: CapsuleColors): ColorStateList = ColorStateList(
        arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
        intArrayOf(colors.disabled, colors.content),
    )
}
