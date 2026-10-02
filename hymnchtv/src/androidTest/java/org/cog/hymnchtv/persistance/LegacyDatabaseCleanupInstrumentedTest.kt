package org.cog.hymnchtv.persistance

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Install policy: leftovers of the old database layouts are deleted, the new file is created on use. */
@RunWith(AndroidJUnit4::class)
class LegacyDatabaseCleanupInstrumentedTest {
    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val legacyFiles = LegacyDatabaseCleanup.LEGACY_DATABASE_NAMES.flatMap { listOf(it, "$it-wal", "$it-shm") }
    private lateinit var db: HymnchtvDatabase

    private fun dbFile(name: String) = File(ctx.getDatabasePath(name).path)

    @Before
    fun setUp() {
        dbFile(NAME).parentFile?.mkdirs()
        legacyFiles.forEach { dbFile(it).writeText("legacy") }
        ctx.deleteDatabase(NAME)
    }

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
        ctx.deleteDatabase(NAME)
        legacyFiles.forEach { dbFile(it).delete() }
    }

    @Test
    fun legacyFilesAreDeletedAndTheNewDatabaseIsCreated() {
        assertThat(legacyFiles.all { dbFile(it).exists() }).isTrue()

        val removed = LegacyDatabaseCleanup.deleteLegacyFiles(ctx)
        db = HymnchtvDatabase.build(ctx, NAME)
        db.openHelper.writableDatabase // force creation

        assertThat(removed).containsExactlyElementsIn(legacyFiles)
        assertThat(legacyFiles.none { dbFile(it).exists() }).isTrue()
        assertThat(dbFile(NAME).exists()).isTrue()
    }

    private companion object {
        const val NAME = "test-legacy-cleanup.db"
    }
}
