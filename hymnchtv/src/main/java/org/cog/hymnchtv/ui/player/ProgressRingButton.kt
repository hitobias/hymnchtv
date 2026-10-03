package org.cog.hymnchtv.ui.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * The capsule's play/pause key: a 40dp `accent` disc inside a 3dp progress ring (`accent` over a `surfaceTone` track),
 * in a 48dp touch target. The icon is the view's image, tinted `onAccent` by the controller.
 */
class ProgressRingButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {
    private val density = resources.displayMetrics.density
    private val disc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = RING_DP * density }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = RING_DP * density; strokeCap = Paint.Cap.ROUND
    }
    private val ringBounds = RectF()

    /** 0..1 */
    var progress: Float = 0f
        set(value) {
            val v = value.coerceIn(0f, 1f)
            if (v != field) {
                field = v
                invalidate()
            }
        }

    fun applyColors(accent: Int, onAccent: Int, trackColor: Int) {
        disc.color = accent
        arc.color = accent
        track.color = trackColor
        setColorFilter(onAccent)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val ringRadius = RING_OUTER_DP * density - arc.strokeWidth / 2f
        ringBounds.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)
        canvas.drawCircle(cx, cy, DISC_DP / 2f * density, disc)
        canvas.drawCircle(cx, cy, ringRadius, track)
        if (progress > 0f) canvas.drawArc(ringBounds, -90f, 360f * progress, false, arc)
        super.onDraw(canvas)
    }

    private companion object {
        const val RING_DP = 3f
        const val RING_OUTER_DP = 24f
        const val DISC_DP = 40f
    }
}
