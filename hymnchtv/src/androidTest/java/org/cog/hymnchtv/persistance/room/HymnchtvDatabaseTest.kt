package org.cog.hymnchtv.persistance.room

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class HymnchtvDatabaseTest {
    private val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val opened = mutableListOf<HymnchtvDatabase>()

    @After
    fun tearDown() {
        opened.forEach { it.close() }
        ctx.deleteDatabase(NAME)
    }

    private fun tableNames(db: HymnchtvDatabase): Set<String> {
        val names = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type = 'table'").use {
            while (it.moveToNext()) names += it.getString(0)
        }
        return names
    }

    private fun pragma(db: HymnchtvDatabase, sql: String): String =
        db.openHelper.writableDatabase.query(sql).use { it.moveToFirst(); it.getString(0) }

    @Test
    fun schemaV1CreatesTheThreeTables() {
        val db = HymnchtvDatabase.inMemory(ctx).also { opened += it }
        assertThat(tableNames(db)).containsAtLeast("media_record", "hymn_history", "english_lyrics")
        assertThat(db.openHelper.readableDatabase.version).isEqualTo(1)
    }

    @Test
    fun fileDatabaseUsesWriteAheadLogging() {
        ctx.deleteDatabase(NAME)
        val db = HymnchtvDatabase.build(ctx, NAME).also { opened += it }
        assertThat(pragma(db, "PRAGMA journal_mode").lowercase()).isEqualTo("wal")
    }

    @Test
    fun defaultFileNameIsTheUnifiedDatabase() {
        assertThat(HymnchtvDatabase.FILE_NAME).isEqualTo("hymnchtv.db")
    }

    @Test
    fun getInstanceReturnsOneProcessWideObject() {
        assertSame(HymnchtvDatabase.getInstance(ctx), HymnchtvDatabase.getInstance(ctx))
    }

    @Test
    fun getInstanceFromManyThreadsCreatesOnlyOneInstance() {
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val futures = (1..8).map {
                pool.submit<HymnchtvDatabase> {
                    start.await()
                    HymnchtvDatabase.getInstance(ctx)
                }
            }
            start.countDown()
            val instances = futures.map { it.get(30, TimeUnit.SECONDS) }
            assertThat(instances.toSet()).hasSize(1)
            assertSame(HymnchtvDatabase.getInstance(ctx), instances.first())
        } finally {
            pool.shutdownNow()
        }
    }

    private companion object {
        const val NAME = "test-room-schema.db"
    }
}
