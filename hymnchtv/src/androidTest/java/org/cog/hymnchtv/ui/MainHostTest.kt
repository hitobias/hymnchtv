package org.cog.hymnchtv.ui

import android.content.Context
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.About
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.about.HelpActivity
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The tab host of MainActivity: tab switching, state across recreation, back handling, insets. */
@RunWith(AndroidJUnit4::class)
class MainHostTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
    }

    private fun launch(block: (ActivityScenario<MainActivity>) -> Unit) =
        ActivityScenario.launch(MainActivity::class.java).use(block)

    private fun selectTab(id: Int) = onView(withId(id)).perform(click())

    @Test
    fun startsOnTheHomeTab() = launch {
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun everyTabCanBeOpened() = launch {
        selectTab(R.id.nav_toc)
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_entry)).check(matches(not(isDisplayed())))
        selectTab(R.id.nav_settings)
        onView(withText(R.string.c_cat_appearance)).check(matches(isDisplayed()))
        selectTab(R.id.nav_home)
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }

    @Test
    fun myHymnsTabIsNotOfferedWhileTheNotebookUiIsOff() = launch { scenario ->
        scenario.onActivity { a ->
            val nav = a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav)
            val visible = (0 until nav.menu.size()).map { nav.menu.getItem(it) }.filter { it.isVisible }.map { it.itemId }
            assertThat(visible).containsExactly(R.id.nav_home, R.id.nav_toc, R.id.nav_settings).inOrder()
            assertThat(a.supportFragmentManager.fragments.none { it is org.cog.hymnchtv.ui.myhymns.MyHymnsFragment }).isTrue()
        }
    }

    @Test
    fun homeKeepsTheTypedNumberWhileAnotherTabIsShown() = launch {
        onView(withId(R.id.n1)).perform(scrollTo(), click())
        onView(withId(R.id.n2)).perform(scrollTo(), click())
        selectTab(R.id.nav_settings)
        selectTab(R.id.nav_home)
        onView(withId(R.id.tv_entry)).check(matches(withText(org.hamcrest.CoreMatchers.containsString(ctx.getString(R.string.c_label_no, 12)))))
    }

    @Test
    fun selectedTabSurvivesRecreation() = launch { scenario ->
        selectTab(R.id.nav_toc)
        scenario.recreate()
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_entry)).check(matches(not(isDisplayed())))
        scenario.onActivity { a ->
            assertThat(a.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_nav).selectedItemId)
                .isEqualTo(R.id.nav_toc)
        }
    }

    @Test
    fun backFromAnotherTabGoesHomeThenFinishes() = launch { scenario ->
        selectTab(R.id.nav_settings)
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { assertThat(it.isFinishing).isTrue() }
    }

    @Test
    fun backClosesTheHistoryPageBeforeFinishing() = launch { scenario ->
        val db = DatabaseBackend.getInstance(ctx)
        db.historyRecords.forEach { db.deleteHymnHistory(it) }
        db.storeHymnHistory(HistoryRecord(MainActivity.HYMN_DB, 1, false))
        FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(isDisplayed())) }
        onView(withId(R.id.btn_recent_more)).perform(click())
        onView(withId(R.id.history_list)).check(matches(isDisplayed()))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.history_list)).check(doesNotExist())
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { assertThat(it.isFinishing).isFalse() }
    }

    @Test
    fun openButtonIsShownOnHomeAndStartsDisabled() = launch {
        onView(withId(R.id.btn_open)).perform(scrollTo()).check(matches(isDisplayed())).check(matches(not(isEnabled())))
    }

    @Test
    fun toolbarSitsBelowTheStatusBarAndNavSitsAboveTheGestureArea() = launch { scenario ->
        scenario.onActivity { a ->
            val root = a.window.decorView
            val insets = androidx.core.view.ViewCompat.getRootWindowInsets(root)!!
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val toolbar = a.findViewById<View>(R.id.toolbar)
            val nav = a.findViewById<View>(R.id.bottom_nav)
            val loc = IntArray(2)
            toolbar.getLocationOnScreen(loc)
            assertThat(loc[1]).isAtLeast(insets.top)
            nav.getLocationOnScreen(loc)
            assertThat(loc[1] + nav.height).isAtMost(root.height - insets.bottom)
        }
    }

    private fun scrollSettingsTo(scenario: ActivityScenario<MainActivity>, key: String) {
        scenario.onActivity { a ->
            val settings = a.supportFragmentManager.fragments.filterIsInstance<SettingsFragment>().single()
            settings.scrollToPreference(key)
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun settingsTabStillReachesHelpAboutAndUpdateCheck() = launch { scenario ->
        selectTab(R.id.nav_settings)
        mapOf("online_help" to R.string.help_online, "about" to R.string.about, "check_update" to R.string.c_check_update)
            .forEach { (key, title) ->
                scrollSettingsTo(scenario, key)
                onView(withText(title)).check(matches(isDisplayed()))
            }
    }

    @Test
    fun helpEntryOpensTheBuiltInHelpPage() = launch { scenario ->
        selectTab(R.id.nav_settings)
        val monitor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .addMonitor(HelpActivity::class.java.name, null, false)
        scrollSettingsTo(scenario, "online_help")
        onView(withText(R.string.help_online)).perform(click())
        val opened = monitor.waitForActivityWithTimeout(5000)
        assertThat(opened).isNotNull()
        opened?.finish()
    }

    @Test
    fun aboutEntryOpensAboutWithoutCrashing() = launch { scenario ->
        selectTab(R.id.nav_settings)
        val monitor = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .addMonitor(About::class.java.name, null, false)
        scrollSettingsTo(scenario, "about")
        onView(withText(R.string.about)).perform(click())
        val opened = monitor.waitForActivityWithTimeout(5000)
        assertThat(opened).isNotNull()
        assertThat(opened!!.isFinishing).isFalse()
        opened.finish()
    }
}
