package org.cog.hymnchtv.ui.settings

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.notebook.Notebook
import org.cog.hymnchtv.notebook.backup.BackupCodec
import org.cog.hymnchtv.notebook.backup.BackupService
import org.cog.hymnchtv.notebook.backup.BackupSnapshot
import org.cog.hymnchtv.notebook.backup.NotebookTables
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.host.MainHost
import org.cog.hymnchtv.ui.settings.backup.BackupUiState
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Backup through the settings page (D-1 F3): results that arrive while the page is stopped or recreated, a cancelled picker,
 * an unreadable document, a file over the 4 MB limit, a content:// document (FileProvider, like a SAF provider) and an import
 * that merges into newer local rows, a deletion and a playlist position collision (BackupMerger, last writer wins).
 */
@RunWith(AndroidJUnit4::class)
class SettingsBackupFlowTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val graph by lazy { Notebook.get(ctx) }
    private val dir by lazy { File(ctx.cacheDir, "share/backup-flow-test").apply { mkdirs() } }
    private val db5 = HymnKey.of(HymnTypes.DB, 5)
    private val db6 = HymnKey.of(HymnTypes.DB, 6)
    private val created = mutableListOf<() -> Unit>()

    @Before fun setUp() = FragmentHost.grantLaunchPermissions(ctx.packageName)

    @After fun tearDown() {
        runBlocking { created.forEach { it() } }
        dir.deleteRecursively()
    }

    private fun contentUri(name: String): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(dir, name))

    private fun withSettings(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { it.findViewById<View>(R.id.btn_home_settings).performClick() }
            FragmentHost.eventually { assertThat(fragment(scenario)).isNotNull() }
            block(scenario)
        }
    }

    private fun fragment(s: ActivityScenario<MainActivity>): SettingsFragment? {
        var found: SettingsFragment? = null
        s.onActivity { found = it.supportFragmentManager.findFragmentByTag(MainHost.TAG_SETTINGS) as? SettingsFragment }
        return found
    }

    private fun runnerState(s: ActivityScenario<MainActivity>): BackupUiState {
        var state = BackupUiState()
        s.onActivity { state = ViewModelProvider(fragment(s)!!)[BackupViewModel::class.java].runner.state.value }
        return state
    }

    /** The shown dialog's text and the runner's pending message, read in one main-thread pass. */
    private fun dialogAndMessage(s: ActivityScenario<MainActivity>): Pair<String?, Any?> {
        var pair: Pair<String?, Any?> = null to null
        s.onActivity {
            val f = fragment(s)
            val text = f?.childFragmentManager?.findFragmentByTag(BackupResultDialog.TAG)?.arguments?.getString(BackupResultDialog.ARG_TEXT)
            pair = text to (f?.let { ViewModelProvider(it)[BackupViewModel::class.java].runner.state.value.message })
        }
        return pair
    }

    /** The message is never cleared before its dialog is there: either still pending, or shown and cleared. */
    private fun assertShownBeforeCleared(s: ActivityScenario<MainActivity>, expected: String) {
        var clearedWithoutDialog = false
        FragmentHost.eventually(10_000) {
            val (text, message) = dialogAndMessage(s)
            if (message == null && text != expected) clearedWithoutDialog = true
            assertThat(text).isEqualTo(expected)
        }
        assertThat(clearedWithoutDialog).isFalse()
        assertThat(dialogAndMessage(s)).isEqualTo(expected to null)
    }

    private fun shownResult(s: ActivityScenario<MainActivity>): String? {
        var text: String? = null
        s.onActivity {
            text = fragment(s)?.childFragmentManager?.findFragmentByTag(BackupResultDialog.TAG)?.arguments?.getString(BackupResultDialog.ARG_TEXT)
        }
        return text
    }

    @Test fun aResultArrivingWhileStoppedIsShownOnReturnAndTooLargeFilesAreRejected() {
        val big = File(dir, "big.json").apply { writeBytes(ByteArray(BackupService.DEFAULT_MAX_BYTES + 1) { ' '.code.toByte() }) }
        withSettings { s ->
            s.moveToState(Lifecycle.State.CREATED)
            s.onActivity { fragment(s)!!.onImportPicked(Uri.fromFile(big)) }
            FragmentHost.eventually(10_000) { assertThat(runnerState(s).running).isFalse() }
            assertThat(runnerState(s).message).isNotNull()
            assertThat(shownResult(s)).isNull()
            s.moveToState(Lifecycle.State.RESUMED)
            assertShownBeforeCleared(s, ctx.getString(R.string.backup_error_too_large))
        }
    }

    @Test fun aResultSurvivesRecreationAndAnUnreadableDocumentSaysSo() {
        withSettings { s ->
            s.moveToState(Lifecycle.State.CREATED)
            s.onActivity { fragment(s)!!.onImportPicked(contentUri("missing.json")) }
            FragmentHost.eventually(10_000) { assertThat(runnerState(s).message).isNotNull() }
            s.recreate()
            s.moveToState(Lifecycle.State.RESUMED)
            assertShownBeforeCleared(s, ctx.getString(R.string.backup_error_io))
        }
    }

    @Test fun cancellingThePickerStartsNothing() {
        withSettings { s ->
            s.onActivity {
                fragment(s)!!.onExportPicked(null)
                fragment(s)!!.onImportPicked(null)
            }
            assertThat(runnerState(s)).isEqualTo(BackupUiState())
            assertThat(shownResult(s)).isNull()
        }
    }

    @Test fun exportAndImportThroughAContentUriKeepALocalDeletion() {
        val note = runBlocking { graph.notes.add(db5, "內容 Uri 測試筆記") }
        created += { runBlocking { graph.notes.delete(note.id) } }
        val uri = contentUri("notebook.json")
        withSettings { s ->
            s.onActivity { fragment(s)!!.onExportPicked(uri) }
            FragmentHost.eventually(10_000) { assertThat(shownResult(s)).isNotNull() }
            assertThat(File(dir, "notebook.json").readText()).contains("內容 Uri 測試筆記")
            // deleted here after the export: the backup's older active copy must not bring it back
            runBlocking { graph.notes.delete(note.id) }
            s.onActivity { fragment(s)!!.onImportPicked(uri) }
            assertShownBeforeCleared(s, ctx.getString(R.string.backup_import_done, 0, 0))
            assertThat(runBlocking { graph.notes.findById(note.id) }).isNull()
        }
    }

    @Test fun importMergesByLastWriterWinsIncludingAPositionCollision() {
        val local = runBlocking { graph.playlists.createPlaylist("本機名稱") }
        created += { runBlocking { graph.playlists.delete(local.id) } }
        val localItem = runBlocking { checkNotNull(graph.playlists.addItem(local.id, db5)) }
        val edited = runBlocking { graph.notes.add(db5, "舊的內容") }
        val removedThere = runBlocking { graph.notes.add(db6, "對方刪掉的筆記") }
        created += { runBlocking { graph.notes.delete(edited.id); graph.notes.delete(removedThere.id) } }
        val later = 60_000L
        val incoming = NotebookTables(
            // older rename on the other device: the local name wins
            playlists = listOf(local.copy(name = "舊名稱", createdAt = local.createdAt - later, updatedAt = local.updatedAt - later)),
            // newer text on the other device wins; a newer deletion there wins too
            notes = listOf(
                edited.copy(body = "對方較新的內容", updatedAt = edited.updatedAt + later),
                removedThere.copy(updatedAt = removedThere.updatedAt + later, deletedAt = removedThere.updatedAt + later),
            ),
            // another device put db6 at the same slot as the local db5, but earlier: db5 keeps the slot, db6 moves after it
            playlistItems = listOf(
                localItem.copy(id = "00000000-0000-0000-0000-00000000d106", hymn = db6, updatedAt = localItem.updatedAt - later, createdAt = localItem.createdAt - later),
            ),
        )
        val file = File(dir, "merge.json").apply {
            writeText(BackupCodec.encode(BackupSnapshot(BackupCodec.CURRENT_SCHEMA_VERSION, System.currentTimeMillis(), "test", incoming)))
        }
        withSettings { s ->
            s.onActivity { fragment(s)!!.onImportPicked(Uri.fromFile(file)) }
            assertShownBeforeCleared(s, ctx.getString(R.string.backup_import_done, 1, 2))
        }
        runBlocking {
            assertThat(graph.playlists.findById(local.id)?.name).isEqualTo("本機名稱")
            assertThat(graph.notes.findById(edited.id)?.body).isEqualTo("對方較新的內容")
            assertThat(graph.notes.findById(removedThere.id)).isNull()
            assertThat(graph.playlists.items(local.id).map { it.hymn }).containsExactly(db5, db6).inOrder()
        }
    }
}
