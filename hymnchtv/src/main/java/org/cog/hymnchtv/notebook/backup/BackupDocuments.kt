package org.cog.hymnchtv.notebook.backup

import android.content.ContentResolver
import android.net.Uri
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
          val stream = openOutput(resolver, uri)
              ?: return@withContext ExportResult.Failure(BackupError.IO, "Cannot open document for writing")
          try {
              stream.use { service.exportTo(it) }
          } catch (e: IOException) {
              Timber.w(e, "Closing exported document failed")
              ExportResult.Failure(BackupError.IO, e.message.orEmpty())
          }
      }

  suspend fun importFrom(resolver: ContentResolver, uri: Uri, service: BackupService): ImportResult =
      withContext(Dispatchers.IO) {
          val stream = openInput(resolver, uri)
              ?: return@withContext ImportResult.Failure(BackupError.IO, "Cannot open document for reading")
          try {
              stream.use { service.importFrom(it) }
          } catch (e: IOException) {
              Timber.w(e, "Closing imported document failed")
              ImportResult.Failure(BackupError.IO, e.message.orEmpty())
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
