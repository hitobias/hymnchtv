package org.cog.hymnchtv.toc

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YbCrossRefAssetTest {
    @Test
    fun mainActivityTableLoadsFromAssetsWithoutMainActivityOnCreate() {
        assertThat(MainActivity.ybXTable[1]).isEqualTo("bb876")
        assertThat(MainActivity.ybXTable).hasSize(113)
    }

    /**
     * Worst case of the lazy design: the first use happens on the main thread before the warm-up ran.
     * Times the exact load path (asset open + parse) on the main thread. Logged to tag "YbPerf" for the
     * measurements doc; the bound is only a sanity guard against a pathological regression.
     */
    @Test
    fun mainThreadFirstUseCost() {
        val samples = mutableListOf<Long>()
        repeat(6) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val start = SystemClock.elapsedRealtime()
                YbCrossRef.loadFromAssets()
                samples += SystemClock.elapsedRealtime() - start
            }
        }
        val first = samples.first()
        val median = samples.drop(1).sorted()[2]
        Log.i("YbPerf", "main_thread_first_ms=$first median_ms=$median samples=${samples.joinToString(",")}")
        assertThat(first).isLessThan(100L)
    }
}
