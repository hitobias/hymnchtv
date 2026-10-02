package org.cog.hymnchtv.reading

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import org.cog.hymnchtv.R
import timber.log.Timber
import java.util.concurrent.Executors

/** HymnalKai SC/TC for the lyrics views (plan A2); loaded once, off the main thread when possible. */
object LyricsTypefaces {
    private var simplified: Typeface? = null
    private var traditional: Typeface? = null
    private val loader = Executors.newSingleThreadExecutor { r -> Thread(r, "lyrics-font").apply { isDaemon = true } }

    /** Starts decoding both fonts in the background so the first lyrics page does not wait for 3.5 MB of TTF. */
    @JvmStatic
    fun preload(context: Context) {
        val app = context.applicationContext
        loader.execute {
            get(app, false)
            get(app, true)
        }
    }

    /** The font for one script, or null if it cannot be loaded (the caller keeps the system font). */
    @JvmStatic
    @Synchronized
    fun get(context: Context, traditionalScript: Boolean): Typeface? {
        val cached = if (traditionalScript) traditional else simplified
        if (cached != null) return cached
        val res = if (traditionalScript) R.font.hymnal_kai_tc else R.font.hymnal_kai_sc
        val loaded = try {
            ResourcesCompat.getFont(context.applicationContext, res)
        } catch (e: Resources.NotFoundException) {
            Timber.e(e, "Lyrics font %s could not be loaded", if (traditionalScript) "TC" else "SC")
            null
        }
        if (traditionalScript) traditional = loaded else simplified = loaded
        return loaded
    }
}
