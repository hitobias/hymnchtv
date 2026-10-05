package org.cog.hymnchtv.share

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.junit.Test
import java.io.File

class ShareAssetsTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })

    private val books = listOf(
        MainActivity.HYMN_DB to "db", MainActivity.HYMN_BB to "bb", MainActivity.HYMN_ER to "er",
        MainActivity.HYMN_XB to "xb", MainActivity.HYMN_XG to "xg", MainActivity.HYMN_YB to "yb",
    )

    @Test
    fun youthHymnsUseTheirOwnDirectories() {
        val p = ShareAssets.paths(MainActivity.HYMN_YB, 102)!!
        assertThat(p.score).isEqualTo("lyrics_yb_score/yb102.png")
        assertThat(p.lyrics).isEqualTo("lyrics_yb_text/yb102.txt")
        assertThat(p.scoreName).isEqualTo("yb102.png")
        assertThat(p.lyricsName).isEqualTo("yb102.txt")
        assertThat(File(assets, p.score).isFile).isTrue()
        assertThat(File(assets, p.lyrics).isFile).isTrue()
    }

    @Test
    fun childrensScoresAreNamedByNumberOnly() {
        val p = ShareAssets.paths(MainActivity.HYMN_ER, 1)!!
        assertThat(p.score).isEqualTo("lyrics_er_score/1.png")
        assertThat(p.scoreName).isEqualTo("er1.png")
        assertThat(File(assets, p.score).isFile).isTrue()
        assertThat(File(assets, p.lyrics).isFile).isTrue()
    }

    @Test
    fun everyLyricsFileOfEveryBookIsFound() {
        for ((type, book) in books) {
            val numbers = File(assets, "lyrics_${book}_text").list()!!
                .filter { it.startsWith(book) && it.endsWith(".txt") }
                .mapNotNull { it.removePrefix(book).removeSuffix(".txt").toIntOrNull() }
            assertThat(numbers).isNotEmpty()
            for (n in numbers) {
                assertThat(File(assets, ShareAssets.paths(type, n)!!.lyrics).isFile).isTrue()
            }
        }
    }

    @Test
    fun unknownBookOrNumberGivesNothing() {
        assertThat(ShareAssets.paths("hymn_zz", 1)).isNull()
        assertThat(ShareAssets.paths(null, 1)).isNull()
        assertThat(ShareAssets.paths(MainActivity.HYMN_DB, 0)).isNull()
    }
}
