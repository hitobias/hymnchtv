package org.cog.hymnchtv.notebook.repo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.NotebookDatabase
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.IdGenerator
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.repo.room.RoomSingLogRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSingLogConcurrencyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: NotebookDatabase
    private val key = HymnKey.of(HymnTypes.DB, 1)
    private val hour = 3_600_000L

    @Before
    fun setUp() {
        context.deleteDatabase(FILE)
        db = NotebookDatabase.build(context, FILE)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(FILE)
    }

    @Test
    fun concurrentAutoRecordsOfTheSameHymnKeepExactlyOne(): Unit = runBlocking {
        val repo = RoomSingLogRepository(db, TestClock(0), IdGenerator.RANDOM_UUID, testDevice)
        val results = (1..20).map { i ->
            async(Dispatchers.Default) {
                repo.recordUnlessDuplicate(key, 10 * hour + i, Occasion.HOME, SingSource.AUTO, 3 * hour)
            }
        }.awaitAll()
        assertThat(results.count { it != null }).isEqualTo(1)
        assertThat(db.singLogDao().findAllIncludingDeleted()).hasSize(1)
    }

    private companion object {
        const val FILE = "notebook-concurrency-test.db"
    }
}
