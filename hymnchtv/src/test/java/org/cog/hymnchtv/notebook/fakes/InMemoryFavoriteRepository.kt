package org.cog.hymnchtv.notebook.fakes

import kotlinx.coroutines.CompletableDeferred
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.isActive
import org.cog.hymnchtv.notebook.repo.FavoriteRepository

class InMemoryFavoriteRepository(private val clock: Clock, private val device: String = TEST_DEVICE) : FavoriteRepository {
    @Volatile
    var rows: Map<String, FavoriteEntity> = emptyMap()
        private set

    @Volatile
    var failNext: Boolean = false

    /** When set, isFavorite/toggle suspend until it completes (to test cancellation). */
    @Volatile
    var gate: CompletableDeferred<Unit>? = null

    private suspend fun enter() {
        gate?.await()
        if (failNext) {
            failNext = false
            throw IllegalStateException("simulated failure")
        }
    }

    private fun put(row: FavoriteEntity): FavoriteEntity {
        rows = rows + (row.id to row)
        return row
    }

    override suspend fun findAll() = rows.values.filter { it.isActive }

    override suspend fun findById(id: String) = rows[id]?.takeIf { it.isActive }

    override suspend fun create(item: FavoriteEntity): FavoriteEntity = checkNotNull(setFavorite(item.hymn, true))

    override suspend fun update(item: FavoriteEntity): FavoriteEntity? =
        findById(item.id)?.copy(updatedAt = clock.nowMillis(), updatedBy = device)?.let(::put)

    override suspend fun delete(id: String): Boolean {
        val existing = findById(id) ?: return false
        val now = clock.nowMillis()
        put(existing.copy(updatedAt = now, deletedAt = now, updatedBy = device))
        return true
    }

    override suspend fun isFavorite(key: HymnKey): Boolean {
        enter()
        return rows[FavoriteIds.forKey(key)]?.isActive == true
    }

    override suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity? {
        val id = FavoriteIds.forKey(key)
        val existing = rows[id]
        val now = clock.nowMillis()
        val row = if (favorite) {
            if (existing != null && existing.isActive) return existing
            existing?.copy(updatedAt = now, deletedAt = null, updatedBy = device)
                ?: FavoriteEntity(id, key, now, now, null, device)
        } else {
            if (existing == null || !existing.isActive) return existing
            existing.copy(updatedAt = now, deletedAt = now, updatedBy = device)
        }
        return put(row)
    }

    override suspend fun toggle(key: HymnKey): Boolean {
        enter()
        val next = rows[FavoriteIds.forKey(key)]?.isActive != true
        setFavorite(key, next)
        return next
    }
}
