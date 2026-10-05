package org.cog.hymnchtv.notebook.backup

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.repo.TestClock
import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
import org.cog.hymnchtv.notebook.repo.testDevice
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The manual backup of D-1 F3 end to end on a device: notes and a playlist exported through a document Uri come back into
 * another database by merging, in order; importing the same file again adds nothing.
 */
@RunWith(AndroidJUnit4::class)
class NotebookBackupRoundTripTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val db6 = HymnKey.of(HymnTypes.DB, 6)

    @Test
    fun notesAndPlaylistsSurviveAnExportAndImport(): Unit = runBlocking {
        val source = HymnchtvDatabase.inMemory(ctx)
        val target = HymnchtvDatabase.inMemory(ctx)
        val file = File(ctx.cacheDir, "notebook-roundtrip.json")
        try {
            val clock = TestClock(1_790_733_600_000L)
            val ids = IdGenerator.RANDOM_UUID
            RoomNoteRepository(source, clock, ids, testDevice).add(db5, "主日的筆記\n第二行")
            val playlists = RoomPlaylistRepository(source, clock, ids, testDevice)
            val sunday = playlists.createPlaylist("主日")
            playlists.addItem(sunday.id, db6)
            playlists.addItem(sunday.id, db5)

            val exported = BackupDocuments.exportTo(ctx.contentResolver, Uri.fromFile(file), BackupService(RoomBackupStore(source), clock, "test"))
            assertThat(exported).isEqualTo(ExportResult.Success(4))

            val service = BackupService(RoomBackupStore(target), clock, "test")
            val first = BackupDocuments.importFrom(ctx.contentResolver, Uri.fromFile(file), service) as ImportResult.Success
            assertThat(first.stats.inserted).isEqualTo(4)
            val again = BackupDocuments.importFrom(ctx.contentResolver, Uri.fromFile(file), service) as ImportResult.Success
            assertThat(again.stats.inserted).isEqualTo(0)

            assertThat(RoomNoteRepository(target, clock, ids, testDevice).findByHymn(db5).map { it.body }).containsExactly("主日的筆記\n第二行")
            val restored = RoomPlaylistRepository(target, clock, ids, testDevice)
            val list = restored.findAll().single()
            assertThat(list.name).isEqualTo("主日")
            assertThat(restored.items(list.id).map { it.hymn }).containsExactly(db6, db5).inOrder()
        } finally {
            source.close()
            target.close()
            file.delete()
        }
    }
}
