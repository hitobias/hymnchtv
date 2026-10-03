package org.cog.hymnchtv.ui.player

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.GlassMode

/**
 * A rounded panel with a [BlurView] as its first layer and its content above it (frosted-player spec section 6: the
 * BlurView is never a row of a LinearLayout). Finds the activity's [BlurTarget] when attached, follows its own visibility
 * and clips the blur to the rounded outline.
 */
open class GlassFrameLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {
    /** Null when the layout has no [BlurView] child. */
    var glass: GlassBackdrop? = null
        private set

    private val density = resources.displayMetrics.density

    override fun onFinishInflate() {
        super.onFinishInflate()
        val blur = (0 until childCount).map(::getChildAt).filterIsInstance<BlurView>().firstOrNull()
        glass = blur?.let { GlassBackdrop(it, BLUR_RADIUS_DP * density) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        glass?.bind(rootView.findViewById<BlurTarget>(R.id.blurTarget))
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        glass?.setShown(isVisible)
    }

    /**
     * Paints the glass: [tint] is the rounded background (the whole look on API 24-30, behind the blur elsewhere) and the
     * colour laid over the blur. [GlassMode.OPAQUE] and [GlassMode.FALLBACK] run no blur.
     */
    fun applyGlass(tint: Int, mode: GlassMode, radiusDp: Float) {
        background = GradientDrawable().apply {
            setColor(tint)
            cornerRadius = radiusDp * density
        }
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        glass?.configure(tint, mode)
    }

    companion object {
        const val BLUR_RADIUS_DP = 24f
    }
}
