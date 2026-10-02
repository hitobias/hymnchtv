package org.cog.hymnchtv.notebook.repo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.NotebookDatabase
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.repo.room.RoomNoteRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomNoteRepositoryTest {
    private lateinit var db: NotebookDatabase
    private lateinit var repo: RoomNoteRepository
    private val clock = TestClock(1_000)
    private val key = HymnKey.of(HymnTypes.DB, 1)

    @Before
    fun setUp() {
        db = NotebookDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repo = RoomNoteRepository(db, clock, TestIds(), testDevice)
    }

    @After
    fun tearDown() = db.close()

    private fun rejects(block: suspend () -> Unit) = runBlocking {
        assertThat(runCatching { block() }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun addValidatesAndListsNewestFirst(): Unit = runBlocking {
        repo.add(key, "第一則")
        clock.now = 2_000
        val second = repo.add(key, "第二則", singLogId = androidTestUuid(9))
        assertThat(second.singLogId).isEqualTo(androidTestUuid(9))
        assertThat(repo.findByHymn(key).map { it.body }).containsExactly("第二則", "第一則").inOrder()
        rejects { repo.add(key, "  ") }
        rejects { repo.add(key, "ok", singLogId = "not-a-uuid") }
    }

    @Test
    fun updateValidatesAndDeleteIsSoft(): Unit = runBlocking {
        val note = repo.add(key, "原文")
        clock.now = 2_000
        val updated = repo.update(note.copy(body = "改過"))
        assertThat(updated?.body).isEqualTo("改過")
        assertThat(updated?.createdAt).isEqualTo(1_000L)
        assertThat(updated?.updatedAt).isEqualTo(2_000L)
        rejects { repo.update(note.copy(body = "")) }
        assertThat(repo.delete(note.id)).isTrue()
        assertThat(repo.findByHymn(key)).isEmpty()
    }
}
