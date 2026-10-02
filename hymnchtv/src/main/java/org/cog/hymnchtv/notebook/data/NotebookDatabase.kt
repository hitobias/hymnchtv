package org.cog.hymnchtv.notebook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

/** The notebook database, separate from the legacy DatabaseBackend (dbHymnApp.db). */
@Database(
    entities = [
        FavoriteEntity::class, SingLogEntity::class, NoteEntity::class,
        PlaylistEntity::class, PlaylistItemEntity::class,
    ],
    version = NotebookDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(NotebookConverters::class)
abstract class NotebookDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun singLogDao(): SingLogDao
    abstract fun noteDao(): NoteDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistItemDao

    companion object {
        const val VERSION = 1

        /** Referenced by res/xml/notebook_*.xml backup rules (asserted by BackupRulesTest). */
        const val FILE_NAME = "notebook.db"

        /**
         * TRUNCATE keeps the database in a single file so backup rules need no -wal/-shm entries. Backup
         * consistency itself comes from Auto Backup shutting the app down before copying files.
         * [fileName] is overridable only for tests that need a real file (e.g. concurrency tests).
         */
        @JvmStatic
        @JvmOverloads
        fun build(context: Context, fileName: String = FILE_NAME): NotebookDatabase =
            Room.databaseBuilder(context.applicationContext, NotebookDatabase::class.java, fileName)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()

        @JvmStatic
        fun inMemory(context: Context): NotebookDatabase =
            Room.inMemoryDatabaseBuilder(context, NotebookDatabase::class.java).build()
    }
}
