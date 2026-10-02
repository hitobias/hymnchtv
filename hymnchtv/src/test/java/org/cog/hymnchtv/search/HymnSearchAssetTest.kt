package org.cog.hymnchtv.search

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.hymn.HymnNumberRules
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.lyrics.HantVariant
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test
import java.io.File

class HymnSearchAssetTest {
    private val assets = File(checkNotNull(System.getProperty("hymnchtv.assetsDir")) { "hymnchtv.assetsDir not set" })
    private val t2s = T2sMap.parse(File(assets, T2sMap.ASSET_PATH).readLines(Charsets.UTF_8))

    private fun source(variant: HantVariant?) =
        AssetLyricsSource({ path -> File(assets, path).takeIf { it.isFile }?.readText(Charsets.UTF_8) }, variant)

    private val search = HymnSearch(source(null), t2s)

    @Test fun simplifiedQueryHitsMainBookOne() {
        val r = search.search("祂的计划", SearchScope.All).results
        assertThat(r.first().ref).isEqualTo(HymnRef(HymnTypes.DB, 1))
        assertThat(r.first().snippet).contains("祂的计划")
    }

    @Test fun traditionalQueryHitsTheSameHymn() {
        val r = HymnSearch(source(HantVariant.TW), t2s).search("祂的計劃", SearchScope.All).results
        assertThat(r.first().ref).isEqualTo(HymnRef(HymnTypes.DB, 1))
        assertThat(r.first().title).contains("計劃")
    }

    @Test fun commonCharacterIsCappedAtTwoHundred() {
        val page = search.search("的", SearchScope.All, limit = 200)
        assertThat(page.results).hasSize(200)
        assertThat(page.hasMore).isTrue()
    }

    @Test fun bookScopeOnlyReturnsThatBook() {
        val r = search.search("主", SearchScope.Book(HymnTypes.BB)).results
        assertThat(r).isNotEmpty()
        assertThat(r.map { it.ref.book }.distinct()).containsExactly(HymnTypes.BB)
    }

    @Test fun hantFilesHaveTheSameLineCountAsSimplifiedForEveryHymn() {
        val simplified = source(null)
        val hant = source(HantVariant.TW)
        var compared = 0
        for (book in HymnSearch.BOOK_ORDER) for (no in HymnNumberRules.storedNumbers(book)) {
            val ref = HymnRef(book, no)
            val s = simplified.simplified(ref) ?: continue
            val t = hant.traditional(ref)
            assertWithMessage("hant file for $ref").that(t).isNotNull()
            assertWithMessage("line count of $ref").that(SnippetExtractor.lines(t!!).size).isEqualTo(SnippetExtractor.lines(s).size)
            compared++
        }
        assertThat(compared).isGreaterThan(1500)
    }

    @Test fun fuHymnsAreSearchableAndKeepTheirFuRef() {
        val fuHits = HymnNumberRules.storedNumbers(HymnTypes.DB).filter { it > 780 }
        assertThat(fuHits).isNotEmpty()
        val one = source(null).simplified(HymnRef(HymnTypes.DB, 781))!!
        val needle = SnippetExtractor.lines(one)[1].substringAfterLast("－").take(2)
        val r = search.search(needle, SearchScope.Book(HymnTypes.DB), limit = 1000).results
        val fu = r.firstOrNull { it.ref.storedNo == 781 }
        assertThat(fu).isNotNull()
        assertThat(fu!!.ref.isFu).isTrue()
        assertThat(fu.ref.displayNo).isEqualTo(1)
    }
}
