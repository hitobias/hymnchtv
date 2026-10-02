package org.cog.hymnchtv.notebook

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.cog.hymnchtv.BuildConfig
import org.cog.hymnchtv.notebook.backup.BackupService
import org.cog.hymnchtv.notebook.backup.RoomBackupStore
import org.cog.hymnchtv.notebook.backup.UriBackupIo
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.cog.hymnchtv.notebook.model.Clock
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.record.SingTracker
import org.cog.hymnchtv.notebook.repo.FavoriteRepository
import org.cog.hymnchtv.notebook.repo.NoteRepository
import org.cog.hymnchtv.notebook.repo.PlaylistRepository
import org.cog.hymnchtv.notebook.repo.SingLogRepository
import org.cog.hymnchtv.notebook.repo.room.RoomFavoriteRepository
import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
import org.cog.hymnchtv.notebook.repo.room.RoomPlaylistRepository
import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
import org.cog.hymnchtv.notebook.settings.NotebookPrefs
import org.cog.hymnchtv.notebook.settings.SharedPrefsNotebookPrefs
import timber.log.Timber

/**
* Everything the notebook needs, built once per process. [appScope] owns background work (SingTracker timers
* and writes): SupervisorJob + Dispatchers.Default, process lifetime, never cancelled explicitly.
* Kotlin UI uses the repositories directly; Java uses [async].
*/
class NotebookGraph internal constructor(
  val database: HymnchtvDatabase,
  val appScope: CoroutineScope,
  val favorites: FavoriteRepository,
  val singLogs: SingLogRepository,
  val notes: NoteRepository,
  val playlists: PlaylistRepository,
  val prefs: NotebookPrefs,
  val tracker: SingTracker,
  val backup: BackupService,
  val async: NotebookAsync,
)

/** Lazy process-wide entry point; no HymnsApp change needed. Java: `Notebook.async(context)`. */
object Notebook {
  @Volatile
  private var graph: NotebookGraph? = null

  @JvmStatic
  fun get(context: Context): NotebookGraph =
      graph ?: synchronized(this) { graph ?: create(context.applicationContext).also { graph = it } }

  @JvmStatic
  fun async(context: Context): NotebookAsync = get(context).async

  private fun create(app: Context): NotebookGraph {
      val clock = Clock.SYSTEM
      val ids = IdGenerator.RANDOM_UUID
      val db = HymnchtvDatabase.getInstance(app)
      val appScope = CoroutineScope(
          SupervisorJob() + Dispatchers.Default +
              CoroutineExceptionHandler { _, e -> Timber.e(e, "Notebook background task failed") },
      )
      val prefs = SharedPrefsNotebookPrefs(app)
      val favorites = RoomFavoriteRepository(db, clock, prefs)
      val singLogs = RoomSingLogRepository(db, clock, ids, prefs)
      val tracker = SingTracker(singLogs, prefs, clock, appScope)
      val backup = BackupService(RoomBackupStore(db), clock, BuildConfig.VERSION_NAME)
      val async = NotebookAsync(
          favorites, singLogs, prefs, tracker, UriBackupIo(app.contentResolver, backup),
          callbackDispatcher = Dispatchers.Main.immediate,
          workDispatcher = Dispatchers.IO,
      )
      return NotebookGraph(
          database = db,
          appScope = appScope,
          favorites = favorites,
          singLogs = singLogs,
          notes = RoomNoteRepository(db, clock, ids, prefs),
          playlists = RoomPlaylistRepository(db, clock, ids, prefs),
          prefs = prefs,
          tracker = tracker,
          backup = backup,
          async = async,
      )
  }
}
