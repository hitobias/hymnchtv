package org.cog.hymnchtv.ui.motion

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Spec 5b-4: the hymn number is a shared element only when opened from home and animations are on; the page never stays hidden. */
@RunWith(AndroidJUnit4::class)
class TransitionsTest : LyricsTestBase() {
    private fun launchShared(): ActivityScenario<ContentHandler> {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
            putInt(MainActivity.ATTR_HYMN_NUMBER, 5)
            putBoolean(Motion.EXTRA_SHARED_NUMBER, true)
        }
        return ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras)).also { it.awaitPage() }
    }

    private fun anchorName(s: ActivityScenario<ContentHandler>): String? =
        s.read { page(it)!!.findViewById<View>(R.id.lyrics_number_anchor).transitionName }

    private var originalScale: String = "1"

    private fun shell(command: String): String {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { String(it.readBytes()) }.trim()
    }

    private fun setAnimatorScale(value: String) {
        shell("settings put global animator_duration_scale $value")
        SystemClock.sleep(300)
    }

    /** Test devices usually run with animations off (scale 0), which disables the shared element: switch them on. */
    @Before
    fun animationsOn() {
        originalScale = shell("settings get global animator_duration_scale").takeUnless { it.isEmpty() || it == "null" } ?: "1"
        setAnimatorScale("1")
    }

    @After
    fun restoreAnimations() = setAnimatorScale(originalScale)

    @Test
    fun headerNumberHasTheHomePreviewTransitionName() {
        var homeName: String? = null
        ActivityScenario.launch(MainActivity::class.java).use { home ->
            home.onActivity { homeName = it.findViewById<View>(R.id.tv_entry).transitionName }
        }
        assertThat(homeName).isEqualTo(Motion.SHARED_NUMBER)
        launchShared().use { s ->
            s.await("anchor named") { page(it)!!.findViewById<View>(R.id.lyrics_number_anchor).transitionName == Motion.SHARED_NUMBER }
            val anchor = s.pageView(R.id.lyrics_number_anchor)
            assertThat(anchor.width).isGreaterThan(1)
            assertThat(anchor.height).isGreaterThan(1)
        }
    }

    @Test
    fun noSharedElementWhenAnimationsAreRemoved() {
        setAnimatorScale("0")
        try {
            assertThat(Motion.enabled(ctx)).isFalse()
            launchShared().use { s ->
                SystemClock.sleep(500)
                assertThat(anchorName(s)).isNull()
                assertThat(s.read { it.window.sharedElementEnterTransition }).isNull()
                assertThat(s.read { it.window.enterTransition }).isNull()
            }
        } finally {
            setAnimatorScale("1")
        }
        assertThat(Motion.enabled(ctx)).isTrue()
    }

    @Test
    fun postponedEnterStartsAfterTheTimeoutAndShowsThePage() {
        launchShared().use { s ->
            val begun = AtomicInteger()
            lateinit var starter: SharedNumberStarter
            instrumentation.runOnMainSync {
                s.onActivity { starter = SharedNumberStarter(it.window.decorView, { false }, { begun.incrementAndGet() }, 300) }
            }
            SystemClock.sleep(150)
            assertThat(begun.get()).isEqualTo(0)
            SystemClock.sleep(500)
            assertThat(begun.get()).isEqualTo(1)
            instrumentation.runOnMainSync { starter.cancel() }
            assertThat(begun.get()).isEqualTo(1)
            assertThat(s.read { page(it)!!.findViewById<View>(R.id.lyrics_scroll).isShown }).isTrue()
        }
    }

    @Test
    fun readyTargetStartsAtOnce() {
        launchShared().use { s ->
            val begun = AtomicInteger()
            instrumentation.runOnMainSync {
                s.onActivity { SharedNumberStarter(it.window.decorView, { true }, { begun.incrementAndGet() }, 5_000) }
            }
            SystemClock.sleep(400)
            assertThat(begun.get()).isEqualTo(1)
        }
    }
}
