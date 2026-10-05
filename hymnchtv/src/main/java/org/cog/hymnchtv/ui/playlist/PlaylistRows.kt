package org.cog.hymnchtv.ui.playlist

import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity

/** One line of the playlists tab. [updatedAt] is when the playlist was created or renamed. */
data class PlaylistRow(val id: String, val name: String, val count: Int, val updatedAt: Long)

/** Pure list model of the playlists tab. */
object PlaylistRows {
    fun build(playlists: List<PlaylistEntity>, counts: Map<String, Int>): List<PlaylistRow> = playlists
        .filter { it.deletedAt == null }
        .sortedWith(compareByDescending<PlaylistEntity> { it.updatedAt }.thenBy { it.id })
        .map { PlaylistRow(it.id, it.name, counts[it.id] ?: 0, it.updatedAt) }
}
