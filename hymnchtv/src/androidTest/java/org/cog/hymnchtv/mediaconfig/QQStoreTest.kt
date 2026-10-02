package org.cog.hymnchtv.mediaconfig

import android.database.SQLException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
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

/** Fixed QQ JSON fixtures fed into a test database: counts, malformed input and rollback. */
@RunWith(AndroidJUnit4::class)
class QQStoreTest {
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
        JSONObject().put(QQRecord.QQ_TITLE, title).put(QQRecord.QQ_URL, url)

    private fun page(vararg items: Any) = JSONArray(items.toList())

    private fun rows(table: String) = db.mediaCount(table)

    @Test
    fun storesWellFormedLinksAndSkipsMalformedOnes() {
        val fixture = page(
            link("D1但愿荣耀归于圣父", "https://q.example/d1"),
            link("B755跟随榜样", "https://q.example/b755"),
            link("Q12青年诗歌", "https://q.example/q12"),            // Q is not a QQ hymn type
            link("D99999999999编号溢位", "https://q.example/huge"),  // NumberFormatException
            link("B没有编号", "https://q.example/none"),
            JSONObject().put(QQRecord.QQ_TITLE, "D2缺网址"),
            "not a JSON object",
        )

        val result = QQRecord.storeQQJArray(db, fixture)

        assertThat(result).isEqualTo(ImportResult(2, 2))
        assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(1L)
        assertThat(rows(MainActivity.HYMN_BB)).isEqualTo(1L)
    }

    @Test
    fun databaseErrorRollsBackTheWholePage() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        val fixture = page(
            link("D1但愿荣耀归于圣父", "https://q.example/d1"),
            link("B755跟随榜样", "https://q.example/b755"),
        )

        assertThrows(SQLException::class.java) { QQRecord.storeQQJArray(db, fixture) }
        assertThat(rows(MainActivity.HYMN_DB)).isEqualTo(0L)
    }

    private companion object {
        const val NAME = "test-qq-store.db"
    }
}
