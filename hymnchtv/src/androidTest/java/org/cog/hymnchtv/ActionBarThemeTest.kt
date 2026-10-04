package org.cog.hymnchtv

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.about.HelpActivity
import org.cog.hymnchtv.about.LicensesActivity
import org.cog.hymnchtv.mediaconfig.MediaConfig
import org.cog.hymnchtv.reading.BackgroundPickerActivity
import org.cog.hymnchtv.reading.ReadingSettingsActivity
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Secondary screens show their title in the window ActionBar; MainActivity (own Toolbar) and ContentHandler have none.
 * Reading settings themes its page with an in-page [org.cog.hymnchtv.ui.page.PageTitleBar] (1.2.0) instead.
 */
@RunWith(AndroidJUnit4::class)
class ActionBarThemeTest {
    @Before
    fun setUp() = FragmentHost.grantLaunchPermissions(getInstrumentation().targetContext.packageName)

    private fun <T : AppCompatActivity> launch(cls: Class<T>, block: (T) -> Unit) {
        val intent = Intent(ApplicationProvider.getApplicationContext(), cls).putExtra(MainActivity.ATTR_SEARCH, "主")
        ActivityScenario.launch<T>(intent).use { scenario ->
            getInstrumentation().waitForIdleSync()
            scenario.onActivity(block)
        }
    }

    private fun assertHasVisibleActionBar(cls: Class<out AppCompatActivity>) {
        launch(cls) { a ->
            assertThat(a.supportActionBar).isNotNull()
            assertThat(a.supportActionBar!!.isShowing).isTrue()
        }
    }

    @Test fun mediaConfigHasActionBar() = assertHasVisibleActionBar(MediaConfig::class.java)

    @Test fun helpHasActionBar() = assertHasVisibleActionBar(HelpActivity::class.java)

    @Test fun licensesHasActionBar() = assertHasVisibleActionBar(LicensesActivity::class.java)

    @Test
    fun readingSettingsHasAVisiblePageTitleBarWithBack() {
        launch(ReadingSettingsActivity::class.java) { a ->
            val back = a.findViewById<android.view.View>(R.id.page_back)
            val title = a.findViewById<android.widget.TextView>(R.id.page_title)
            assertThat(back).isNotNull()
            assertThat(back.isShown).isTrue()
            assertThat(title.isShown).isTrue()
            assertThat(title.text.toString()).isNotEmpty()
        }
    }

    @Test fun backgroundPickerHasActionBar() = assertHasVisibleActionBar(BackgroundPickerActivity::class.java)

    @Test
    fun mainActivityUsesItsOwnToolbarNotAWindowActionBar() {
        launch(MainActivity::class.java) { a ->
            val windowActionBar = a.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.windowActionBar))
            try {
                assertThat(windowActionBar.getBoolean(0, true)).isFalse()
            } finally {
                windowActionBar.recycle()
            }
            assertThat(a.supportActionBar).isNotNull()
        }
    }

    @Test
    fun contentHandlerHasNoWindowActionBar() {
        val themeRes = getInstrumentation().targetContext.packageManager
            .getActivityInfo(
                android.content.ComponentName(getInstrumentation().targetContext, ContentHandler::class.java), 0
            ).theme
        val ctx = android.view.ContextThemeWrapper(getInstrumentation().targetContext, themeRes)
        val a = ctx.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.windowActionBar))
        try {
            assertThat(a.getBoolean(0, true)).isFalse()
        } finally {
            a.recycle()
        }
    }
}
