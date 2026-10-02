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
   * Local rows that are not being replaced keep their slot (soft-deleted ones too). Incoming rows whose slot is
   * taken move after the playlist's highest slot, in (position, id) order. updatedAt is not changed: re-slotting
   * is a local adjustment, not an edit.
   */
  internal fun resolvePositionCollisions(
      local: List<PlaylistItemEntity>,
      changes: List<PlaylistItemEntity>,
  ): List<PlaylistItemEntity> {
      val changedIds = changes.map { it.id }.toSet()
      val kept = local.filter { it.id !in changedIds }
      val reslotted = changes.groupBy { it.playlistId }.flatMap { (playlistId, rows) ->
          val keptSlots = kept.filter { it.playlistId == playlistId }.map { it.position }
          val highest = (keptSlots + rows.map { it.position }).maxOrNull() ?: -1
          val start = Slots(keptSlots.toSet(), highest, emptyList())
          rows.sortedWith(compareBy({ it.position }, { it.id })).fold(start) { slots, row ->
              if (row.position !in slots.occupied) slots.take(row) else slots.take(row.copy(position = slots.highest + 1))
          }.rows
      }.associateBy { it.id }
      return changes.map { reslotted.getValue(it.id) }
  }

  private data class Slots(val occupied: Set<Int>, val highest: Int, val rows: List<PlaylistItemEntity>) {
      fun take(row: PlaylistItemEntity) =
          Slots(occupied + row.position, maxOf(highest, row.position), rows + row)
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
