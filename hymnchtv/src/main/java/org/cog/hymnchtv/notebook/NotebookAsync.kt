package org.cog.hymnchtv.notebook

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cog.hymnchtv.notebook.backup.BackupIo
import org.cog.hymnchtv.notebook.backup.ExportResult
import org.cog.hymnchtv.notebook.backup.ImportResult
import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.record.SingTracker
import org.cog.hymnchtv.notebook.repo.FavoriteRepository
import org.cog.hymnchtv.notebook.repo.SingLogRepository
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/** Result of a NotebookAsync call. Java: `outcome.getOrNull()` / `outcome.errorOrNull()`. */
sealed class Outcome<out T> {
  data class Ok<out T>(val value: T) : Outcome<T>()
  data class Err(val error: Throwable) : Outcome<Nothing>()

  fun getOrNull(): T? = (this as? Ok<T>)?.value
  fun errorOrNull(): Throwable? = (this as? Err)?.error
}

fun interface NotebookCallback<T> {
  fun onResult(outcome: Outcome<T>)
}

fun interface Cancellable {
  fun cancel()
}

/**
* Callback API for Java callers (ContentHandler, MainActivity in the UI wave).
* Repository work runs on [workDispatcher] (Dispatchers.IO in production; e.g. RoomSingLogRepository.create()
* calls device.deviceId(), which may commit SharedPreferences once, so it must never run on the main thread).
* Every callback runs on [callbackDispatcher] (the main thread in production) and is never invoked after
* cancel(); activities cancel their calls in onDestroy so no result reaches a destroyed view.
*/
class NotebookAsync(
  private val favorites: FavoriteRepository,
  private val singLogs: SingLogRepository,
  private val prefs: NotebookPrefs,
  private val tracker: SingTracker,
  private val backupIo: BackupIo,
  callbackDispatcher: CoroutineDispatcher,
  private val workDispatcher: CoroutineDispatcher,
) {
  private val scope = CoroutineScope(SupervisorJob() + callbackDispatcher)

  // ---- favorites ----

  fun isFavorite(key: HymnKey, callback: NotebookCallback<Boolean>): Cancellable =
      call(callback) { favorites.isFavorite(key) }

  fun toggleFavorite(key: HymnKey, callback: NotebookCallback<Boolean>): Cancellable =
      call(callback) { favorites.toggle(key) }

  fun favorites(callback: NotebookCallback<List<FavoriteEntity>>): Cancellable =
      call(callback) { favorites.findAll() }

  // ---- sing logs ----

  fun singStats(key: HymnKey, callback: NotebookCallback<SingStats>): Cancellable =
      call(callback) { singLogs.statsFor(key) }

  fun singLogsOf(key: HymnKey, callback: NotebookCallback<List<SingLogEntity>>): Cancellable =
      call(callback) { singLogs.findByHymn(key) }

  /**
   * Manual entry. Deliberately bypasses the 3-hour dedupe (the user says they sang it, e.g. twice in one
   * meeting); the UI may warn using singStats(). Also remembers [occasion] as the default for future auto logs.
   */
  fun recordManual(
      key: HymnKey,
      occasion: Occasion,
      sungAt: Long,
      callback: NotebookCallback<SingLogEntity>,
  ): Cancellable = call(callback) {
      singLogs.record(key, sungAt, occasion, SingSource.MANUAL).also { prefs.setLastChosenOccasion(occasion) }
  }

  /** User correction of a log; remembers the corrected occasion. Null when the log no longer exists. */
  fun updateSingLog(log: SingLogEntity, callback: NotebookCallback<SingLogEntity?>): Cancellable =
      call(callback) { singLogs.update(log)?.also { prefs.setLastChosenOccasion(it.occasion) } }

  fun deleteSingLog(id: String, callback: NotebookCallback<Boolean>): Cancellable =
      call(callback) { singLogs.delete(id) }

  // ---- auto record: main thread, non-blocking; invalid or non-canonical hymn numbers are ignored ----

  fun onHymnVisible(hymnType: String?, hymnNo: Int) {
      HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onHymnVisible)
  }

  fun onHymnHidden(hymnType: String?, hymnNo: Int) {
      HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onHymnHidden)
  }

  fun onMediaCompleted(hymnType: String?, hymnNo: Int) {
      HymnKey.ofOrNull(hymnType, hymnNo)?.let(tracker::onMediaCompleted)
  }

  /** Delivers each new AUTO log on the callback dispatcher (e.g. to refresh the sing count or offer undo). */
  fun observeAutoRecorded(listener: NotebookCallback<SingLogEntity>): Cancellable {
      val job = scope.launch { tracker.recorded.collect { listener.onResult(Outcome.Ok(it)) } }
      return Cancellable { job.cancel() }
  }

  fun isAutoRecordEnabled(): Boolean = prefs.autoRecordEnabled

  fun setAutoRecordEnabled(enabled: Boolean) = prefs.setAutoRecordEnabled(enabled)

  // ---- backup (Uris come from ActivityResultContracts.CreateDocument / OpenDocument) ----

  fun exportTo(uri: Uri, callback: NotebookCallback<ExportResult>): Cancellable =
      call(callback) { backupIo.exportTo(uri) }

  fun importFrom(uri: Uri, callback: NotebookCallback<ImportResult>): Cancellable =
      call(callback) { backupIo.importFrom(uri) }

  private fun <T> call(callback: NotebookCallback<T>, block: suspend () -> T): Cancellable {
      val job = scope.launch {
          val outcome: Outcome<T> = try {
              Outcome.Ok(withContext(workDispatcher) { block() })
          } catch (e: CancellationException) {
              throw e
          } catch (e: Exception) {
              Timber.w(e, "Notebook call failed")
              Outcome.Err(e)
          }
          callback.onResult(outcome)
      }
      return Cancellable { job.cancel() }
  }
}
