package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_A
import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_B
import org.cog.hymnchtv.notebook.backup.SampleTables.upserted
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.junit.Test

class BackupMergerTest {
    private val a = testUuid(1)
    private val b = testUuid(2)
    private val c = testUuid(3)
    private val pl = testUuid(9)

    private fun log(id: String, updatedAt: Long, deletedAt: Long? = null, sungAt: Long = 1, by: String = DEVICE_A) =
        SampleTables.singLog(id, sungAt = sungAt, updatedAt = updatedAt, deletedAt = deletedAt, updatedBy = by)

    /** The row that ends up stored when [incoming] is merged into a notebook holding [local]. */
    private fun winner(local: SingLogEntity, incoming: SingLogEntity): SingLogEntity =
        BackupMerger.mergeTable(listOf(local), listOf(incoming)).changes.singleOrNull() ?: local

    @Test
    fun insertsEverythingIntoAnEmptyNotebook() {
        val full = SampleTables.full()
        val result = BackupMerger.merge(NotebookTables.EMPTY, full)
        assertThat(result.changes).isEqualTo(full)
        assertThat(result.stats).isEqualTo(MergeStats(10, 0, 0))
    }

    @Test
    fun newerIncomingWins() {
        val incoming = log(a, updatedAt = 2, sungAt = 9)
        val merge = BackupMerger.mergeTable(listOf(log(a, 1)), listOf(incoming))
        assertThat(merge.changes).containsExactly(incoming)
        assertThat(merge.stats).isEqualTo(MergeStats(0, 1, 0))
    }

    @Test
    fun olderIncomingIsIgnored() {
        val merge = BackupMerger.mergeTable(listOf(log(a, 5)), listOf(log(a, 1, sungAt = 9)))
        assertThat(merge.changes).isEmpty()
        assertThat(merge.stats).isEqualTo(MergeStats(0, 0, 1))
    }

    @Test
    fun identicalRowIsUnchanged() {
        val merge = BackupMerger.mergeTable(listOf(log(a, 5)), listOf(log(a, 5)))
        assertThat(merge.changes).isEmpty()
        assertThat(merge.stats).isEqualTo(MergeStats(0, 0, 1))
    }

    @Test
    fun onEqualTimeADeleteWinsFromEitherSide() {
        val active = log(a, 5)
        val deleted = log(a, 5, deletedAt = 5)
        assertThat(winner(local = active, incoming = deleted)).isEqualTo(deleted)
        assertThat(winner(local = deleted, incoming = active)).isEqualTo(deleted)
    }

    @Test
    fun onEqualTimeTheLargerDeviceIdWins() {
        val fromA = log(a, 5, sungAt = 1, by = DEVICE_A)
        val fromB = log(a, 5, sungAt = 2, by = DEVICE_B)
        assertThat(winner(local = fromA, incoming = fromB)).isEqualTo(fromB)
        assertThat(winner(local = fromB, incoming = fromA)).isEqualTo(fromB)
    }

    @Test
    fun onEqualTimeAndDeviceTheResultStillConverges() {
        val x = log(a, 5, sungAt = 1)
        val y = log(a, 5, sungAt = 2)
        assertThat(winner(local = x, incoming = y)).isEqualTo(winner(local = y, incoming = x))
    }

    @Test
    fun deletedAtLaterThanUpdatedAtCounts() {
        val incoming = log(a, updatedAt = 3, deletedAt = 12)
        assertThat(BackupMerger.mergeTable(listOf(log(a, 10)), listOf(incoming)).changes).containsExactly(incoming)
    }

    @Test
    fun duplicateIdsInTheFileKeepTheNewest() {
        val merge = BackupMerger.mergeTable(emptyList(), listOf(log(a, 1), log(a, 7), log(a, 3)))
        assertThat(merge.changes.single().updatedAt).isEqualTo(7L)
        assertThat(merge.stats).isEqualTo(MergeStats(1, 0, 0))
    }

    @Test
    fun mergingTheSameFileTwiceIsIdempotent() {
        val local = NotebookTables(singLogs = listOf(log(SampleTables.LOG_1, updatedAt = 50), log(c, 1)))
        val incoming = SampleTables.full()
        val first = BackupMerger.merge(local, incoming)
        val second = BackupMerger.merge(local.upserted(first.changes), incoming)
        assertThat(second.changes.rowCount).isEqualTo(0)
        assertThat(second.stats).isEqualTo(MergeStats(0, 0, incoming.rowCount))
    }

    @Test
    fun statsAddUpAcrossTables() {
        val local = NotebookTables(
            favorites = listOf(SampleTables.favorite(SampleTables.db1, updatedAt = 999)),
            notes = listOf(SampleTables.note(SampleTables.NOTE_1, updatedAt = 1)),
        )
        val stats = BackupMerger.merge(local, SampleTables.full()).stats
        assertThat(stats).isEqualTo(MergeStats(inserted = 8, updated = 1, unchanged = 1))
    }

