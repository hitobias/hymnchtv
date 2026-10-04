package org.cog.hymnchtv.ui.home

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.fakes.InMemoryFavoriteRepository
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Test

class FavoriteRowsTest {
    private fun entity(key: HymnKey, updatedAt: Long, createdAt: Long = updatedAt) =
        FavoriteEntity("id-${key.hymnType}-${key.hymnNo}", key, createdAt, updatedAt, null, "dev")

    private val noTitle: (HymnRef) -> String? = { null }

    @Test fun sortedByUpdatedAtDescending() {
        val rows = FavoriteRows.build(
            listOf(
                entity(HymnKey.of(HymnTypes.DB, 1), 100),
                entity(HymnKey.of(HymnTypes.DB, 2), 300),
                entity(HymnKey.of(HymnTypes.DB, 3), 200),
            ),
            noTitle,
        )
        assertThat(rows.map { it.ref.storedNo }).containsExactly(2, 3, 1).inOrder()
    }

    @Test fun unfavouriteThenRefavouriteMovesToTopAndKeepsCreatedAt() = runBlocking {
        var now = 1000L
        val repo = InMemoryFavoriteRepository(Clock { now })
        val a = HymnKey.of(HymnTypes.DB, 10)
        val b = HymnKey.of(HymnTypes.DB, 20)
        repo.setFavorite(a, true)
        now += 10
        repo.setFavorite(b, true)
        val createdA = repo.findAll().first { it.hymn == a }.createdAt
        now += 10
        repo.setFavorite(a, false)
        now += 10
        repo.setFavorite(a, true)

        val all = repo.findAll()
        val rows = FavoriteRows.build(all, noTitle)
        assertThat(rows.map { it.key }).containsExactly(a, b).inOrder()
        val a2 = all.first { it.hymn == a }
        assertThat(a2.createdAt).isEqualTo(createdA)
        assertThat(a2.updatedAt).isGreaterThan(createdA)
    }

    @Test fun youthSupplementRowShowsAppendixRef() {
        val row = FavoriteRows.build(listOf(entity(HymnKey.of(HymnTypes.YB, 276), 1)), noTitle).single()
        assertThat(row.ref).isEqualTo(HymnRef(HymnTypes.YB, 276))
        assertThat(row.ref.isFu).isTrue()
        assertThat(row.ref.displayNo).isEqualTo(1)
    }

    @Test fun dateIsUpdatedAtNotCreatedAt() {
        val row = FavoriteRows.build(listOf(entity(HymnKey.of(HymnTypes.BB, 5), updatedAt = 900, createdAt = 100)), noTitle).single()
        assertThat(row.dateMillis).isEqualTo(900)
    }

    @Test fun titleLookupUsesTheGivenSource() {
        val seen = mutableListOf<HymnRef>()
        val rows = FavoriteRows.build(
            listOf(entity(HymnKey.of(HymnTypes.BB, 5), 2), entity(HymnKey.of(HymnTypes.XB, 7), 1)),
        ) { ref ->
            seen += ref
            if (ref.book == HymnTypes.BB) "標題" else null
        }
        assertThat(seen).containsExactly(HymnRef(HymnTypes.BB, 5), HymnRef(HymnTypes.XB, 7))
        assertThat(rows[0].title).isEqualTo("標題")
        assertThat(rows[1].title).isNull()
    }

    @Test fun emptyListIsEmptyState() {
        assertThat(FavoriteRows.isEmpty(FavoriteRows.build(emptyList(), noTitle))).isTrue()
        assertThat(FavoriteRows.isEmpty(FavoriteRows.build(listOf(entity(HymnKey.of(HymnTypes.DB, 1), 1)), noTitle))).isFalse()
    }

    @Test fun validKeysAreAllKeptAndTitleFailuresDoNotCrash() {
        // HymnKey cannot hold an invalid number (its init rejects it); the isValid filter is only a backup for odd imports
        val keys = listOf(HymnKey.of(HymnTypes.DB, 786), HymnKey.of(HymnTypes.YB, 277))
        val rows = FavoriteRows.build(keys.mapIndexed { i, k -> entity(k, i.toLong()) }, noTitle)
        assertThat(rows).hasSize(2)
    }
}
