package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.cog.hymnchtv.notebook.model.Clock
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class BackupDocumentsTest {
    private fun kotlinx.coroutines.test.TestScope.service(store: BackupStore = InMemoryBackupStore(SampleTables.full())) =
        BackupService(store, Clock { 1_790_733_600_000L }, "2.9.2", UnconfinedTestDispatcher(testScheduler))

    private class Discards {
        var count = 0
        val action: () -> Unit = { count++ }
    }

    @Test
    fun successfulExportKeepsTheDocument() = runTest {
        val discards = Discards()
        val out = ByteArrayOutputStream()
        val result = BackupDocuments.exportVia({ out }, discards.action, service())
        assertThat(result).isInstanceOf(ExportResult.Success::class.java)
        assertThat(discards.count).isEqualTo(0)
    }

    @Test
    fun failedExportDeletesTheDocument() = runTest {
        val discards = Discards()
        val failing = InMemoryBackupStore(SampleTables.full()).apply { failNext = true }
        val result = BackupDocuments.exportVia({ ByteArrayOutputStream() }, discards.action, service(failing))
        assertThat(result).isInstanceOf(ExportResult.Failure::class.java)
        assertThat(discards.count).isEqualTo(1)
    }

    @Test
    fun anExportOverTheSizeCapDeletesTheDocumentAndSaysTooLarge() = runTest {
        val discards = Discards()
        val tiny = BackupService(
            InMemoryBackupStore(SampleTables.full()), Clock { 1_790_733_600_000L }, "2.9.2",
            UnconfinedTestDispatcher(testScheduler), maxBytes = 100,
        )
        val out = ByteArrayOutputStream()
        val result = BackupDocuments.exportVia({ out }, discards.action, tiny)
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.TOO_LARGE)
        assertThat(out.size()).isEqualTo(0)
        assertThat(discards.count).isEqualTo(1)
    }

    @Test
    fun documentThatCannotBeOpenedIsDeleted() = runTest {
        val discards = Discards()
        val result = BackupDocuments.exportVia({ null }, discards.action, service())
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.IO)
        assertThat(discards.count).isEqualTo(1)
    }

    @Test
    fun failureWhileClosingTheDocumentDeletesIt() = runTest {
        val discards = Discards()
        val closeFails = object : ByteArrayOutputStream() {
            override fun close() = throw IOException("close failed")
        }
        val result = BackupDocuments.exportVia({ closeFails }, discards.action, service())
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.IO)
        assertThat(discards.count).isEqualTo(1)
    }

    @Test
    fun exceptionsAndCancellationDeleteTheDocumentAndPropagate() = runTest {
        val discards = Discards()
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { BackupDocuments.exportVia({ error("boom") }, discards.action, service()) }
        }
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                BackupDocuments.exportVia({ throw CancellationException("cancelled") }, discards.action, service())
            }
        }
        assertThat(discards.count).isEqualTo(2)
    }

    @Test
    fun aFailingDeleteDoesNotMaskTheResult() = runTest {
        val failing = InMemoryBackupStore(SampleTables.full()).apply { failNext = true }
        val result = BackupDocuments.exportVia({ ByteArrayOutputStream() }, { throw SecurityException("no") }, service(failing))
        assertThat((result as ExportResult.Failure).error).isEqualTo(BackupError.STORAGE)
    }

    @Test
    fun closeFailureAfterASuccessfulImportKeepsTheSuccess() = runTest {
        val bytes = ByteArrayOutputStream().also { service().exportTo(it) }.toByteArray()
        val closeFails = object : InputStream() {
            private val inner = ByteArrayInputStream(bytes)
            override fun read(): Int = inner.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len)
            override fun close() = throw IOException("close failed")
        }
        val result = BackupDocuments.importVia({ closeFails }, service(InMemoryBackupStore()))
        assertThat(result).isInstanceOf(ImportResult.Success::class.java)
    }

    @Test
    fun unreadableImportDocumentIsIo() = runTest {
        val result = BackupDocuments.importVia({ null }, service())
        assertThat((result as ImportResult.Failure).error).isEqualTo(BackupError.IO)
    }
}
