package org.cog.hymnchtv.nav

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class PagerItemIdsTest {
    private val counts = mapOf(
        HymnTypes.DB to 786, HymnTypes.BB to 514, HymnTypes.ER to 330, HymnTypes.XB to 168, HymnTypes.XG to 206, HymnTypes.YB to 277,
    )

    @Test fun everyPageOfEveryBookHasItsOwnId() {
        val ids = counts.flatMap { (book, n) -> (0 until n).map { PagerItemIds.itemId(book, it) } }
        assertThat(ids.toSet()).hasSize(counts.values.sum())
    }

    @Test fun idsAreStable() {
        assertThat(PagerItemIds.itemId(HymnTypes.BB, 36)).isEqualTo(PagerItemIds.itemId(HymnTypes.BB, 36))
        assertThat(PagerItemIds.itemId(HymnTypes.DB, 4)).isNotEqualTo(PagerItemIds.itemId(HymnTypes.BB, 4))
    }

    @Test fun containsOnlyItsOwnBooksPagesInRange() {
        val id = PagerItemIds.itemId(HymnTypes.BB, 36)
        assertThat(PagerItemIds.contains(HymnTypes.BB, id, 514)).isTrue()
        assertThat(PagerItemIds.contains(HymnTypes.DB, id, 786)).isFalse()
        assertThat(PagerItemIds.contains(HymnTypes.BB, PagerItemIds.itemId(HymnTypes.BB, 513), 513)).isFalse()
        assertThat(PagerItemIds.contains(HymnTypes.BB, -1L, 514)).isFalse()
    }
}
