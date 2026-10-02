package org.cog.hymnchtv.notebook.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Export/import by document Uri; implemented by [UriBackupIo]. */
interface BackupIo {
    suspend fun exportTo(uri: Uri): ExportResult
    suspend fun importFrom(uri: Uri): ImportResult
}

/**
 * Storage Access Framework glue. The UI obtains Uris with
 * ActivityResultContracts.CreateDocument(MIME_TYPE) / OpenDocument() and passes them here.
 */
object BackupDocuments {
    const val MIME_TYPE = "application/json"

    /** Some providers label .json files as text/plain or application/octet-stream. */
    @JvmField
    val OPEN_MIME_TYPES: Array<String> = arrayOf(MIME_TYPE, "text/plain", "application/octet-stream")

    suspend fun exportTo(resolver: ContentResolver, uri: Uri, service: BackupService): ExportResult =
        withContext(Dispatchers.IO) {
            exportVia(
                open = { openOutput(resolver, uri) },
                discard = {
                    if (!DocumentsContract.deleteDocument(resolver, uri)) Timber.w("Provider did not delete %s", uri)
                },
                service = service,
            )
        }

    suspend fun importFrom(resolver: ContentResolver, uri: Uri, service: BackupService): ImportResult =
        withContext(Dispatchers.IO) { importVia(open = { openInput(resolver, uri) }, service = service) }

    /**
     * Writes through [open]. Unless the export succeeds (including on failure, exception or cancellation) the empty or
     * partial document is removed with [discard], best effort, so a failed export never leaves a file that looks like a backup.
     */
    internal suspend fun exportVia(open: () -> OutputStream?, discard: () -> Unit, service: BackupService): ExportResult {
        var result: ExportResult? = null
        try {
            val stream = open()
            result = if (stream == null) {
                ExportResult.Failure(BackupError.IO, "Cannot open document for writing")
            } else {
                try {
                    stream.use { service.exportTo(it) }
                } catch (e: IOException) {
                    Timber.w(e, "Closing exported document failed")
                    ExportResult.Failure(BackupError.IO, e.message.orEmpty())
                }
            }
            return result
        } finally {
            if (result !is ExportResult.Success) discardQuietly(discard)
        }
    }

    /** A failure to close the input after a completed import is only logged: the rows are already committed. */
    internal suspend fun importVia(open: () -> InputStream?, service: BackupService): ImportResult {
        val stream = open() ?: return ImportResult.Failure(BackupError.IO, "Cannot open document for reading")
        try {
            return service.importFrom(stream)
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                Timber.w(e, "Closing imported document failed")
            }
        }
    }

    private fun discardQuietly(discard: () -> Unit) {
        try {
            discard()
        } catch (e: Exception) {
            Timber.w(e, "Could not delete the incomplete backup document")
        }
    }

    private fun openOutput(resolver: ContentResolver, uri: Uri): OutputStream? = try {
        openTruncating(resolver, uri)
    } catch (e: FileNotFoundException) {
        Timber.w(e, "Cannot open %s for writing", uri)
        null
    } catch (e: SecurityException) {
        Timber.w(e, "No permission to write %s", uri)
        null
    }

    /** "wt" truncates an existing document; a few providers only accept "w". */
    private fun openTruncating(resolver: ContentResolver, uri: Uri): OutputStream? = try {
        resolver.openOutputStream(uri, "wt")
    } catch (e: IllegalArgumentException) {
        resolver.openOutputStream(uri, "w")
    }

    private fun openInput(resolver: ContentResolver, uri: Uri): InputStream? = try {
        resolver.openInputStream(uri)
    } catch (e: FileNotFoundException) {
        Timber.w(e, "Cannot open %s for reading", uri)
        null
    } catch (e: SecurityException) {
        Timber.w(e, "No permission to read %s", uri)
        null
    }
}

class UriBackupIo(private val resolver: ContentResolver, private val service: BackupService) : BackupIo {
    override suspend fun exportTo(uri: Uri): ExportResult = BackupDocuments.exportTo(resolver, uri, service)
    override suspend fun importFrom(uri: Uri): ImportResult = BackupDocuments.importFrom(resolver, uri, service)
}
