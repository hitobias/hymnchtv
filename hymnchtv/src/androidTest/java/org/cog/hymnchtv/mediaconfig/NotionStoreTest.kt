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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Fixed Notion JSON fixtures fed into a test database: counts, malformed input, overwrite and rollback. */
@RunWith(AndroidJUnit4::class)
class NotionStoreTest {
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

    private fun link(title: String, url: String): JSONObject =
        JSONObject().put(NotionRecord.NQ_TITLE, title).put(NotionRecord.NQ_URL, url)

    private fun page(vararg items: Any) = JSONArray(items.toList())

    private fun rows(table: String) = db.mediaCount(table)

    private fun uriOf(table: String, no: Int): String? {
        val record = MediaRecord(table, no, false, MediaType.HYMN_JIAOCHANG)
        return if (db.getMediaRecord(record, true)) record.mediaUri else null
    }

    @Test
    fun storesWellFormedLinksAndSkipsMalformedOnes() {
        val fixture = page(
            link("D1但愿荣耀归于圣父", "https://n.example/d1"),
            link("B23羔羊是配", "https://n.example/b23"),
            link("C006朵朵小花含笑", "https://n.example/c6"),
            link("Z9未知诗歌本", "https://n.example/z9"),       // unknown hymn type prefix
            link("D无编号", "https://n.example/none"),           // no hymn number
            link("", "https://n.example/empty"),                // empty title
            JSONObject().put(NotionRecord.NQ_TITLE, "D5缺网址"), // missing url
            "not a JSON object",
        )

        val result = NotionRecord.storeNQJArray(db, fixture, false)

        assertThat(result).isEqualTo(ImportResult(3, 3))
        assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(1L)
        assertThat(rows(MainActivity.HYMN_BB)).isEqualTo(1L)
        assertThat(rows(MainActivity.HYMN_ER)).isEqualTo(1L)
        assertThat(uriOf(MainActivity.HYMN_ER, 6)).isEqualTo("https://n.example/c6")
    }

    @Test
    fun keepsExistingLinksUnlessOverwrite() {
        db.storeMediaRecord(NotionRecord(MainActivity.HYMN_DB, 1).apply { setMediaUri("https://old") })
        val fixture = page(link("D1但愿荣耀归于圣父", "https://new"))

        assertThat(NotionRecord.storeNQJArray(db, fixture, false)).isEqualTo(ImportResult(0, 1))
        assertThat(uriOf(MainActivity.HYMN_DB, 1)).isEqualTo("https://old")

        assertThat(NotionRecord.storeNQJArray(db, fixture, true)).isEqualTo(ImportResult(1, 1))
        assertThat(uriOf(MainActivity.HYMN_DB, 1)).isEqualTo("https://new")
    }

    /** A DB error on the second link must roll back the first one and propagate (no silent partial commit). */
    @Test
    fun databaseErrorRollsBackTheWholePage() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        val fixture = page(
            link("D1但愿荣耀归于圣父", "https://n.example/d1"),
            link("B23羔羊是配", "https://n.example/b23"),
        )

        assertThrows(SQLException::class.java) { NotionRecord.storeNQJArray(db, fixture, false) }
        assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(0L)
    }

    private companion object {
        const val NAME = "test-notion-store.db"
    }
}
