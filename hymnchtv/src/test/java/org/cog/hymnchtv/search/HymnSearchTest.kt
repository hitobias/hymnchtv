package org.cog.hymnchtv.search

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class HymnSearchTest {
    private fun text(title: String, body: String) = "1\r\n$title\r\n（英1）\r\n降A大调4/4\r\n\r\n$body\r\n"

    /** The first n main-book hymns contain 「祂」; nothing else has a file. */
    private fun dbHits(n: Int) = LyricsSource { ref ->
        if (ref.book == HymnTypes.DB && ref.storedNo <= n) text("颂赞－标题${ref.storedNo}", "一\r\n祂是主") else null
    }

    @Test fun limitBoundaries() {
        for ((hits, expectMore) in listOf(199 to false, 200 to false, 201 to true)) {
            val page = HymnSearch(dbHits(hits)).search("祂", SearchScope.All, limit = 200)
            assertWithMessage("hits=$hits").that(page.results).hasSize(minOf(hits, 200))
            assertWithMessage("hits=$hits").that(page.hasMore).isEqualTo(expectMore)
        }
    }

    @Test fun sameNumberInTwoBooksKeepsBothWithTheirOwnBook() {
        val src = LyricsSource { ref -> if (ref.storedNo == 5 && (ref.book == HymnTypes.DB || ref.book == HymnTypes.BB)) text("甲－乙", "祂是主") else null }
        val refs = HymnSearch(src).search("祂", SearchScope.All).results.map { it.ref }
        assertThat(refs).containsExactly(HymnRef(HymnTypes.DB, 5), HymnRef(HymnTypes.BB, 5)).inOrder()
    }

    @Test fun scopeLimitsToOneBookAndBooksComeInTheOldOrder() {
        val everywhere = LyricsSource { ref -> if (ref.storedNo == 101 || ref.storedNo == 1) text("甲－乙", "祂是主") else null }
        val all = HymnSearch(everywhere).search("祂", SearchScope.All).results.map { it.ref.book }.distinct()
        assertThat(all).containsExactly(
            HymnTypes.DB, HymnTypes.BB, HymnTypes.XB, HymnTypes.XG, HymnTypes.YB, HymnTypes.ER,
        ).inOrder()
        val bb = HymnSearch(everywhere).search("祂", SearchScope.Book(HymnTypes.BB)).results
        assertThat(bb).isNotEmpty()
        assertThat(bb.map { it.ref.book }.distinct()).containsExactly(HymnTypes.BB)
    }

    @Test fun unknownBookScopeFindsNothing() {
        assertThat(HymnSearch(dbHits(5)).search("祂", SearchScope.Book("hymn_zz")).results).isEmpty()
    }

    @Test fun traditionalQueryMatchesSimplifiedLyrics() {
        val t2s = T2sMap.parse(listOf("祂\t他", "詩\t诗"))
        val src = LyricsSource { ref -> if (ref == HymnRef(HymnTypes.DB, 1)) text("颂赞－诗歌", "一\r\n他是主") else null }
        assertThat(HymnSearch(src, t2s).search("詩", SearchScope.All).results).hasSize(1)
    }

    @Test fun fuHitsCarryTheFuRef() {
        val src = LyricsSource { ref ->
            if ((ref.book == HymnTypes.YB && ref.storedNo > 275) || ref == HymnRef(HymnTypes.DB, 781)) text("甲－乙", "祂") else null
        }
        val refs = HymnSearch(src).search("祂", SearchScope.All).results.map { it.ref }
        assertThat(refs.map { it.isFu }).containsExactly(true, true, true)
        assertThat(refs).containsExactly(HymnRef(HymnTypes.DB, 781), HymnRef(HymnTypes.YB, 276), HymnRef(HymnTypes.YB, 277)).inOrder()
    }

    @Test fun titleHitAndLyricsHitBothFound() {
        val src = LyricsSource { ref ->
            when (ref) {
                HymnRef(HymnTypes.DB, 1) -> text("颂赞－独特标题", "一\r\n普通歌词")
                HymnRef(HymnTypes.DB, 2) -> text("颂赞－别的", "一\r\n独特歌词")
                else -> null
            }
        }
        val search = HymnSearch(src)
        val byTitle = search.search("独特标", SearchScope.All).results.single()
        assertThat(byTitle.ref.storedNo).isEqualTo(1)
        assertThat(byTitle.snippet).startsWith("颂赞－独特标题")
        assertThat(byTitle.title).isEqualTo("独特标题（英1）")
        val byLyrics = search.search("独特歌", SearchScope.All).results.single()
        assertThat(byLyrics.ref.storedNo).isEqualTo(2)
        assertThat(byLyrics.snippet).startsWith("独特歌词")
    }

    @Test fun blankQueryReturnsEmptyAndCancelStopsEarly() {
        assertThat(HymnSearch(dbHits(5)).search("   ", SearchScope.All)).isEqualTo(SearchPage(emptyList(), false))
        var reads = 0
        val src = LyricsSource { reads++; null }
        val page = HymnSearch(src).search("祂", SearchScope.All, isCancelled = { reads >= 3 })
        assertThat(page.results).isEmpty()
        assertThat(reads).isEqualTo(3)
    }

    @Test fun traditionalDisplayUsesTheHantTextWhenLineCountsMatch() {
        val simplified = text("颂赞－计划", "一\r\n祂的计划")
        val hant = text("頌讚－計劃", "一\r\n祂的計劃")
        val src = object : LyricsSource {
            override fun simplified(ref: HymnRef) = if (ref == HymnRef(HymnTypes.DB, 1)) simplified else null
            override fun traditional(ref: HymnRef) = hant
        }
        val r = HymnSearch(src).search("祂的计划", SearchScope.All).results.single()
        assertThat(r.title).isEqualTo("計劃（英1）")
        assertThat(r.snippet).startsWith("祂的計劃")

        val mismatched = object : LyricsSource {
            override fun simplified(ref: HymnRef) = if (ref == HymnRef(HymnTypes.DB, 1)) simplified else null
            override fun traditional(ref: HymnRef) = "1\r\n頌讚－計劃\r\n"
        }
        val fallback = HymnSearch(mismatched).search("祂的计划", SearchScope.All).results.single()
        assertThat(fallback.title).isEqualTo("计划（英1）")
        assertThat(fallback.snippet).startsWith("祂的计划")
    }
}
