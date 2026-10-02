package org.cog.hymnchtv.notebook

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.notebook.backup.BackupDocuments
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.backup.RoomBackupStore
import org.cog.hymnchtv.notebook.backup.SkippedRows
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
* Manual E2E driver (Task 13), run only through d1a-e2e.sh.
* Modes: seed, export, verifyEmpty, import, importAgain, verifySeeded, writeLoop, integrity.
*/
@RunWith(AndroidJUnit4::class)
class NotebookE2eTest {
  private val mode: String? = InstrumentationRegistry.getArguments().getString(ARG_MODE)
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val graph by lazy { Notebook.get(context) }
  private val file by lazy { File(context.filesDir, "e2e/notebook-e2e.json") }

  @Test
  fun scenario(): Unit = runBlocking {
      assumeTrue("Run only via am instrument -e $ARG_MODE <mode>", mode != null)
      when (mode) {
          "seed" -> seed()
          "export" -> export()
          "verifyEmpty" -> assertThat(tables().rowCount).isEqualTo(0)
          "import" -> importAndCheck(expectInserted = TOTAL_ROWS)
          "importAgain" -> importAndCheck(expectInserted = 0)
          "verifySeeded" -> verifySeeded(checkPrefs = true)
          "writeLoop" -> writeLoop()
          "integrity" -> integrity()
          else -> fail("Unknown mode $mode")
      }
  }

  private suspend fun tables() = RoomBackupStore(graph.database).readAll()

  /** The legacy app settings file (MainActivity.PREF_SETTINGS), included in Auto Backup. */
  private fun settings() = context.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

  private suspend fun seed() {
      assertThat(tables().rowCount).isEqualTo(0)
      graph.favorites.setFavorite(DB1, true)
      graph.favorites.setFavorite(FU1, true)
      graph.favorites.setFavorite(BB5, true)
      graph.favorites.setFavorite(BB5, false)
      graph.singLogs.record(DB1, T1, Occasion.LORDS_DAY, SingSource.MANUAL)
      graph.singLogs.record(DB1, T2, Occasion.HOME, SingSource.AUTO)
      val removed = graph.singLogs.record(FU1, T1, Occasion.SMALL_GROUP, SingSource.MANUAL)
      graph.singLogs.delete(removed.id)
      graph.notes.add(DB1, "主日唱這首，很受感動。")
      graph.notes.add(FU1, "附歌一的筆記")
      val playlist = graph.playlists.createPlaylist("主日 10/4")
      graph.playlists.addItem(playlist.id, DB1)
      graph.playlists.addItem(playlist.id, FU1)
      // A legacy-table row (not part of the notebook JSON backup); only Auto Backup carries it across a restore.
      withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().insert(LEGACY_HISTORY) }
      graph.prefs.setAutoRecordEnabled(false)
      settings().edit().putString(SETTINGS_MARKER_KEY, SETTINGS_MARKER_VALUE).commit()
      verifySeeded(checkPrefs = true)
  }

  private suspend fun export() {
      file.parentFile?.mkdirs()
      val result = BackupDocuments.exportTo(context.contentResolver, Uri.fromFile(file), graph.backup)
      assertThat(result).isEqualTo(ExportResult.Success(TOTAL_ROWS))
  }

  private suspend fun importAndCheck(expectInserted: Int) {
      val result = BackupDocuments.importFrom(context.contentResolver, Uri.fromFile(file), graph.backup)
      assertThat(result).isInstanceOf(ImportResult.Success::class.java)
      val success = result as ImportResult.Success
      assertThat(success.stats.inserted).isEqualTo(expectInserted)
      assertThat(success.stats.inserted + success.stats.updated + success.stats.unchanged).isEqualTo(TOTAL_ROWS)
      assertThat(success.skipped).isEqualTo(SkippedRows.NONE)
      verifySeeded(checkPrefs = false) // import never touches prefs
  }

  private suspend fun verifySeeded(checkPrefs: Boolean) {
      val all = tables()
      assertThat(all.favorites).hasSize(3)
      assertThat(all.singLogs).hasSize(3)
      assertThat(all.notes).hasSize(2)
      assertThat(all.playlists).hasSize(1)
      assertThat(all.playlistItems).hasSize(2)
      assertThat(graph.favorites.findAll().map { it.hymn }).containsExactly(DB1, FU1)
      assertThat(graph.favorites.isFavorite(BB5)).isFalse()
      if (checkPrefs) { // legacy rows are not in the JSON backup, so only the Auto Backup restore carries them
          val history = withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().listNewestFirst() }
          assertThat(history).contains(LEGACY_HISTORY)
      }
      assertThat(graph.singLogs.statsFor(DB1)).isEqualTo(SingStats(2, T2))
      assertThat(graph.singLogs.statsFor(FU1)).isEqualTo(SingStats(0, null))
      assertThat(graph.notes.findByHymn(FU1).map { it.body }).containsExactly("附歌一的筆記")
      val playlist = graph.playlists.findAll().single()
      assertThat(graph.playlists.items(playlist.id).map { it.hymn }).containsExactly(DB1, FU1).inOrder()
      if (checkPrefs) {
          assertThat(graph.prefs.autoRecordEnabled).isFalse()
          assertThat(settings().getString(SETTINGS_MARKER_KEY, null)).isEqualTo(SETTINGS_MARKER_VALUE)
      }
  }

  /** Keeps writing for ~30 s so the script can trigger Auto Backup mid-write. */
  private suspend fun writeLoop() {
      repeat(WRITE_LOOP_COUNT) { i ->
          graph.singLogs.record(DB1, T2 + 60_000L * (i + 1), Occasion.HOME, SingSource.MANUAL)
          delay(100)
      }
  }

  private suspend fun integrity() {
      val check = withContext(Dispatchers.IO) {
          graph.database.openHelper.writableDatabase.query("PRAGMA integrity_check").use { cursor ->
              cursor.moveToFirst()
              cursor.getString(0)
          }
      }
      assertThat(check).isEqualTo("ok")
      assertThat(tables().singLogs.size).isAtLeast(3)
      assertThat(graph.favorites.findAll()).hasSize(2)
      val history = withContext(Dispatchers.IO) { graph.database.hymnHistoryDao().listNewestFirst() }
      assertThat(history).contains(LEGACY_HISTORY)
  }

  private companion object {
      const val ARG_MODE = "notebookE2e"
      const val TOTAL_ROWS = 11 // favorites 3 + sing logs 3 + notes 2 + playlists 1 + items 2
      const val T1 = 1_790_733_600_000L
      const val T2 = T1 + 86_400_000L
      const val WRITE_LOOP_COUNT = 300
      const val SETTINGS_MARKER_KEY = "notebook_e2e_marker"
      const val SETTINGS_MARKER_VALUE = "restored"
      val LEGACY_HISTORY = HymnHistoryEntity("hymn_db", 12, false, "legacy-row", 1_759_300_000_000L)
      val DB1 = HymnKey.of(HymnTypes.DB, 1)
      val FU1 = HymnKey.of(HymnTypes.DB, 781)
      val BB5 = HymnKey.of(HymnTypes.BB, 5)
  }
}
