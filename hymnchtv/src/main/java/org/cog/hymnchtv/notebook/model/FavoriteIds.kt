package org.cog.hymnchtv.notebook.model

import java.util.UUID

/**
 * Favorites use a name-based UUID so the same hymn has the same id on every device:
 * backup merges and future sync match by id and never collide on the unique hymn index.
 */
object FavoriteIds {
    @JvmStatic
    fun forKey(key: HymnKey): String =
        UUID.nameUUIDFromBytes("favorite:${key.hymnType}:${key.hymnNo}:${key.isFu}".toByteArray(Charsets.UTF_8))
            .toString()
}
