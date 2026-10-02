package org.cog.hymnchtv.ui.search

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.search.AssetLyricsSource
import org.cog.hymnchtv.search.HymnSearch
import org.cog.hymnchtv.search.SearchScope
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A search for a string nobody sings reads every lyrics file: the worst case. The automated bound only catches a
 * collapse (30 s); the release gate (api24b 10 s, api34b 5 s, median) is checked from the logcat tag "SearchPerf".
 */
@RunWith(AndroidJUnit4::class)
class SearchPerfTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    private fun usedHeap(): Long {
        System.gc()
        System.gc()
        val rt = Runtime.getRuntime()
        return rt.totalMemory() - rt.freeMemory()
    }

    @Test fun worstCaseSearchOverAllBooksStaysWithinBoundsAndMemory() {
        val source = AssetLyricsSource.create(ctx, null)
        val t2s = AssetLyricsSource.loadT2s(ctx)
        val search = HymnSearch(source, t2s)
        val before = usedHeap()
        val times = (1..3).map {
            val start = SystemClock.elapsedRealtime()
            val page = search.search("zzzqqq不存在", SearchScope.All)
            assertThat(page.results).isEmpty()
            SystemClock.elapsedRealtime() - start
        }
        val median = times.sorted()[1]
        Log.i("SearchPerf", "all-books miss: runs=$times ms, median=$median ms")
        val after = usedHeap()
        Log.i("SearchPerf", "heap growth=${(after - before) / 1024} KiB")
        assertThat(median).isLessThan(30_000L)
        assertThat(after - before).isLessThan(16L * 1024 * 1024)
    }
}
