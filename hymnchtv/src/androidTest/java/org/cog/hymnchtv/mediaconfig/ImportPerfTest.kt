package org.cog.hymnchtv.mediaconfig

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.utils.HymnNoValidate
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures importing the bundled url_import.txt (2,696 lines) into an empty test database.
 * Results go to logcat tag "ImportPerf"; tools/perf/import_perf.sh collects them.
 */
@RunWith(AndroidJUnit4::class)
class ImportPerfTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private val content: String by lazy {
        ctx.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun freshDb(name: String): DatabaseBackend {
        ctx.deleteDatabase(name)
        return DatabaseBackend.createForTest(ctx, name)
    }

    private fun dispose(db: DatabaseBackend, name: String) {
        db.close()
        ctx.deleteDatabase(name)
    }

    /** The pre-B-9a loop: one auto-commit per record, isOverWrite = false. */
    private fun legacyImport(db: DatabaseBackend, text: String): Int {
        var imported = 0
        for (line in text.split(Regex("\r\n|\n"))) {
            val record = try {
                MediaRecord.toRecord(line)
            } catch (e: IllegalArgumentException) {
                null
            } ?: continue
            val hymnNo = if (record.isFu()) record.hymnNo - HymnNoValidate.HYMN_DB_NO_MAX else record.hymnNo
            val nui = HymnNoValidate.validateHymnNo(record.hymnType, hymnNo, record.isFu())
            if (nui != -1 && !db.getMediaRecord(record, false) && db.storeMediaRecord(record) != -1L) {
                imported++
            }
        }
        return imported
    }

    private fun timeMs(block: () -> Unit): Long {
        val start = SystemClock.elapsedRealtime()
        block()
        return SystemClock.elapsedRealtime() - start
    }

    private fun report(label: String, samples: List<Long>, imported: Int) {
        val sorted = samples.sorted()
        Log.i(TAG, "$label median_ms=${sorted[sorted.size / 2]} samples=${sorted.joinToString(",")} imported=$imported")
    }

    private fun measure(label: String, importOnce: (DatabaseBackend) -> Int) {
        var imported = 0
        val samples = (0..RUNS).map { run ->
            val name = "perf-$label-$run.db"
            val db = freshDb(name)
            try {
                timeMs { imported = importOnce(db) }
            } finally {
                dispose(db, name)
            }
        }.drop(1) // run 0 is the warm-up
        assertThat(imported).isGreaterThan(0)
        report(label, samples, imported)
    }

    @Test
    fun legacyPerRecordCommit() {
        measure("legacy") { db -> legacyImport(db, content) }
    }

    @Test
    fun batchSingleTransaction() {
        val checkName = "perf-check-legacy.db"
        val checkDb = freshDb(checkName)
        val expected = try {
            legacyImport(checkDb, content)
        } finally {
            dispose(checkDb, checkName)
        }

        measure("batch") { db ->
            val result = MediaConfig.importUrlRecords(db, content, false)
            assertThat(result.imported).isEqualTo(expected)
            result.imported
        }
    }

    private companion object {
        const val TAG = "ImportPerf"
        const val ASSET = "url_import.txt"
        const val RUNS = 5
    }
}
