package org.cog.hymnchtv.persistance.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.cog.hymnchtv.notebook.data.NotebookConverters
import org.cog.hymnchtv.notebook.data.dao.FavoriteDao
import org.cog.hymnchtv.notebook.data.dao.NoteDao
import org.cog.hymnchtv.notebook.data.dao.PlaylistDao
import org.cog.hymnchtv.notebook.data.dao.PlaylistItemDao
import org.cog.hymnchtv.notebook.data.dao.SingLogDao
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.BuildConfig
import org.cog.hymnchtv.persistance.room.dao.EnglishLyricsDao
import org.cog.hymnchtv.persistance.room.dao.HymnHistoryDao
import org.cog.hymnchtv.persistance.room.dao.MediaRecordDao
import org.cog.hymnchtv.persistance.room.entity.EnglishLyricsEntity
import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity
import org.cog.hymnchtv.persistance.room.entity.MediaRecordEntity

/**
 * The one database of the app. Schema is version 1 on purpose: the project is unreleased, so there is no
 * migration (see docs/superpowers/plans/2026-10-02-room-unification.md, section 2.5).
 *
 * Production code gets the process-wide instance from [getInstance]; [build] with a file name is for tests that
 * need their own file-backed database, [inMemory] for the rest. Never call [build] for the production file
 * outside [getInstance]: two instances on one file break the single-writer assumption.
 */
@Database(
    entities = [
        MediaRecordEntity::class, HymnHistoryEntity::class, EnglishLyricsEntity::class,
        FavoriteEntity::class, SingLogEntity::class, NoteEntity::class,
        PlaylistEntity::class, PlaylistItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(NotebookConverters::class)
abstract class HymnchtvDatabase : RoomDatabase() {
    abstract fun mediaRecordDao(): MediaRecordDao
    abstract fun hymnHistoryDao(): HymnHistoryDao
    abstract fun englishLyricsDao(): EnglishLyricsDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun singLogDao(): SingLogDao
    abstract fun noteDao(): NoteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao

    companion object {
        const val FILE_NAME = "hymnchtv.db"

        @Volatile
        private var instance: HymnchtvDatabase? = null

        /**
         * The process-wide database. Main-thread queries stay allowed as a transition (plan section 2.4): the
         * listed call sites must move to AppExecutors.io before 1.0. The debug-only 1.0 gate turns this off with
         * `-PstrictDbThread` (see [BuildConfig.ALLOW_MAIN_THREAD_DB] and the DatabaseBackend class doc).
         */
        @JvmStatic
        fun getInstance(context: Context): HymnchtvDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, FILE_NAME, BuildConfig.ALLOW_MAIN_THREAD_DB).also { instance = it }
            }

        /**
         * A file-backed database. Write-ahead logging is set explicitly (Room's default may pick another mode).
         * With [allowMainThreadQueries] false, Room throws IllegalStateException for a query on the main thread.
         */
        @JvmStatic
        @JvmOverloads
        fun build(
            context: Context,
            fileName: String = FILE_NAME,
            allowMainThreadQueries: Boolean = true,
        ): HymnchtvDatabase =
            Room.databaseBuilder(context.applicationContext, HymnchtvDatabase::class.java, fileName)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .apply { if (allowMainThreadQueries) allowMainThreadQueries() }
                .build()

        /** A throw-away in-memory database for tests. */
        @JvmStatic
        fun inMemory(context: Context): HymnchtvDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, HymnchtvDatabase::class.java).build()
    }
}
