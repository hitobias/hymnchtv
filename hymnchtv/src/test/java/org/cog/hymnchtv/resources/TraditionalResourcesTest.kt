package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards against Simplified text leaking into the Traditional UI: every Chinese string in the
 * default resources must have a values-b+zh+Hant counterpart that contains no Simplified-only glyph.
 */
class TraditionalResourcesTest {
    private val resDir = File("src/main/res")

    private val cjk = Regex("[\\u4e00-\\u9fff]")

    // Simplified-only glyphs that occur in this app's UI vocabulary
    private val simplifiedOnly = "诗歌颂咏补儿体对录设选关习项开显错误载级语页简码连线动无统称续议说请确认".toSet() - "歌".toSet()

    // Language names are intentionally identical in every locale
    private val localeNeutral = setOf("locale_chinese", "locale_chinese_hant")

    private fun strings(dir: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        File(resDir, dir).listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }
            .orEmpty().forEach { f ->
                val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f)
                val nodes = doc.getElementsByTagName("string")
                for (i in 0 until nodes.length) {
                    val n = nodes.item(i)
                    out[n.attributes.getNamedItem("name").nodeValue] = n.textContent
                }
            }
        return out
    }

    @Test
    fun displayNameKeysExistInHant() {
        val hant = strings("values-b+zh+Hant")
        listOf("db", "bb", "xb", "xg", "yb", "er").forEach {
            assertThat(hant).containsKey("hymn_type_name_$it")
        }
        listOf("media", "jiaochang", "changshi", "banzou").forEach {
            assertThat(hant).containsKey("media_type_name_$it")
        }
        assertThat(hant["hymn_type_name_db"]).isEqualTo("大本詩歌")
        assertThat(hant["media_type_name_media"]).isEqualTo("媒體")
    }

    @Test
    fun everyChineseDefaultStringHasTraditionalVariant() {
        val default = strings("values")
        val hant = strings("values-b+zh+Hant")
        val missing = default.filter { (k, v) -> cjk.containsMatchIn(v) && k !in hant && k !in localeNeutral }.keys
        assertThat(missing).isEmpty()
    }

    @Test
    fun traditionalStringsHaveNoSimplifiedGlyphs() {
        val offenders = strings("values-b+zh+Hant").filter { (_, v) -> v.any { it in simplifiedOnly } }
        assertThat(offenders).isEmpty()
    }
}
