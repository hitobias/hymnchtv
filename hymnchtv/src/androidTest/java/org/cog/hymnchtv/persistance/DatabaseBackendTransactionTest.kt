package org.cog.hymnchtv.persistance

import android.database.SQLException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.mediaconfig.MediaRecord
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseBackendTransactionTest {
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

    private fun record(no: Int) =
        MediaRecord(MainActivity.HYMN_DB, no, false, MediaType.HYMN_MEDIA, "https://example.org/$no", null)

    private fun rows() = db.mediaCount(MainActivity.HYMN_DB)

    @Test
    fun commitsEveryWriteOfTheBody() {
        db.runInTransaction {
            db.storeMediaRecord(record(1))
            db.storeMediaRecord(record(2))
        }
        assertThat(rows()).isEqualTo(2L)
    }

    @Test
    fun rollsBackWhenTheBodyThrows() {
        val error = assertThrows(IllegalStateException::class.java) {
            db.runInTransaction {
                db.storeMediaRecord(record(1))
                throw IllegalStateException("boom")
            }
        }
        assertThat(error).hasMessageThat().isEqualTo("boom")
        assertThat(rows()).isEqualTo(0L)
    }

    @Test
    fun inTransactionReturnsTheBodyResult() {
        assertThat(db.inTransaction { 42 }).isEqualTo(42)
    }

    @Test
    fun testDatabaseIsSeparateFromTheAppDatabase() {
        assertThat(db.roomDatabase().openHelper.databaseName).isEqualTo(NAME)
    }

    /** Nested success: the inner transaction joins the outer one and both writes commit together. */
    @Test
    fun nestedSuccessCommitsBothWrites() {
        db.runInTransaction {
            db.storeMediaRecord(record(1))
            db.runInTransaction { db.storeMediaRecord(record(2)) }
        }
        assertThat(rows()).isEqualTo(2L)
    }

    /**
     * Android SQLiteDatabase semantics: when a nested transaction ends without being marked successful,
     * the whole outer transaction rolls back, even if the outer body catches the error and returns normally.
     */
    @Test
    fun innerFailureRollsBackTheWholeOuterTransaction() {
        db.runInTransaction {
            db.storeMediaRecord(record(1))
            try {
                db.runInTransaction {
                    db.storeMediaRecord(record(2))
                    throw IllegalStateException("inner")
                }
            } catch (expected: IllegalStateException) {
                // swallowed on purpose: the outer body still "succeeds"
            }
        }
        assertThat(rows()).isEqualTo(0L)
    }

    /** storeMediaRecord() swallows SQL errors (returns -1); the OrThrow variant must surface them. */
    @Test
    fun storeMediaRecordOrThrowSurfacesDatabaseErrors() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        val bb = MediaRecord(MainActivity.HYMN_BB, 1, false, MediaType.HYMN_MEDIA, "https://example.org/bb", null)
        assertThrows(SQLException::class.java) { db.storeMediaRecordOrThrow(bb) }
        assertThat(db.storeMediaRecord(bb)).isEqualTo(-1L)
    }

    private companion object {
        const val NAME = "test-transaction.db"
    }
}