    @Test
    fun theNewerItemKeepsAContestedSlotAndTheOlderLocalItemMoves() {
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
        val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(b, pl, 0, updatedAt = 200)))
        val changes = BackupMerger.merge(local, incoming).changes.playlistItems
        assertThat(changes.associate { it.id to it.position }).containsExactly(b, 0, a, 1)
    }

    @Test
    fun aRowMovedToAFreeSlotKeepsIt() {
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
        val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 5, updatedAt = 200)))
        assertThat(BackupMerger.merge(local, incoming).changes.playlistItems.single().position).isEqualTo(5)
    }

    @Test
    fun softDeletedLocalRowsStillOccupyTheirSlot() {
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0, deletedAt = 150)))
        val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(c, pl, 0)))
        assertThat(BackupMerger.merge(local, incoming).changes.playlistItems.single().position).isEqualTo(1)
    }

    @Test
    fun canonicalContentIsPinned() {
        val log = SampleTables.singLog(
            SampleTables.LOG_2, key = SampleTables.fu1, occasion = Occasion.SMALL_GROUP, source = SingSource.AUTO,
            playlistId = SampleTables.PL_1,
        )
        assertThat(BackupMerger.canonicalContent(log))
            .isEqualTo("7:hymn_db|3:781|4:true|4:1000|11:SMALL_GROUP|4:AUTO|36:00000000-0000-0000-0000-00000000012d|3:100")
        assertThat(BackupMerger.canonicalContent(SampleTables.singLog(SampleTables.LOG_1)))
            .isEqualTo("7:hymn_db|1:1|5:false|4:1000|9:LORDS_DAY|6:MANUAL|~|3:100")
        assertThat(BackupMerger.canonicalContent(SampleTables.playlist(SampleTables.PL_1, name = "a|b")))
            .isEqualTo("3:a|b|3:100")
    }

    @Test
    fun finalTieBreakUsesCanonicalContent() {
        val later = SampleTables.note(SampleTables.NOTE_1, body = "b-body")
        val earlier = SampleTables.note(SampleTables.NOTE_1, body = "a-body")
        assertThat(BackupMerger.compare(later, earlier)).isGreaterThan(0)
        assertThat(BackupMerger.mergeTable(listOf(earlier), listOf(later)).changes).containsExactly(later)
        assertThat(BackupMerger.mergeTable(listOf(later), listOf(earlier)).changes).isEmpty()
    }

    @Test
    fun swappedItemsKeepTheirImportedSlots() {
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0), SampleTables.item(b, pl, 1)))
        val incoming = NotebookTables(
            playlistItems = listOf(SampleTables.item(a, pl, 1, updatedAt = 200), SampleTables.item(b, pl, 0, updatedAt = 200)),
        )
        val changes = BackupMerger.merge(local, incoming).changes.playlistItems
        assertThat(changes.associate { it.id to it.position }).containsExactly(a, 1, b, 0)
    }

    @Test
    fun reslottingIsDeterministicByPositionThenId() {
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, 0)))
        val incoming = NotebookTables(playlistItems = listOf(SampleTables.item(c, pl, 0), SampleTables.item(b, pl, 0)))
        val changes = BackupMerger.merge(local, incoming).changes.playlistItems
        assertThat(changes.associate { it.id to it.position }).containsExactly(b, 1, c, 2)
    }

    @Test
    fun reslottingAtTheMaximumImportedPositionStaysBelowTenDigitsAndConverges() {
        val max = BackupCodec.MAX_POSITION.toInt()
        val ids = (1..50).map { testUuid(100 + it) }
        val local = NotebookTables(playlistItems = listOf(SampleTables.item(a, pl, max, updatedAt = 500)))
        val incoming = NotebookTables(playlistItems = ids.map { SampleTables.item(it, pl, max, updatedAt = 100) })
        val merged = local.upserted(BackupMerger.merge(local, incoming).changes)
        val all = merged.playlistItems.map { it.position }
        assertThat(all.toSet()).hasSize(51)
        assertThat(all.max()).isLessThan(1_000_000_000)
        assertThat(BackupMerger.merge(merged, incoming).changes.playlistItems).isEmpty()
    }

    private fun positions(tables: NotebookTables) = tables.playlistItems.associate { it.id to it.position }

    @Test
    fun exchangingFilesBothWaysConvergesToTheSameLayout() {
        // Device A reordered (newer edits); device B appended an item into a slot that A reused.
        val x = testUuid(11)
        val y = testUuid(12)
        val z = testUuid(13)
        val base = listOf(SampleTables.item(x, pl, 0), SampleTables.item(y, pl, 1))
        val deviceA = NotebookTables(
            playlistItems = listOf(SampleTables.item(y, pl, 2, updatedAt = 300), SampleTables.item(x, pl, 3, updatedAt = 300)),
        )
        val deviceB = NotebookTables(playlistItems = base + SampleTables.item(z, pl, 2, updatedAt = 200, updatedBy = DEVICE_B))
        val mergedOnA = deviceA.upserted(BackupMerger.merge(deviceA, deviceB).changes)
        val mergedOnB = deviceB.upserted(BackupMerger.merge(deviceB, deviceA).changes)
        assertThat(positions(mergedOnA)).isEqualTo(positions(mergedOnB))
        assertThat(positions(mergedOnA).values.toSet()).hasSize(3)
        // a later exchange of the original files changes nothing
        assertThat(BackupMerger.merge(mergedOnA, deviceB).changes.playlistItems).isEmpty()
        assertThat(BackupMerger.merge(mergedOnB, deviceA).changes.playlistItems).isEmpty()
    }
}
