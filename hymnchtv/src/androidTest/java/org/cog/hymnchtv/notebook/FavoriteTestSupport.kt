package org.cog.hymnchtv.notebook

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.repo.FavoriteRepository

/**
 * Shared helpers for instrumented favourites tests. Everything goes through the repository (no raw table access)
 * and must run off the main thread, i.e. on the instrumentation thread, never inside runOnMainSync.
 */
object FavoriteTestSupport {
    private fun repo(): FavoriteRepository {
        check(Looper.myLooper() != Looper.getMainLooper()) { "FavoriteTestSupport must not run on the main thread" }
        return Notebook.get(ApplicationProvider.getApplicationContext()).favorites
    }

    /** Un-favourites every active favourite. */
    fun reset() {
        val repo = repo()
        runBlocking { repo.findAll().forEach { repo.setFavorite(it.hymn, false) } }
    }

    fun add(vararg keys: HymnKey) {
        val repo = repo()
        runBlocking { keys.forEach { repo.setFavorite(it, true) } }
    }

    fun active(): List<FavoriteEntity> {
        val repo = repo()
        return runBlocking { repo.findAll() }
    }

    fun isFavorite(key: HymnKey): Boolean {
        val repo = repo()
        return runBlocking { repo.isFavorite(key) }
    }
}
