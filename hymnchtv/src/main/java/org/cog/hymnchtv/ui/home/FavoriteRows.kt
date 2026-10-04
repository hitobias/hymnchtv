package org.cog.hymnchtv.ui.home

import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.HymnKey

/** One line of the favourites tab. [dateMillis] is when the hymn was (last) favourited. */
data class FavoriteRow(val key: HymnKey, val ref: HymnRef, val title: String?, val dateMillis: Long)

/** Pure list model of the favourites tab (no Android dependency, so it is unit tested on the JVM). */
object FavoriteRows {
    /** Newest favourite first; [titleOf] is called once per row with the stored (book, number) reference. */
    fun build(entities: List<FavoriteEntity>, titleOf: (HymnRef) -> String?): List<FavoriteRow> = entities
        .sortedWith(compareByDescending<FavoriteEntity> { it.updatedAt }.thenBy { it.id })
        .mapNotNull { entity ->
            val ref = HymnRef(entity.hymn.hymnType, entity.hymn.hymnNo)
            if (ref.isValid) FavoriteRow(entity.hymn, ref, titleOf(ref), entity.updatedAt) else null
        }

    fun isEmpty(rows: List<FavoriteRow>): Boolean = rows.isEmpty()
}
