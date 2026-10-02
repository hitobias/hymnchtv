package org.cog.hymnchtv.ui.titles

import android.content.Context
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.toc.YbCrossRef
import timber.log.Timber
import java.io.IOException

/**
 * [HymnTitleSource] over the bundled lyrics assets. Same algorithm as HymnToc.getHymnTitle and HistoryRecord:
 * 1. line 2 is the title, minus the category before the last "－";
 * 2. line 3, when it holds "（", contributes the "（…）" note such as "（英1）";
 * 3. YB numbers listed in the cross-reference table are read from the book they live in.
 *
 * [reader] returns an asset's text, or null when it does not exist (injected so unit tests read the asset directory).
 */
class AssetHymnTitles(
    private val reader: (String) -> String?,
    private val ybTable: () -> Map<Int, String> = { YbCrossRef.TABLE },
) : HymnTitleSource {

    override fun lookup(hymnType: String, hymnNo: Int): String? {
        val path = lyricsPath(hymnType, hymnNo) ?: return null
        val lines = (reader(path) ?: return null).split(LINE_BREAK)
        if (lines.size < 2) return null

        val title = lines[1].substringAfterLast(CATEGORY_SEPARATOR)
        val note = lines.getOrNull(2)?.let { third ->
            val idx = third.indexOf(NOTE_OPEN)
            if (idx == -1) "" else third.substring(idx)
        }.orEmpty()
        return title + note
    }

    private fun lyricsPath(hymnType: String, hymnNo: Int): String? {
        if (hymnNo < 1) return null
        if (hymnType == MainActivity.HYMN_YB) {
            // e.g. "bb876": the two-letter prefix names the book the lyrics live in
            val target = ybTable()[hymnNo]
            if (target != null) {
                val no = target.drop(PREFIX_LENGTH).toIntOrNull() ?: return null
                return lyricsPath(typeOfPrefix(target.take(PREFIX_LENGTH)) ?: return null, no)
            }
        }
        val prefix = prefixOf(hymnType) ?: return null
        return "lyrics_${prefix}_text/$prefix$hymnNo.txt"
    }

    companion object {
        private val LINE_BREAK = Regex("\r\n|\n")
        private const val CATEGORY_SEPARATOR = "－"
        private const val NOTE_OPEN = "（"
        private const val PREFIX_LENGTH = 2

        // Book type <-> asset prefix (lyrics_<prefix>_text/<prefix><no>.txt); constants are inlined, so no MainActivity class init
        private val PREFIXES = mapOf(
            MainActivity.HYMN_DB to "db",
            MainActivity.HYMN_BB to "bb",
            MainActivity.HYMN_ER to "er",
            MainActivity.HYMN_XB to "xb",
            MainActivity.HYMN_XG to "xg",
            MainActivity.HYMN_YB to "yb",
        )

        private fun prefixOf(hymnType: String): String? = PREFIXES[hymnType]

        private fun typeOfPrefix(prefix: String): String? = PREFIXES.entries.firstOrNull { it.value == prefix }?.key

        @JvmStatic
        fun from(context: Context): AssetHymnTitles {
            val assets = context.applicationContext.assets
            return AssetHymnTitles(reader = { path ->
                try {
                    assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                } catch (e: IOException) {
                    Timber.v("No lyrics asset %s: %s", path, e.message)
                    null
                }
            })
        }
    }
}
