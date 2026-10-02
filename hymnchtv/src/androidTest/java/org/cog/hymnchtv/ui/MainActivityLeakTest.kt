package org.cog.hymnchtv.ui

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.ref.WeakReference

/**
 * A recreated (e.g. rotated) MainActivity must be released: each one holds the whole tab host (fragments, views,
 * backgrounds), so on a 48 MB heap (API 24 phones) a handful of rotations ran out of memory.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLeakTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
    }

    @Test
    fun recreatingDoesNotAddProcessLifecycleObservers() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val before = observerCount()
            repeat(RECREATIONS) { scenario.recreate() }
            assertThat(observerCount()).isEqualTo(before)
        }
    }

    @Test
    fun recreatedActivityCanBeGarbageCollected() {
        val replaced = recreateAndKeepWeakRefsToReplacedActivities()
        assertThat(awaitCollected(replaced)).isEqualTo(0)
    }

    /**
     * Done in its own frame, which has returned before the GC: a debuggable runtime may keep a dead register of the
     * test thread pointing at an activity, which would look like a leak.
     */
    private fun recreateAndKeepWeakRefsToReplacedActivities(): List<WeakReference<MainActivity>> =
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            List(RECREATIONS) {
                var current: WeakReference<MainActivity>? = null
                scenario.onActivity { current = WeakReference(it) }
                scenario.recreate()
                current!!
            }
        }

    private fun observerCount(): Int {
        var count = 0
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            count = (ProcessLifecycleOwner.get().lifecycle as androidx.lifecycle.LifecycleRegistry).observerCount
        }
        return count
    }

    /** @return how many of [refs] are still reachable after repeated GCs */
    private fun awaitCollected(refs: List<WeakReference<MainActivity>>): Int {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        repeat(GC_ATTEMPTS) {
            instrumentation.waitForIdleSync()
            Runtime.getRuntime().gc()
            System.runFinalization()
            if (refs.none { it.get() != null }) return 0
            Thread.sleep(GC_PAUSE_MS)
        }
        return refs.count { it.get() != null }
    }

    private companion object {
        const val RECREATIONS = 3
        const val GC_ATTEMPTS = 20
        const val GC_PAUSE_MS = 250L
    }
}
