package org.cog.hymnchtv.reading.background

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.ImageView
import org.cog.hymnchtv.R
import timber.log.Timber
import java.io.File

/** Shows a background in a full-size ImageView that sits behind the content (plan A2). */
object BackgroundApplier {
    /**
     * Applies [choice] and returns what is really on screen: a photo that cannot be decoded is replaced by
     * [fallback], so the caller's palette always matches the pixels.
     */
    @JvmStatic
    fun apply(target: ImageView, choice: BackgroundChoice, prefs: SharedPreferences, photo: File?, fallback: BackgroundPreset): BackgroundChoice {
        if (choice is BackgroundChoice.Preset) {
            showPreset(target, choice.preset)
            return choice
        }
        val metrics = target.resources.displayMetrics
        val bitmap = photo?.let { decodePhoto(it, metrics.widthPixels, metrics.heightPixels) }
        if (bitmap == null) {
            Timber.w("Background photo unreadable (%s); showing %s", photo, fallback.id)
            showPreset(target, fallback)
            return BackgroundChoice.Preset(fallback)
        }
        target.background = null
        target.scaleType = ImageView.ScaleType.CENTER_CROP
        target.setImageBitmap(bitmap)
        val dim = intPref(prefs, PhotoBackground.PREF_DIM, PhotoBackground.DIM_DEFAULT)
        target.setColorFilter(Color.argb(PhotoBackground.dimAlpha(dim), 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        val blur = intPref(prefs, PhotoBackground.PREF_BLUR, PhotoBackground.BLUR_DEFAULT)
        setBlur(target, PhotoBackground.blurRadiusPx(blur, metrics.density))
        return choice
    }

    /** Decodes [file] no larger than needed for [reqWidth] x [reqHeight]; null if it is not an image. */
    @JvmStatic
    fun decodePhoto(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoBackground.boundedSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return try {
            BitmapFactory.decodeFile(file.path, options)
        } catch (e: OutOfMemoryError) {
            Timber.w(e, "Background photo too large to decode: %s", file)
            null
        }
    }

    private fun showPreset(target: ImageView, preset: BackgroundPreset) {
        target.setImageDrawable(null)
        target.clearColorFilter()
        setBlur(target, 0f)
        target.background = BackgroundDrawables.create(target.context, preset)
    }

    private fun setBlur(target: ImageView, radiusPx: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            target.setRenderEffect(
                if (radiusPx > 0f) RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP) else null
            )
            // View has no getter for its RenderEffect; record the radius so tests can see what was applied
            target.setTag(R.id.tag_blur_radius, radiusPx)
        }
    }

    /** Blur radius last applied to [target] (0 when none or below API 31); for tests. */
    @JvmStatic
    fun appliedBlurRadius(target: ImageView): Float = (target.getTag(R.id.tag_blur_radius) as? Float) ?: 0f

    private fun intPref(prefs: SharedPreferences, key: String, default: Int): Int =
        runCatching { prefs.getInt(key, default) }.getOrDefault(default)
}
