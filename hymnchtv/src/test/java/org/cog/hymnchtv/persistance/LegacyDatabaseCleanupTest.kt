package org.cog.hymnchtv.persistance

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LegacyDatabaseCleanupTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun touch(dir: File, name: String) = File(dir, name).apply { writeText("x") }

    @Test
    fun deletesLegacyFilesWithTheirWalShmAndJournal() {
        val dir = folder.newFolder("databases")
        val names = listOf(
            "dbHymnApp.db", "dbHymnApp.db-wal", "dbHymnApp.db-shm", "dbHymnApp.db-journal",
            "notebook.db", "notebook.db-wal", "notebook.db-shm",
        )
        names.forEach { touch(dir, it) }

        val removed = LegacyDatabaseCleanup.deleteLegacyFiles(dir)

        assertThat(removed).containsExactlyElementsIn(names)
        assertThat(dir.list()).isEmpty()
    }

    @Test
    fun neverTouchesTheCurrentDatabaseOrOtherFiles() {
        val dir = folder.newFolder("databases")
        val keep = listOf("hymnchtv.db", "hymnchtv.db-wal", "hymnchtv.db-shm", "other.db")
        keep.forEach { touch(dir, it) }
        touch(dir, "dbHymnApp.db")

        val removed = LegacyDatabaseCleanup.deleteLegacyFiles(dir)

        assertThat(removed).containsExactly("dbHymnApp.db")
        assertThat(dir.list()!!.toList()).containsExactlyElementsIn(keep)
    }

    @Test
    fun removesOrphanedWalWithoutMainFile() {
        val dir = folder.newFolder("databases")
        touch(dir, "notebook.db-wal")
        assertThat(LegacyDatabaseCleanup.deleteLegacyFiles(dir)).containsExactly("notebook.db-wal")
    }

    @Test
    fun nothingToDeleteIsANoOp() {
        val dir = folder.newFolder("databases")
        assertThat(LegacyDatabaseCleanup.deleteLegacyFiles(dir)).isEmpty()
        assertThat(LegacyDatabaseCleanup.deleteLegacyFiles(File(dir, "missing"))).isEmpty()
    }
}
