package org.cog.hymnchtv.persistance.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    entities = [MediaRecordEntity::class, HymnHistoryEntity::class, EnglishLyricsEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HymnchtvDatabase : RoomDatabase() {
    abstract fun mediaRecordDao(): MediaRecordDao
    abstract fun hymnHistoryDao(): HymnHistoryDao
    abstract fun englishLyricsDao(): EnglishLyricsDao

    companion object {
        const val FILE_NAME = "hymnchtv.db"

        @Volatile
        private var instance: HymnchtvDatabase? = null

        /**
         * The process-wide database. Main-thread queries stay allowed as a transition (plan section 2.4): the
         * listed call sites must move to AppExecutors.io before 1.0.
         */
        @JvmStatic
        fun getInstance(context: Context): HymnchtvDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context, FILE_NAME).also { instance = it }
            }

        /** A file-backed database. Write-ahead logging is set explicitly (Room's default may pick another mode). */
        @JvmStatic
        @JvmOverloads
        fun build(context: Context, fileName: String = FILE_NAME): HymnchtvDatabase =
            Room.databaseBuilder(context.applicationContext, HymnchtvDatabase::class.java, fileName)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .allowMainThreadQueries()
                .build()

        /** A throw-away in-memory database for tests. */
        @JvmStatic
        fun inMemory(context: Context): HymnchtvDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, HymnchtvDatabase::class.java).build()
    }
}
