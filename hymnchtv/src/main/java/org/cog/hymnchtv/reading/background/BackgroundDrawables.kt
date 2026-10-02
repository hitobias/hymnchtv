package org.cog.hymnchtv.reading.background

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import org.cog.hymnchtv.R
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Maps BackgroundPreset to its drawable and name (plan A2). */
object BackgroundDrawables {
    /** A new drawable for [preset]; rays and stars are drawn in code on top of the generated layer-list. */
    @JvmStatic
    fun create(context: Context, preset: BackgroundPreset): Drawable {
        val base = AppCompatResources.getDrawable(context, drawableRes(preset)) ?: ColorDrawable(preset.baseColor)
        val extra: Drawable? = when (preset) {
            BackgroundPreset.RAYS -> RaysDrawable()
            BackgroundPreset.STARRY -> StarsDrawable(context.resources.displayMetrics.density)
            else -> null
        }
        return if (extra == null) base else LayerDrawable(arrayOf(base, extra))
    }

    @DrawableRes
    @JvmStatic
    fun drawableRes(preset: BackgroundPreset): Int = when (preset) {
        BackgroundPreset.XUAN -> R.drawable.bg_xuan
        BackgroundPreset.LINEN -> R.drawable.bg_linen
        BackgroundPreset.PARCHMENT -> R.drawable.bg_parchment
        BackgroundPreset.MIST -> R.drawable.bg_mist
        BackgroundPreset.DAWN -> R.drawable.bg_dawn
        BackgroundPreset.SKY -> R.drawable.bg_sky
        BackgroundPreset.HARVEST -> R.drawable.bg_harvest
        BackgroundPreset.DUSK -> R.drawable.bg_dusk
        BackgroundPreset.MEADOW -> R.drawable.bg_meadow
        BackgroundPreset.STAFF -> R.drawable.bg_staff
        BackgroundPreset.OLIVE -> R.drawable.bg_olive
        BackgroundPreset.WHEAT -> R.drawable.bg_wheat
        BackgroundPreset.DOVE -> R.drawable.bg_dove
        BackgroundPreset.RAYS -> R.drawable.bg_rays
        BackgroundPreset.WATER -> R.drawable.bg_water
        BackgroundPreset.VINE -> R.drawable.bg_vine
        BackgroundPreset.NIGHTREAD -> R.drawable.bg_nightread
        BackgroundPreset.STARRY -> R.drawable.bg_starry
        BackgroundPreset.DEEPSEA -> R.drawable.bg_deepsea
        BackgroundPreset.INK -> R.drawable.bg_ink
    }

    @StringRes
    @JvmStatic
    fun nameRes(preset: BackgroundPreset): Int = when (preset) {
        BackgroundPreset.XUAN -> R.string.bg_name_xuan
        BackgroundPreset.LINEN -> R.string.bg_name_linen
        BackgroundPreset.PARCHMENT -> R.string.bg_name_parchment
        BackgroundPreset.MIST -> R.string.bg_name_mist
        BackgroundPreset.DAWN -> R.string.bg_name_dawn
        BackgroundPreset.SKY -> R.string.bg_name_sky
        BackgroundPreset.HARVEST -> R.string.bg_name_harvest
        BackgroundPreset.DUSK -> R.string.bg_name_dusk
        BackgroundPreset.MEADOW -> R.string.bg_name_meadow
        BackgroundPreset.STAFF -> R.string.bg_name_staff
        BackgroundPreset.OLIVE -> R.string.bg_name_olive
        BackgroundPreset.WHEAT -> R.string.bg_name_wheat
        BackgroundPreset.DOVE -> R.string.bg_name_dove
        BackgroundPreset.RAYS -> R.string.bg_name_rays
        BackgroundPreset.WATER -> R.string.bg_name_water
        BackgroundPreset.VINE -> R.string.bg_name_vine
        BackgroundPreset.NIGHTREAD -> R.string.bg_name_nightread
        BackgroundPreset.STARRY -> R.string.bg_name_starry
        BackgroundPreset.DEEPSEA -> R.string.bg_name_deepsea
        BackgroundPreset.INK -> R.string.bg_name_ink
    }

