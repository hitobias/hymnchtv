package org.cog.hymnchtv.ui.player

import android.graphics.Color
import android.os.Build
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView
import org.cog.hymnchtv.reading.background.GlassMode

/**
 * The frosted layer behind the player card or capsule (frosted-player spec sections 2 and 6). Drives one [BlurView]:
 * on API 31+ it blurs the [BlurTarget] (lyrics pager and background) with RenderEffect while the panel is shown and the
 * glass is blurred; below that, or while hidden or opaque, it is off and no blur work or bitmap exists (API 24-30 keep
 * the flat translucent fallback colour that the panel paints itself).
 */
class GlassBackdrop(private val view: BlurView, private val radiusPx: Float) {
    /** True when this device can blur (API 31+); below, the [BlurView] is never set up and stays gone. */
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** True while the blur is really running. */
    var blurActive = false
        private set

    var tint = Color.TRANSPARENT
        private set

    var mode = GlassMode.FALLBACK
        private set

    private var target: BlurTarget? = null
    private var shown = false

    init {
        if (!supported) view.visibility = android.view.View.GONE
    }

    /** Connects the blur to the lyrics layer; a second call with the same target does nothing. */
    fun bind(target: BlurTarget?) {
        if (!supported || target == null || this.target === target) return
        this.target = target
        // scale factor 1: the radius is then the real radius; no noise texture
        view.setupWith(target, 1f, false).setBlurRadius(radiusPx).setOverlayColor(tint)
        blurActive = false
        refresh(force = true)
    }

    /** The glass colour drawn over the blur, and how it is to be drawn. */
    fun configure(tint: Int, mode: GlassMode) {
        this.tint = tint
        this.mode = mode
        if (target != null) view.setOverlayColor(tint)
        refresh(force = false)
    }

    /** Whether the panel is on screen (visibility of it and its parents, aggregated). */
    fun setShown(shown: Boolean) {
        this.shown = shown
        refresh(force = false)
    }

    private fun refresh(force: Boolean) {
        val on = supported && target != null && shown && mode == GlassMode.BLUR
        if (on == blurActive && !force) return
        blurActive = on
        if (target != null) {
            // The library's own pre-draw hook keeps the blur in step with the hierarchy while auto-update is on
            view.setBlurAutoUpdate(on)
            view.setBlurEnabled(on)
        }
    }
}
