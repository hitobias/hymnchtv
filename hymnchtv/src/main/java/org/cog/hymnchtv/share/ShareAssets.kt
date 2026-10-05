package org.cog.hymnchtv.share

import org.cog.hymnchtv.MainActivity

/**
 * What "share" sends for one hymn: page 1 of the score (missing for most youth hymns) and the lyrics, as asset paths,
 * plus the file names they get in cacheDir/share/. The lyrics are the Simplified text, as before 1.4.1.
 */
object ShareAssets {
    data class Paths(val score: String, val lyrics: String, val scoreName: String, val lyricsName: String)

    private val BOOKS = mapOf(
        MainActivity.HYMN_DB to "db", MainActivity.HYMN_BB to "bb", MainActivity.HYMN_ER to "er",
        MainActivity.HYMN_XB to "xb", MainActivity.HYMN_XG to "xg", MainActivity.HYMN_YB to "yb",
    )

    /** @return the paths of [hymnType] [hymnNo], or null for an unknown book or a number below 1 */
    @JvmStatic
    fun paths(hymnType: String?, hymnNo: Int): Paths? {
        val book = BOOKS[hymnType] ?: return null
        if (hymnNo < 1) return null
        // children's hymns name their score pages by the number only (lyrics_er_score/1.png)
        val scoreAsset = if (book == "er") "$hymnNo.png" else "$book$hymnNo.png"
        return Paths(
            score = "lyrics_${book}_score/$scoreAsset",
            lyrics = "lyrics_${book}_text/$book$hymnNo.txt",
            scoreName = "$book$hymnNo.png",
            lyricsName = "$book$hymnNo.txt",
        )
    }
}
