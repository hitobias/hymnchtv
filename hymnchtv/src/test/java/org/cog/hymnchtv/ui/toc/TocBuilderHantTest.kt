package org.cog.hymnchtv.ui.toc

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.HymnToc
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.cog.hymnchtv.lyrics.HantVariant
import org.junit.Test
import java.io.File

/** The table of contents in Traditional Chinese: same structure and order as the Simplified one, only the characters differ. */
class TocBuilderHantTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })

    private fun reader(path: String): String? = File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8)

    private fun build(type: String, page: String, variant: HantVariant?) =
        TocBuilder.build(::reader, type, page, TEST_BB_LIMIT, TEST_ER_LIMIT, variant)

    @Test
    fun categoryNamesAreTraditional() {
        val simplified = build(HYMN_DB, HymnToc.TOC_CATEGORY, null)
        val hant = build(HYMN_DB, HymnToc.TOC_CATEGORY, HantVariant.TW)
        assertThat(simplified.keys).contains("颂三一神")
        assertThat(hant.keys).contains("頌三一神")
        assertThat(hant.keys).contains("聖靈豐滿")
        assertThat(hant.keys).doesNotContain("圣灵丰满")
        assertThat(hant.keys.size).isEqualTo(simplified.keys.size)
    }

    @Test
    fun categoryItemsAreTraditionalWithTheSameNumbersAndCounts() {
        val simplified = build(HYMN_DB, HymnToc.TOC_CATEGORY, null).values.toList()
        val hant = build(HYMN_DB, HymnToc.TOC_CATEGORY, HantVariant.TW).values.toList()
        assertThat(hant.map { it.size }).isEqualTo(simplified.map { it.size })
        assertThat(hant.first().first()).isEqualTo("0001: 祂的計劃（英1）")
        assertThat(simplified.first().first()).isEqualTo("0001: 祂的计划（英1）")
        // numbers survive the conversion
        val numbers = { l: List<List<String>> -> l.flatten().map { it.substringBefore(':') } }
        assertThat(numbers(hant)).isEqualTo(numbers(simplified))
    }

    @Test
    fun pinyinGroupsAndOrderAreUnchangedOnlyTheCharactersConvert() {
        val simplified = build(HYMN_DB, HymnToc.TOC_PINYIN, null)
        val hant = build(HYMN_DB, HymnToc.TOC_PINYIN, HantVariant.TW)
        // letters A..Z; the "（first characters）" suffix is shown in Traditional
        assertThat(hant.keys.map { it.takeWhile { c -> c != '（' } }).containsExactlyElementsIn(simplified.keys.map { it.takeWhile { c -> c != '（' } }).inOrder()
        // same hymn numbers in the same order inside every group: sorting never depends on the shown script
        for ((s, h) in simplified.values.zip(hant.values)) {
            assertThat(h.map(TocBuilder::hymnNoOf)).containsExactlyElementsIn(s.map(TocBuilder::hymnNoOf)).inOrder()
        }
        assertThat(hant.getValue(hant.keys.first()).first()).startsWith("哎喲救主真曾流血")
        assertThat(hant.values.flatten().any { it.contains("榮") }).isTrue()
        assertThat(hant.values.flatten().any { it.contains("荣") }).isFalse()
    }

    @Test
    fun strokeHeadingsAreTraditionalAndKeepTheSimplifiedGrouping() {
        val simplified = build(HYMN_DB, HymnToc.TOC_STROKE, null)
        val hant = build(HYMN_DB, HymnToc.TOC_STROKE, HantVariant.TW)
        assertThat(simplified.keys.first()).startsWith("一画")
        assertThat(hant.keys.first()).startsWith("一畫")
        assertThat(hant.keys.size).isEqualTo(simplified.keys.size)
        for ((s, h) in simplified.values.zip(hant.values)) {
            assertThat(h.map(TocBuilder::hymnNoOf)).containsExactlyElementsIn(s.map(TocBuilder::hymnNoOf)).inOrder()
        }
    }

    @Test
    fun ybListIsTraditional() {
        val hant = build(HYMN_YB, HymnToc.TOC_CATEGORY, HantVariant.TW)
        assertThat(hant.keys).containsExactly("青年詩歌")
        assertThat(hant.getValue("青年詩歌").first()).isEqualTo("0001: 神就是愛 (bb876)")
    }

    @Test
    fun englishCrossReferenceItemsAreTraditional() {
        val hant = build(HYMN_BB, HymnToc.TOC_ENGLISH, HantVariant.TW)
        assertThat(hant).isNotEmpty()
        assertThat(hant.values.flatten().none { it.contains("诗") }).isTrue()
    }

    @Test
    fun withoutAVariantNothingChanges() {
        assertThat(build(HYMN_DB, HymnToc.TOC_CATEGORY, null))
            .isEqualTo(TocBuilder.build(::reader, HYMN_DB, HymnToc.TOC_CATEGORY, TEST_BB_LIMIT, TEST_ER_LIMIT))
    }

    @Test
    fun missingTraditionalAssetsFallBackToSimplified() {
        val onlySimplified = { path: String -> if ("_hant_" in path) null else reader(path) }
        val toc = TocBuilder.build(onlySimplified, HYMN_DB, HymnToc.TOC_CATEGORY, TEST_BB_LIMIT, TEST_ER_LIMIT, HantVariant.TW)
        assertThat(toc.keys).contains("颂三一神")
    }

    @Test
    fun categoryNameFileMatchesTocData() {
        val rows = File(assets, "lyrics_toc/toc_categories.txt").readLines().filter { it.isNotBlank() }.map { it.split('\t') }
        val expected = mapOf(
            "db" to TocData.hymnCategoryDb, "bb" to TocData.hymnCategoryBb, "xg" to TocData.hymnCategoryXg,
            "yb" to TocData.hymnCategoryYb, "xb" to TocData.hymnCategoryXb, "er" to TocData.hymnCategoryEr,
        )
        assertThat(rows.associate { it[0] to it.drop(1) }).isEqualTo(expected.mapValues { it.value.toList() })
    }

    private companion object {
        // HymnNoValidate needs android.util.Range, which unit tests do not have: the same values as TocBuilderTest
        val TEST_BB_LIMIT = intArrayOf(38, 151, 259, 350, 471, 544, 630, 763, 881, 931, 1006)
        val TEST_ER_LIMIT = intArrayOf(18, 125, 213, 324, 446, 525, 622, 720, 837, 921, 1040, 1119, 1233)
    }
}
