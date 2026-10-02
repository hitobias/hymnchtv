package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.About
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Plan F1: no hidden long-press features remain on the lyrics page, the player card or About. */
@RunWith(AndroidJUnit4::class)
class NoLongPressTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TestPermissions.grantLaunchPermission(ctx.packageName)
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
    }

    @After
    fun tearDown() {
        mainScenario?.close()
    }

    @Test
    fun lyricsPageAndPlayerCardHaveNoLongPressHandlers() {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_ER)
            putInt(MainActivity.ATTR_HYMN_NUMBER, 1)
        }
        ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras)).use { scenario ->
            val ids = intArrayOf(
                R.id.playback_play, R.id.btn_hymnSearch, R.id.btn_media, R.id.btn_jiaochang, R.id.btn_changshi,
                R.id.button_ts, R.id.button_english, R.id.button_mode,
            )
            val end = SystemClock.uptimeMillis() + 10_000
            var longClickable: List<String> = listOf("not checked")
            while (SystemClock.uptimeMillis() < end) {
                var found = false
                scenario.onActivity { activity ->
                    val views = ids.map { it to activity.findViewById<View>(it) }
                    found = views.all { it.second != null }
                    if (found) longClickable = views.filter { it.second.isLongClickable }.map { activity.resources.getResourceEntryName(it.first) }
                }
                if (found) break
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                SystemClock.sleep(50)
            }
            assertThat(longClickable).isEmpty()
        }
    }

    @Test
    fun aboutHistoryButtonIsNotLongClickable() {
        ActivityScenario.launch(About::class.java).use { scenario ->
            var longClickable = true
            scenario.onActivity { longClickable = it.findViewById<View>(R.id.history_log).isLongClickable }
            assertThat(longClickable).isFalse()
        }
    }
}