    @StringRes
    @JvmStatic
    fun nameRes(choice: BackgroundChoice): Int = when (choice) {
        BackgroundChoice.Photo -> R.string.bg_photo
        is BackgroundChoice.Preset -> nameRes(choice.preset)
    }

    /** Rounded panel behind text for [ReadingPalette.backdropColor]; null when the palette needs none. */
    @JvmStatic
    fun backdrop(context: Context, palette: ReadingPalette): Drawable? {
        if (palette.backdropColor == 0) return null
        return GradientDrawable().apply {
            setColor(palette.backdropColor)
            cornerRadius = 10f * context.resources.displayMetrics.density
        }
    }
}

/**
 * 光芒: CSS repeating-conic-gradient(from 0deg at 50% -10%, white .55 0-4deg, clear 4-12deg).
 * Drawn as one Path, so no screen-sized bitmap cache (unlike a full-screen VectorDrawable).
 */
class RaysDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RAY_COLOR }
    private val path = Path()

    override fun onBoundsChange(bounds: Rect) {
        path.reset()
        val cx = bounds.exactCenterX()
        val cy = bounds.top - bounds.height() * 0.1f
        val reach = 2f * hypot(bounds.width().toFloat(), bounds.height().toFloat())
        var deg = 0
        while (deg < 360) {
            // CSS conic angles: 0deg points up, clockwise
            path.moveTo(cx, cy)
            path.lineTo(cx + reach * sin(rad(deg)), cy - reach * cos(rad(deg)))
            path.lineTo(cx + reach * sin(rad(deg + WIDTH_DEG)), cy - reach * cos(rad(deg + WIDTH_DEG)))
            path.close()
            deg += PERIOD_DEG
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = (RAY_COLOR ushr 24) * alpha / 255
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun rad(deg: Int): Float = Math.toRadians(deg.toDouble()).toFloat()

    private companion object {
        const val PERIOD_DEG = 12
        const val WIDTH_DEG = 4
        const val RAY_COLOR = 0x8DFFFFFF.toInt()   // white at .55
    }
}

/**
 * 星夜: five faint stars at the CSS positions; radius 1.5x the CSS value so they survive the fade-out.
 * The brightest star uses the STARRY overlay alpha from the registry (contrast-tested); the others keep the CSS ratios.
 */
class StarsDrawable(private val density: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var alphaScale = 1f
    private var glows: List<Glow> = emptyList()

    private class Glow(val cx: Float, val cy: Float, val radius: Float, val paint: Paint)

    override fun onBoundsChange(bounds: Rect) = rebuild()

    /** Gradients are built here, not in draw(), so drawing allocates nothing. */
    private fun rebuild() {
        val b = bounds
        glows = STARS.map { star ->
            val cx = b.left + b.width() * star.x
            val cy = b.top + b.height() * star.y
            val radius = star.radiusDp * density * 1.5f
            val alpha = (255 * star.relativeAlpha * MAX_ALPHA * alphaScale).toInt()
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = paint.colorFilter
                shader = RadialGradient(cx, cy, radius, Color.argb(alpha, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            Glow(cx, cy, radius, glowPaint)
        }
    }

    override fun draw(canvas: Canvas) {
        for (g in glows) canvas.drawCircle(g.cx, g.cy, g.radius, g.paint)
    }

    override fun setAlpha(alpha: Int) {
        alphaScale = alpha / 255f
        rebuild()
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        rebuild()
        invalidateSelf()
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private data class Star(val x: Float, val y: Float, val radiusDp: Float, val relativeAlpha: Float)

    private companion object {
        val MAX_ALPHA = BackgroundPreset.STARRY.overlays.maxOf { it.maxAlpha }
        // CSS alphas .8 .7 .6 .6 .5, relative to the brightest
        val STARS = listOf(
            Star(0.20f, 0.18f, 1.2f, 1.0f),
            Star(0.72f, 0.12f, 1.0f, 0.875f),
            Star(0.85f, 0.34f, 1.4f, 0.75f),
            Star(0.38f, 0.08f, 1.0f, 0.75f),
            Star(0.58f, 0.28f, 1.0f, 0.625f),
        )
    }
}
