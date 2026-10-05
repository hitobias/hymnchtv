package org.cog.hymnchtv.ui.playlist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ItemMovesTest {
    private val list = listOf("a", "b", "c", "d")

    @Test fun movesDownAndUp() {
        assertThat(ItemMoves.move(list, 0, 2)).containsExactly("b", "c", "a", "d").inOrder()
        assertThat(ItemMoves.move(list, 3, 0)).containsExactly("d", "a", "b", "c").inOrder()
        assertThat(ItemMoves.move(list, 1, 2)).containsExactly("a", "c", "b", "d").inOrder()
    }

    @Test fun sameOrInvalidIndexesReturnTheListUnchanged() {
        assertThat(ItemMoves.move(list, 1, 1)).isSameInstanceAs(list)
        assertThat(ItemMoves.move(list, -1, 2)).isSameInstanceAs(list)
        assertThat(ItemMoves.move(list, 0, 4)).isSameInstanceAs(list)
    }

    @Test fun theInputIsNotChanged() {
        ItemMoves.move(list, 0, 3)
        assertThat(list).containsExactly("a", "b", "c", "d").inOrder()
    }
}
