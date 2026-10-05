package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.model.Clock
import org.json.JSONObject
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BackupServiceTest {
    private val now = 1_790_733_600_000L
    private val clock = Clock { now }

    private fun TestScope.service(store: BackupStore, maxBytes: Int = BackupService.DEFAULT_MAX_BYTES) =
        BackupService(store, clock, "2.9.2", UnconfinedTestDispatcher(testScheduler), maxBytes)

    private suspend fun BackupService.exportBytes(): ByteArray {
        val out = ByteArrayOutputStream()
        assertThat(exportTo(out)).isInstanceOf(ExportResult.Success::class.java)
        return out.toByteArray()
    }

    private fun assertSameRows(actual: NotebookTables, expected: NotebookTables) {
        assertThat(actual.favorites).containsExactlyElementsIn(expected.favorites)
        assertThat(actual.singLogs).containsExactlyElementsIn(expected.singLogs)
        assertThat(actual.notes).containsExactlyElementsIn(expected.notes)
        assertThat(actual.playlists).containsExactlyElementsIn(expected.playlists)
        assertThat(actual.playlistItems).containsExactlyElementsIn(expected.playlistItems)
    }

    @Test
    fun exportWritesHeaderAndAllRows() = runTest {
        val out = ByteArrayOutputStream()
        assertThat(service(InMemoryBackupStore(SampleTables.full())).exportTo(out)).isEqualTo(ExportResult.Success(10))
        val json = JSONObject(out.toString(Charsets.UTF_8.name()))
        assertThat(json.getInt("schemaVersion")).isEqualTo(1)
        assertThat(json.getLong("exportedAt")).isEqualTo(now)
        assertThat(json.getString("appVersionName")).isEqualTo("2.9.2")
    }

    @Test
    fun anExportOverTheCapFailsAsTooLargeAndWritesNothing() = runTest {
        val size = service(InMemoryBackupStore(SampleTables.full())).exportBytes().size
        val out = ByteArrayOutputStream()
        val result = service(InMemoryBackupStore(SampleTables.full()), maxBytes = size - 1).exportTo(out)
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.TOO_LARGE)
        assertThat(out.size()).isEqualTo(0)
    }

    @Test
    fun anExportAtTheCapSucceedsAndImportsBackWithTheSameCap() = runTest {
        val size = service(InMemoryBackupStore(SampleTables.full())).exportBytes().size
        val capped = service(InMemoryBackupStore(SampleTables.full()), maxBytes = size)
        val bytes = capped.exportBytes()
        val target = InMemoryBackupStore()
        val result = service(target, maxBytes = size).importFrom(ByteArrayInputStream(bytes))
        assertThat(result).isEqualTo(ImportResult.Success(MergeStats(10, 0, 0), SkippedRows.NONE))
    }

    @Test
    fun exportThenImportIntoAnEmptyStoreRestoresEverything() = runTest {
        val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
        val target = InMemoryBackupStore()
        val result = service(target).importFrom(ByteArrayInputStream(bytes))
        assertThat(result).isEqualTo(ImportResult.Success(MergeStats(10, 0, 0), SkippedRows.NONE))
        assertSameRows(target.tables, SampleTables.full())
    }

    @Test
    fun importingTwiceIsIdempotent() = runTest {
        val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
        val target = InMemoryBackupStore()
        service(target).importFrom(ByteArrayInputStream(bytes))
        assertThat(service(target).importFrom(ByteArrayInputStream(bytes)))
            .isEqualTo(ImportResult.Success(MergeStats(0, 0, 10), SkippedRows.NONE))
    }

    @Test
    fun notJsonIsReported() = runTest {
        val result = service(InMemoryBackupStore()).importFrom(ByteArrayInputStream("hello".toByteArray()))
        assertThat((result as ImportResult.Failure).error).isEqualTo(BackupError.NOT_JSON)
    }

    @Test
    fun oversizedInputIsRejectedAndExactLimitAccepted() = runTest {
        val bytes = service(InMemoryBackupStore()).exportBytes()
        val tooSmall = service(InMemoryBackupStore(), maxBytes = bytes.size - 1)
        assertThat((tooSmall.importFrom(ByteArrayInputStream(bytes)) as ImportResult.Failure).error)
            .isEqualTo(BackupError.TOO_LARGE)
        val exact = service(InMemoryBackupStore(), maxBytes = bytes.size)
        assertThat(exact.importFrom(ByteArrayInputStream(bytes))).isInstanceOf(ImportResult.Success::class.java)
    }

    @Test
    fun writeFailureIsIo() = runTest {
        val broken = object : OutputStream() {
            override fun write(b: Int) = throw IOException("disk full")
            override fun write(b: ByteArray, off: Int, len: Int) = throw IOException("disk full")
        }
        val result = service(InMemoryBackupStore(SampleTables.full())).exportTo(broken)
        assertThat(result).isEqualTo(ExportResult.Failure(BackupError.IO, "disk full"))
    }

    @Test
    fun storeFailureIsStorage() = runTest {
        val store = InMemoryBackupStore(SampleTables.full()).apply { failNext = true }
        val result = service(store).exportTo(ByteArrayOutputStream())
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.STORAGE)

        val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
        val target = InMemoryBackupStore().apply { failNext = true }
        val imported = service(target).importFrom(ByteArrayInputStream(bytes))
        assertThat((imported as ImportResult.Failure).error).isEqualTo(BackupError.STORAGE)
        assertThat(target.tables).isEqualTo(NotebookTables.EMPTY)
    }

    @Test
    fun defaultLimitIsFourMegabytes() {
        assertThat(BackupService.DEFAULT_MAX_BYTES).isEqualTo(4 * 1024 * 1024)
    }

    @Test
    fun outOfMemoryDuringExportOrImportIsTooLargeNotACrash() = runTest {
        val oom = OutOfMemoryError("simulated")
        val exporting = InMemoryBackupStore(SampleTables.full()).apply { failNext = true; failure = oom }
        assertThat((service(exporting).exportTo(ByteArrayOutputStream()) as ExportResult.Failure).error)
            .isEqualTo(BackupError.TOO_LARGE)

        val bytes = service(InMemoryBackupStore(SampleTables.full())).exportBytes()
        val importing = InMemoryBackupStore().apply { failNext = true; failure = oom }
        assertThat((service(importing).importFrom(ByteArrayInputStream(bytes)) as ImportResult.Failure).error)
            .isEqualTo(BackupError.TOO_LARGE)
    }

    @Test
    fun streamThatKeepsGoingPastTheLimitIsRejectedWithoutReadingItAll() = runTest {
        var served = 0L
        val endless = object : java.io.InputStream() {
            override fun read(): Int = 0.also { served++ }
            override fun read(b: ByteArray, off: Int, len: Int): Int = len.also { served += it }
        }
        val result = service(InMemoryBackupStore(), maxBytes = 1_000_000).importFrom(endless)
        assertThat((result as ImportResult.Failure).error).isEqualTo(BackupError.TOO_LARGE)
        assertThat(served).isLessThan(2_000_000L)
    }
}
