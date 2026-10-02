package org.cog.hymnchtv.notebook.backup

import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.SyncRecord
import org.cog.hymnchtv.notebook.model.version

/**
 * Deterministic last-writer-wins (same rule as future sync until S moves to server versions).
 * Order: version = max(updatedAt, deletedAt), then deleted over active, then larger updatedBy, then larger
 * canonicalContent(). It is a total order, so two devices importing each other's files converge. Only rows to upsert
 * are returned; playlist items are re-slotted so (playlistId, position) stays unique.
 */
object BackupMerger {
    fun merge(local: NotebookTables, incoming: NotebookTables): MergeResult {
        val favorites = mergeTable(local.favorites, incoming.favorites)
        val singLogs = mergeTable(local.singLogs, incoming.singLogs)
        val notes = mergeTable(local.notes, incoming.notes)
        val playlists = mergeTable(local.playlists, incoming.playlists)
        val items = mergeTable(local.playlistItems, incoming.playlistItems)
        return MergeResult(
            changes = NotebookTables(
                favorites.changes, singLogs.changes, notes.changes, playlists.changes,
                resolvePositionCollisions(local.playlistItems, items.changes),
            ),
            stats = favorites.stats + singLogs.stats + notes.stats + playlists.stats + items.stats,
        )
    }

    @JvmStatic
    fun <T : SyncRecord> compare(a: T, b: T): Int =
        compareValuesBy(a, b, { it.version }, { it.deletedAt != null }, { it.updatedBy }, { canonicalContent(it) })

    /**
     * Explicit, injective serialization of a row's content columns (id and sync columns excluded), used as the
     * last tie-breaker. Each field is "<length>:<value>", null is "~", joined by "|". Pinned by BackupMergerTest:
     * changing it changes merge results, so treat it like a file format.
     */
    internal fun canonicalContent(row: SyncRecord): String = when (row) {
        is FavoriteEntity -> hymnFields(row.hymn) + listOf(row.createdAt)
        is SingLogEntity ->
            hymnFields(row.hymn) + listOf(row.sungAt, row.occasion.name, row.source.name, row.playlistId, row.createdAt)
        is NoteEntity -> hymnFields(row.hymn) + listOf(row.body, row.singLogId, row.createdAt)
        is PlaylistEntity -> listOf(row.name, row.createdAt)
        is PlaylistItemEntity -> listOf<Any?>(row.playlistId, row.position) + hymnFields(row.hymn) + listOf(row.createdAt)
        else -> throw IllegalArgumentException("Unsupported row type ${row::class.java.name}")
    }.joinToString("|") { field -> field?.toString()?.let { "${it.length}:$it" } ?: "~" }

    private fun hymnFields(key: HymnKey): List<Any?> = listOf(key.hymnType, key.hymnNo, key.isFu)

    /** True when [candidate] should replace [current]. */
    @JvmStatic
    fun <T : SyncRecord> isNewer(candidate: T, current: T): Boolean = compare(candidate, current) > 0

    internal fun <T : SyncRecord> mergeTable(local: List<T>, incoming: List<T>): TableMerge<T> {
        val localById = local.associateBy { it.id }
        val newestIncoming = incoming.groupBy { it.id }.values.map { rows ->
            rows.reduce { kept, next -> if (isNewer(next, kept)) next else kept }
        }
        val decisions = newestIncoming.map { row -> decide(localById[row.id], row) to row }
        return TableMerge(
            changes = decisions.filter { (decision, _) -> decision != Decision.KEEP }.map { (_, row) -> row },
            stats = MergeStats(
                inserted = decisions.count { it.first == Decision.INSERT },
                updated = decisions.count { it.first == Decision.UPDATE },
                unchanged = decisions.count { it.first == Decision.KEEP },
            ),
        )
    }

    /**
     * Makes (playlistId, position) unique after a merge, using only the merged set of winning rows, so the layout does
     * not depend on which side is "local" and two devices importing each other's files end up identical. For each
     * contested slot the row that wins [compare] (then the smaller id) keeps it; the others move after the playlist's
     * highest slot, in (position, id) order. Returns the incoming rows plus any local row that had to move.
     * updatedAt is not changed: re-slotting is a layout adjustment, not an edit.
     */
    internal fun resolvePositionCollisions(
        local: List<PlaylistItemEntity>,
        changes: List<PlaylistItemEntity>,
    ): List<PlaylistItemEntity> {
        if (changes.isEmpty()) return changes
        val changedIds = changes.mapTo(HashSet()) { it.id }
        val incomingByPlaylist = changes.groupBy { it.playlistId }
        val keptByPlaylist =
            local.filter { it.id !in changedIds && it.playlistId in incomingByPlaylist }.groupBy { it.playlistId }
        val finalPosition = HashMap<String, Int>()
        val contested = Comparator<PlaylistItemEntity> { x, y -> compare(y, x) }.thenBy { it.id }
        for ((playlistId, incoming) in incomingByPlaylist) {
            val winners = keptByPlaylist[playlistId].orEmpty() + incoming
            var nextSlot = winners.maxOf { it.position } + 1
            for ((_, rows) in winners.groupBy { it.position }.toSortedMap()) {
                val ranked = rows.sortedWith(contested)
                finalPosition[ranked.first().id] = ranked.first().position
                ranked.drop(1).sortedBy { it.id }.forEach { finalPosition[it.id] = nextSlot++ }
            }
        }
        val movedKept = keptByPlaylist.values.flatten().filter { finalPosition.getValue(it.id) != it.position }
        return (changes + movedKept).map { it.copy(position = finalPosition.getValue(it.id)) }
    }

    private fun <T : SyncRecord> decide(current: T?, incoming: T): Decision = when {
        current == null -> Decision.INSERT
        current == incoming -> Decision.KEEP
        isNewer(incoming, current) -> Decision.UPDATE
        else -> Decision.KEEP
    }

    private enum class Decision { INSERT, UPDATE, KEEP }
}

internal data class TableMerge<T>(val changes: List<T>, val stats: MergeStats)
