package org.cog.hymnchtv.mediaconfig

import android.database.SQLException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.persistance.mediaCount
import org.cog.hymnchtv.persistance.failInsertsFor
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UrlImportTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: DatabaseBackend

    @Before
    fun setUp() {
        ctx.deleteDatabase(NAME)
        db = DatabaseBackend.createForTest(ctx, NAME)
    }

    @After
    fun tearDown() {
        db.close()
        ctx.deleteDatabase(NAME)
    }

    private fun rows() = db.mediaCount(MainActivity.HYMN_DB)

    private fun uriOf(no: Int, type: MediaType): String? {
        val record = MediaRecord(MainActivity.HYMN_DB, no, false, type)
        return if (db.getMediaRecord(record, true)) record.mediaUri else null
    }

    @Test
    fun importsWellFormedLinesAndSkipsMalformedOnes() {
        val content = listOf(
            "hymn_db,1,0,HYMN_MEDIA,https://example.org/1,null",
            "hymn_db,abc,0,HYMN_MEDIA,https://example.org/bad-number,null",
            "hymn_db,3,0,NOT_A_TYPE,https://example.org/bad-type,null",
            "",
            "hymn_db,2,0,HYMN_JIAOCHANG,https://example.org/2,null",
        ).joinToString("\r\n")

        val result = MediaConfig.importUrlRecords(db, content, false)

        assertThat(result).isEqualTo(ImportResult(2, 2))
        assertThat(rows()).isEqualTo(2L)
    }

    @Test
    fun keepsExistingRecordsUnlessOverwrite() {
        db.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, 1, false, MediaType.HYMN_MEDIA, "https://old", null))
        val line = "hymn_db,1,0,HYMN_MEDIA,https://new,null"

        assertThat(MediaConfig.importUrlRecords(db, line, false)).isEqualTo(ImportResult(0, 1))
        assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://old")

        assertThat(MediaConfig.importUrlRecords(db, line, true)).isEqualTo(ImportResult(1, 1))
        assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://new")
    }

    /** The existing-key lookup is prefetched once, so a repeated line of the same file must still see the first one. */
    @Test
    fun repeatedLineInOneFileKeepsTheFirstUnlessOverwrite() {
        val content = "hymn_db,1,0,HYMN_MEDIA,https://first,null\nhymn_db,1,0,HYMN_MEDIA,https://second,null"

        assertThat(MediaConfig.importUrlRecords(db, content, false)).isEqualTo(ImportResult(1, 2))
        assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://first")

        assertThat(MediaConfig.importUrlRecords(db, content, true)).isEqualTo(ImportResult(2, 2))
        assertThat(uriOf(1, MediaType.HYMN_MEDIA)).isEqualTo("https://second")
    }

    /** A database error part-way through rolls back every earlier write of the same import. */
    @Test
    fun databaseErrorRollsBackTheWholeImport() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        val content = "hymn_db,1,0,HYMN_MEDIA,https://example.org/1,null\nhymn_bb,1,0,HYMN_MEDIA,https://example.org/bb1,null"

        assertThrows(SQLException::class.java) { MediaConfig.importUrlRecords(db, content, false) }
        assertThat(rows()).isEqualTo(0L)
    }

    private companion object {
        const val NAME = "test-url-import.db"
    }
}
