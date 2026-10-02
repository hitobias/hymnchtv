package org.cog.hymnchtv.persistance

import android.content.Context
import timber.log.Timber
import java.io.File

/**
 * Install policy (plan section 2.5): 1.0 is the first release, so no installed user has data in an earlier
 * database layout and nothing is migrated. Files of the superseded databases are only developer leftovers and
 * are deleted at startup. The current database file (HymnchtvDatabase.FILE_NAME) is never in this list.
 */
object LegacyDatabaseCleanup {
    val LEGACY_DATABASE_NAMES = listOf("dbHymnApp.db", "notebook.db")
    private val SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    /** Deletes the legacy database files; returns the names of the files that were actually removed. */
    @JvmStatic
    fun deleteLegacyFiles(context: Context): List<String> =
        deleteLegacyFiles(checkNotNull(context.getDatabasePath(LEGACY_DATABASE_NAMES.first()).parentFile))

    internal fun deleteLegacyFiles(databaseDir: File): List<String> {
        val removed = mutableListOf<String>()
        for (name in LEGACY_DATABASE_NAMES) {
            for (suffix in SUFFIXES) {
                val file = File(databaseDir, name + suffix)
                if (!file.exists()) continue
                if (file.delete()) {
                    removed += file.name
                } else {
                    Timber.w("Could not delete legacy database file %s", file.absolutePath)
                }
            }
        }
        if (removed.isNotEmpty()) Timber.i("Deleted legacy database files: %s", removed)
        return removed
    }
}
