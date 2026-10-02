package org.cog.hymnchtv.persistance

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** All data access goes through Room: no hand-rolled SQLite initialisation may creep back into the app sources. */
class NoNativeSqliteGuardTest {
    private val srcMain = checkNotNull(System.getProperty("hymnchtv.assetsDir")).let { File(it).parentFile!! }

    private val forbidden = listOf(
        "SQLiteOpenHelper", "getWritableDatabase", "getReadableDatabase", "execSQL(\"PRAGMA", "android.database.sqlite.SQLiteDatabase",
    )

    @Test
    fun mainSourcesHaveNoNativeSqliteInitialisation() {
        val hits = File(srcMain, "java").walkTopDown()
            .filter { it.isFile && it.extension in setOf("java", "kt") }
            .flatMap { file ->
                file.readLines().asSequence().mapIndexedNotNull { i, line ->
                    forbidden.firstOrNull { it in line }?.let { "${file.name}:${i + 1}: $it" }
                }
            }.toList()
        assertThat(hits).isEmpty()
    }
}
