package org.cog.hymnchtv.ui.toc

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_ER
import org.cog.hymnchtv.MainActivity.HYMN_XB
import org.cog.hymnchtv.MainActivity.HYMN_XG
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.junit.Test
import java.io.File

/**
 * TocBuilder must produce exactly what the original HymnToc activity built. The fixtures were dumped from that
 * original code before it was extracted (see test/resources/toc_fixtures: a category line, then one tab-indented line per item).
 */
class TocBuilderTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })

    private fun reader(path: String): String? = File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    private fun fixture(code: String, name: String): LinkedHashMap<String, List<String>> {
        val text = checkNotNull(javaClass.getResourceAsStream("/toc_fixtures/$code-$name.txt")) { "missing fixture $code-$name" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        val result = LinkedHashMap<String, MutableList<String>>()
        var current: MutableList<String>? = null
        for (line in text.split('\n')) {
            when {
                line.startsWith('\t') -> checkNotNull(current).add(line.substring(1))
                line.isNotEmpty() -> current = mutableListOf<String>().also { result[line] = it }
            }
        }
        return LinkedHashMap(result)
    }

    private fun assertParity(type: String, code: String, page: String, name: String) {
        // The Bb/Er skip ranges live in HymnNoValidate, whose static init needs android.util.Range (not in unit tests):
        // pass the same values explicitly; TocBuilderAssetTest (instrumented) checks the defaults.
        val actual = TocBuilder.build(::reader, type, page, TEST_BB_LIMIT, TEST_ER_LIMIT)
        val expected = fixture(code, name)
        assertThat(actual.keys.toList()).containsExactlyElementsIn(expected.keys).inOrder()
        for ((category, items) in expected) {
            assertThat(actual.getValue(category)).containsExactlyElementsIn(items).inOrder()
        }
    }

    @Test fun dbCategory() = assertParity(HYMN_DB, "db", CATEGORY, "CATEGORY")
    @Test fun dbPinyin() = assertParity(HYMN_DB, "db", PINYIN, "PINYIN")
    @Test fun dbStroke() = assertParity(HYMN_DB, "db", STROKE, "STROKE")
    @Test fun dbEnglish() = assertParity(HYMN_DB, "db", ENGLISH, "ENGLISH")
    @Test fun bbCategory() = assertParity(HYMN_BB, "bb", CATEGORY, "CATEGORY")
    @Test fun bbPinyin() = assertParity(HYMN_BB, "bb", PINYIN, "PINYIN")
    @Test fun bbEnglish() = assertParity(HYMN_BB, "bb", ENGLISH, "ENGLISH")
    @Test fun erCategory() = assertParity(HYMN_ER, "er", CATEGORY, "CATEGORY")
    @Test fun erStroke() = assertParity(HYMN_ER, "er", STROKE, "STROKE")
    @Test fun erEnglishIsEmpty() = assertParity(HYMN_ER, "er", ENGLISH, "ENGLISH")
    @Test fun xbCategory() = assertParity(HYMN_XB, "xb", CATEGORY, "CATEGORY")
    @Test fun xgCategory() = assertParity(HYMN_XG, "xg", CATEGORY, "CATEGORY")
    @Test fun xgEnglish() = assertParity(HYMN_XG, "xg", ENGLISH, "ENGLISH")
    @Test fun ybCategory() = assertParity(HYMN_YB, "yb", CATEGORY, "CATEGORY")
    @Test fun ybPinyin() = assertParity(HYMN_YB, "yb", PINYIN, "PINYIN")

    @Test
    fun hymnNoOfReadsTheNumberAfterHashOrBeforeColon() {
        assertThat(TocBuilder.hymnNoOf("一切全奉献 #337")).isEqualTo(337)
        assertThat(TocBuilder.hymnNoOf("0012: 标题")).isEqualTo(12)
        assertThat(TocBuilder.hymnNoOf("1: 神就是爱 (bb876)")).isEqualTo(1)
        assertThat(TocBuilder.hymnNoOf("no number")).isNull()
        assertThat(TocBuilder.hymnNoOf("")).isNull()
    }

    @Test
    fun missingAssetsGiveAnEmptyToc() {
        assertThat(TocBuilder.build({ null }, HYMN_DB, STROKE, TEST_BB_LIMIT, TEST_ER_LIMIT)).isEmpty()
    }

    private companion object {
        const val CATEGORY = "诗歌类别"
        const val STROKE = "笔画索引"
        const val PINYIN = "拼音索引"
        const val ENGLISH = "英中对照"
        val TEST_BB_LIMIT = intArrayOf(38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006)
        val TEST_ER_LIMIT = intArrayOf(18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233)
    }
}
