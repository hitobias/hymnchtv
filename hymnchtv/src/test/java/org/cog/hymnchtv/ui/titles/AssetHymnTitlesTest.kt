package org.cog.hymnchtv.ui.titles

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity.HYMN_BB
import org.cog.hymnchtv.MainActivity.HYMN_DB
import org.cog.hymnchtv.MainActivity.HYMN_YB
import org.cog.hymnchtv.toc.YbCrossRef
import org.junit.Test
import java.io.File

class AssetHymnTitlesTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })

    private val titles = AssetHymnTitles(
        reader = { path -> File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8) },
        ybTable = { YbCrossRef.parse(File(assets, YbCrossRef.ASSET).readText(Charsets.UTF_8)) },
    )

    @Test
    fun dbTitleStripsCategoryAndAppendsNote() {
        // db1: line 2 "颂赞三一神－祂的计划", line 3 "8583（英1）"
        assertThat(titles.lookup(HYMN_DB, 1)).isEqualTo("祂的计划（英1）")
    }

    @Test
    fun missingHymnIsNull() {
        assertThat(titles.lookup(HYMN_DB, 9999)).isNull()
        assertThat(titles.lookup(HYMN_DB, 0)).isNull()
    }

    @Test
    fun unknownTypeIsNull() {
        assertThat(titles.lookup("hymn_zz", 1)).isNull()
    }

    @Test
    fun ybCrossReferenceUsesTheTargetBooksFile() {
        // YB 1 lives in bb876
        assertThat(titles.lookup(HYMN_YB, 1)).isNotNull()
        assertThat(titles.lookup(HYMN_YB, 1)).isEqualTo(titles.lookup(HYMN_BB, 876))
    }

    @Test
    fun ybWithoutCrossReferenceReadsItsOwnFile() {
        assertThat(titles.lookup(HYMN_YB, 102)).isNotNull()
    }

    @Test
    fun fuHymnFilesAreReachableByTheirContinuedNumber() {
        assertThat(titles.lookup(HYMN_DB, 781)).isNotNull()
    }

    @Test
    fun titleOfStripsCategoryAndKeepsTheNote() {
        assertThat(AssetHymnTitles.titleOf("1\r\n颂赞三一神－祂的计划\r\n8583（英1）\r\n")).isEqualTo("祂的计划（英1）")
        assertThat(AssetHymnTitles.titleOf("1\n标题\n无注\n")).isEqualTo("标题")
        assertThat(AssetHymnTitles.titleOf("1\n甲－乙－丙")).isEqualTo("丙")
    }

    @Test
    fun titleOfNeedsTwoLines() {
        assertThat(AssetHymnTitles.titleOf("1")).isNull()
        assertThat(AssetHymnTitles.titleOf("")).isNull()
    }

    private val hantTitles = AssetHymnTitles(
        reader = { path -> File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8) },
        ybTable = { YbCrossRef.parse(File(assets, YbCrossRef.ASSET).readText(Charsets.UTF_8)) },
        variant = org.cog.hymnchtv.lyrics.HantVariant.TW,
    )

    @Test
    fun traditionalTitleComesFromTheGeneratedLyrics() {
        assertThat(hantTitles.lookup(HYMN_DB, 1)).isEqualTo("祂的計劃（英1）")
    }

    @Test
    fun traditionalCrossReferenceFollowsTheTargetBook() {
        assertThat(hantTitles.lookup(HYMN_YB, 1)).isEqualTo(hantTitles.lookup(HYMN_BB, 876))
        assertThat(hantTitles.lookup(HYMN_YB, 1)).isEqualTo("神就是愛")
    }

    @Test
    fun traditionalFallsBackToSimplifiedWhenTheAssetIsMissing() {
        val onlySimplified = AssetHymnTitles(
            reader = { path -> if ("_hant_" in path) null else File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8) },
            variant = org.cog.hymnchtv.lyrics.HantVariant.TW,
        )
        assertThat(onlySimplified.lookup(HYMN_DB, 1)).isEqualTo("祂的计划（英1）")
    }
}
