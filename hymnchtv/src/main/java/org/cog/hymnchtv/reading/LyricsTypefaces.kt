package org.cog.hymnchtv.reading

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * HymnalKai SC/TC, Regular and the heavier Medium, for the lyrics views (plan A2). Each face (~1.5-2 MB) is decoded at most once, on a
 * background thread, and only when its script is first shown; the main thread never waits for a load.
 */
object LyricsTypefaces {
    /** Called on the main thread when a requested face is ready. */
    fun interface Callback {
        fun onReady(typeface: Typeface)
    }

    private val faces = arrayOfNulls<Typeface>(4)
    private val attempted = BooleanArray(4)
    private val loader = Executors.newSingleThreadExecutor { r -> Thread(r, "lyrics-font").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())

    private fun slot(traditionalScript: Boolean, medium: Boolean) = (if (traditionalScript) 1 else 0) + (if (medium) 2 else 0)

    /** Starts decoding one face in the background (e.g. the script about to be shown). */
    @JvmStatic
    @JvmOverloads
    fun preload(context: Context, traditionalScript: Boolean, medium: Boolean = false) {
        val app = context.applicationContext
        loader.execute { load(app, traditionalScript, medium) }
    }

    /** The face if it is already loaded, else null; never blocks. */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun peek(traditionalScript: Boolean, medium: Boolean = false): Typeface? = faces[slot(traditionalScript, medium)]

    /** Delivers the face on the main thread once loaded; never called if it cannot be loaded (system font stays). */
    @JvmStatic
    @JvmOverloads
    fun request(context: Context, traditionalScript: Boolean, medium: Boolean = false, callback: Callback) {
        peek(traditionalScript, medium)?.let { callback.onReady(it); return }
        val app = context.applicationContext
        loader.execute {
            load(app, traditionalScript, medium)?.let { main.post { callback.onReady(it) } }
        }
    }

    /**
     * A failed load is remembered so it is not retried on every page. The locks are short (state only): the
     * multi-MB font decode runs outside them so [peek] on the main thread never waits for it.
     */
    private fun load(context: Context, traditionalScript: Boolean, medium: Boolean): Typeface? {
        val i = slot(traditionalScript, medium)
        synchronized(this) {
            if (attempted[i]) return faces[i]
            attempted[i] = true
        }
        val res = (LyricsFaceSpec.choose(LyricsFont.KAI, if (medium) LyricsWeight.MEDIUM else LyricsWeight.REGULAR, traditionalScript) as LyricsFaceSpec.Kai).res
        val face = try {
            ResourcesCompat.getFont(context, res)
        } catch (e: Resources.NotFoundException) {
            Timber.e(e, "Lyrics font %s%s could not be loaded", if (traditionalScript) "TC" else "SC", if (medium) " medium" else "")
            null
        }
        synchronized(this) { faces[i] = face }
        return face
    }

    /** Tests only: forget one face so the next request loads it again in the background (a "first use" on demand). */
    @VisibleForTesting
    @JvmStatic
    @Synchronized
    fun forgetForTest(traditionalScript: Boolean, medium: Boolean) {
        val i = slot(traditionalScript, medium)
        faces[i] = null
        attempted[i] = false
    }

    /** The device font for a [LyricsFaceSpec.System] weight; Medium (500) needs API 28, older devices show it as bold. */
    @JvmStatic
    fun systemFace(weight: Int, sdkInt: Int = Build.VERSION.SDK_INT): Typeface = when {
        weight < 500 -> Typeface.DEFAULT
        weight == 500 && sdkInt >= Build.VERSION_CODES.P -> Typeface.create(Typeface.DEFAULT, 500, false)
        else -> Typeface.DEFAULT_BOLD
    }
}
