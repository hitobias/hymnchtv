package org.cog.hymnchtv.toc

import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import org.cog.hymnchtv.HymnsApp
import timber.log.Timber
import java.io.IOException

/**
 * YB (青年诗歌) numbers whose lyrics live in another book, e.g. 1 -> "bb876". Replaces
 * MainActivity.createYbXTable(): the table loads lazily and thread-safely on first use, so it is also
 * correct when the process is restored straight into ContentHandler (MainActivity.onCreate never ran).
 */
object YbCrossRef {
    const val ASSET = "lyrics_toc/toc_yb_toc.txt"

    private val WHITESPACE = Regex("\\s+")

    /** Read-only. The first access loads the asset (blocking briefly) unless [prewarm] already did. */
    @JvmField
    val TABLE: Map<Int, String> = LazyMap { load() }

    /**
     * Call on a background thread at startup so the main thread usually does not pay for the first load.
     * It still can (e.g. a YB hymn opened before the warm-up ran); that synchronous load is small and measured
     * by YbCrossRefAssetTest.mainThreadFirstUseCost (see the measurements doc).
     */
    @JvmStatic
    fun prewarm() {
        TABLE.isEmpty()
    }

    /** Lines look like "#0001 神就是爱 #bb876"; YB-to-YB entries and malformed lines are skipped. */
    @JvmStatic
    fun parse(text: String): Map<Int, String> =
        text.lineSequence().mapNotNull(::parseLine).toMap()

    private fun parseLine(line: String): Pair<Int, String>? {
        val tokens = line.trim().split(WHITESPACE)
        if (tokens.size < 3) return null
        val number = tokens[0].removePrefix("#").toIntOrNull() ?: return null
        val target = tokens[2].removePrefix("#")
        if (target.isEmpty() || target.startsWith("yb")) return null
        return number to target
    }

    private fun load(): Map<Int, String> {
        val start = SystemClock.elapsedRealtime()
        val table = loadFromAssets()
        Timber.i("perf: YB cross-reference loaded (%d entries) in %d ms on %s",
            table.size, SystemClock.elapsedRealtime() - start, Thread.currentThread().name)
        return table
    }

    /** Reads and parses the asset every call; [TABLE] calls it once. Visible for the first-use cost test. */
    @VisibleForTesting
    internal fun loadFromAssets(): Map<Int, String> = try {
        HymnsApp.getAppResources().assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { parse(it.readText()) }
    } catch (e: IOException) {
        Timber.w(e, "YB cross-reference not available: %s", ASSET)
        emptyMap()
    }
}
