package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.HymnKey

interface FavoriteRepository : Repository<FavoriteEntity> {
    suspend fun isFavorite(key: HymnKey): Boolean

    /** Idempotent. Returns the stored row, or null when un-favoriting a hymn that was never favorited. */
    suspend fun setFavorite(key: HymnKey, favorite: Boolean): FavoriteEntity?

    /** Flips the state atomically and returns the new state. */
    suspend fun toggle(key: HymnKey): Boolean
}
