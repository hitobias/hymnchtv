package org.cog.hymnchtv.notebook.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.model.Clock
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

/** Export/import over plain streams; the caller owns (opens and closes) the streams. Never throws except cancellation. */
class BackupService(
    private val store: BackupStore,
    private val clock: Clock,
    private val appVersionName: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxBytes: Int = DEFAULT_MAX_BYTES,
    private val limits: BackupLimits = BackupLimits(),
) {
    suspend fun exportTo(output: OutputStream): ExportResult = withContext(ioDispatcher) {
        guarded<ExportResult>(onError = { error, e -> ExportResult.Failure(error, e.message.orEmpty()) }) {
            val tables = store.readAll()
            val snapshot = BackupSnapshot(BackupCodec.CURRENT_SCHEMA_VERSION, clock.nowMillis(), appVersionName, tables)
            output.write(BackupCodec.encode(snapshot).toByteArray(Charsets.UTF_8))
            output.flush()
            ExportResult.Success(tables.rowCount)
        }
    }

    /** Decodes (rows > 24 h in the future are skipped), merges with local rows and writes in one transaction. Prefs are untouched. */
    suspend fun importFrom(input: InputStream): ImportResult = withContext(ioDispatcher) {
        guarded<ImportResult>(onError = { error, e -> ImportResult.Failure(error, e.message.orEmpty()) }) {
            val bytes = readLimited(input, maxBytes)
            if (bytes == null) {
                ImportResult.Failure(BackupError.TOO_LARGE, "Backup larger than $maxBytes bytes")
            } else {
                when (val decoded = BackupCodec.decode(String(bytes, Charsets.UTF_8), clock.nowMillis(), limits)) {
                    is DecodeResult.Failure -> ImportResult.Failure(decoded.error, decoded.detail)
                    is DecodeResult.Success -> {
                        val stats = store.mergeAtomically { local ->
                            val merged = BackupMerger.merge(local, decoded.snapshot.tables)
                            Planned(merged.changes, merged.stats)
                        }
                        ImportResult.Success(stats, decoded.skipped)
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 16 * 1024 * 1024
    }
}

/** Maps I/O errors to IO and anything else (e.g. SQLite) to STORAGE; cancellation propagates. */
private inline fun <T> guarded(onError: (BackupError, Exception) -> T, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    Timber.w(e, "Notebook backup I/O failed")
    onError(BackupError.IO, e)
} catch (e: Exception) {
    Timber.e(e, "Notebook backup failed")
    onError(BackupError.STORAGE, e)
}

private const val BUFFER_SIZE = 64 * 1024

/** Reads the whole stream if it is at most [limit] bytes; null when longer. */
internal fun readLimited(input: InputStream, limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return out.toByteArray()
        total += read
        if (total > limit) return null
        out.write(buffer, 0, read)
    }
}
