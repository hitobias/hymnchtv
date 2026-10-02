package org.cog.hymnchtv.reading

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import org.cog.hymnchtv.R
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * HymnalKai SC/TC for the lyrics views (plan A2). Each face (~1.5-2 MB) is decoded at most once, on a
 * background thread, and only when its script is first shown; the main thread never waits for a load.
 */
object LyricsTypefaces {
    /** Called on the main thread when a requested face is ready. */
    fun interface Callback {
        fun onReady(typeface: Typeface)
    }

    private val faces = arrayOfNulls<Typeface>(2)
    private val attempted = BooleanArray(2)
    private val loader = Executors.newSingleThreadExecutor { r -> Thread(r, "lyrics-font").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())

    private fun slot(traditionalScript: Boolean) = if (traditionalScript) 1 else 0

    /** Starts decoding one face in the background (e.g. the script about to be shown). */
    @JvmStatic
    fun preload(context: Context, traditionalScript: Boolean) {
        val app = context.applicationContext
        loader.execute { load(app, traditionalScript) }
    }

    /** The face if it is already loaded, else null; never blocks. */
    @JvmStatic
    @Synchronized
    fun peek(traditionalScript: Boolean): Typeface? = faces[slot(traditionalScript)]

    /** Delivers the face on the main thread once loaded; never called if it cannot be loaded (system font stays). */
    @JvmStatic
    fun request(context: Context, traditionalScript: Boolean, callback: Callback) {
        peek(traditionalScript)?.let { callback.onReady(it); return }
        val app = context.applicationContext
        loader.execute {
            load(app, traditionalScript)?.let { main.post { callback.onReady(it) } }
        }
    }

    /**
     * A failed load is remembered so it is not retried on every page. The locks are short (state only): the
     * multi-MB font decode runs outside them so [peek] on the main thread never waits for it.
     */
    private fun load(context: Context, traditionalScript: Boolean): Typeface? {
        val i = slot(traditionalScript)
        synchronized(this) {
            if (attempted[i]) return faces[i]
            attempted[i] = true
        }
        val res = if (traditionalScript) R.font.hymnal_kai_tc else R.font.hymnal_kai_sc
        val face = try {
            ResourcesCompat.getFont(context, res)
        } catch (e: Resources.NotFoundException) {
            Timber.e(e, "Lyrics font %s could not be loaded", if (traditionalScript) "TC" else "SC")
            null
        }
        synchronized(this) { faces[i] = face }
        return face
    }
}
