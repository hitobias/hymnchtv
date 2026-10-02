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
import java.util.concurrent.Executors

/** Shows a background in a full-size ImageView that sits behind the content (plan A2). */
object BackgroundApplier {
    private val decoder = Executors.newSingleThreadExecutor { r -> Thread(r, "bg-decode").apply { isDaemon = true } }

    /**
     * Applies [choice] and returns what is really on screen: a photo that cannot be decoded is replaced by
     * [fallback], so the caller's palette always matches the pixels.
     */
    @JvmStatic
    fun apply(target: ImageView, choice: BackgroundChoice, prefs: SharedPreferences, photo: File?, fallback: BackgroundPreset): BackgroundChoice {
        target.setTag(R.id.tag_bg_request, null) // a pending photo decode for an older choice must not land
        if (choice is BackgroundChoice.Preset) {
            showPreset(target, choice.preset)
            return choice
        }
        val metrics = target.resources.displayMetrics
        // Header-only probe keeps the contract: an unreadable photo is replaced by [fallback] right away
        if (photo == null || !isDecodable(photo)) {
            Timber.w("Background photo unreadable (%s); showing %s", photo, fallback.id)
            showPreset(target, fallback)
            return BackgroundChoice.Preset(fallback)
        }
        // The fallback colour shows while the photo decodes off the main thread
        showPreset(target, fallback)
        val dim = intPref(prefs, PhotoBackground.PREF_DIM, PhotoBackground.DIM_DEFAULT)
        val blur = intPref(prefs, PhotoBackground.PREF_BLUR, PhotoBackground.BLUR_DEFAULT)
        val token = Any()
        target.setTag(R.id.tag_bg_request, token)
        decoder.execute {
            val bitmap = decodePhoto(photo, metrics.widthPixels, metrics.heightPixels)
            target.post {
                // Stale (a newer apply ran) or the view went away: drop the result
                if (target.getTag(R.id.tag_bg_request) !== token || !target.isAttachedToWindow) return@post
                if (bitmap == null) {
                    Timber.w("Background photo failed to decode (%s); keeping %s", photo, fallback.id)
                    return@post
                }
                target.background = null
                target.scaleType = ImageView.ScaleType.CENTER_CROP
                target.setImageBitmap(bitmap)
                target.setColorFilter(Color.argb(PhotoBackground.dimAlpha(dim), 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
                setBlur(target, PhotoBackground.blurRadiusPx(blur, metrics.density))
            }
        }
        return choice
    }

    private fun isDecodable(file: File): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    /** Decodes [file] within the pixel budget for [reqWidth] x [reqHeight]; null if it is not an image. */
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
