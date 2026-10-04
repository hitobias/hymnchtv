package org.cog.hymnchtv.ui

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class FavoriteStringsTest {
    private val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")) { "hymnchtv.resDir not set" })
    private val keys = listOf(
        "fav_add", "fav_remove", "fav_added", "fav_removed", "fav_undo",
        "fav_tab_recent", "fav_tab_favorites", "fav_empty", "fav_marked",
    )

    private fun load(dir: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(res, "$dir/strings_favorites.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate {
            val n = nodes.item(it)
            n.attributes.getNamedItem("name").nodeValue to n.textContent
        }
    }

    @Test
    fun everyKeyExistsAndIsNonEmptyInAllThreeLocales() {
        for (dir in listOf("values", "values-zh", "values-b+zh+Hant")) {
            val strings = load(dir)
            for (key in keys) {
                assertWithMessage("$dir/$key").that(strings[key]).isNotEmpty()
            }
        }
    }

    @Test
    fun emptyHintMatchesSpecVerbatim() {
        assertThat(load("values-b+zh+Hant")["fav_empty"]).isEqualTo("還沒有收藏的詩歌。在歌詞頁右上角「更多」選單選「收藏」即可加入。")
        assertThat(load("values-zh")["fav_empty"]).isEqualTo("还没有收藏的诗歌。在歌词页右上角“更多”菜单选“收藏”即可加入。")
        assertThat(load("values")["fav_empty"]).isEqualTo("No favourites yet. On a lyrics page, open the More menu and choose Add to favourites.")
    }
}
