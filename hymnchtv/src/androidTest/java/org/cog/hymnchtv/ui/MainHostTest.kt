package org.cog.hymnchtv.ui

import org.cog.hymnchtv.QuickTest
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
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
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
import org.cog.hymnchtv.ui.host.BackExitGuard
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The host of MainActivity: the home page with its top-bar entries, full pages over it, state across recreation, back handling, insets. */
@RunWith(AndroidJUnit4::class)
class MainHostTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
    }

    private fun launch(block: (ActivityScenario<MainActivity>) -> Unit) =
        ActivityScenario.launch(MainActivity::class.java).use(block)

    private fun openToc() = onView(withId(R.id.btn_home_toc)).perform(click())

    private fun openSettings() = onView(withId(R.id.btn_home_settings)).perform(click())

    @Test
    @QuickTest
    fun startsOnTheHomePageWithTheTopBarEntries() = launch { scenario ->
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_home_toc)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_home_settings)).check(matches(isDisplayed()))
        scenario.onActivity { a ->
            val dm = a.resources.displayMetrics
            for (id in intArrayOf(R.id.btn_home_toc, R.id.btn_home_settings)) {
                val v = a.findViewById<View>(id)
                assertThat(v.width).isAtLeast((48 * dm.density).toInt())
                assertThat(v.height).isAtLeast((48 * dm.density).toInt())
            }
            assertThat(a.findViewById<View>(R.id.btn_home_toc).contentDescription).isEqualTo(a.getString(R.string.c_nav_toc))
            assertThat(a.findViewById<View>(R.id.btn_home_settings).contentDescription).isEqualTo(a.getString(R.string.c_nav_settings))
        }
    }

    @Test
    @QuickTest
    fun contentsAndSettingsOpenFullPageAndBackReturnsHome() = launch { scenario ->
        openToc()
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_entry)).check(doesNotExist())
        scenario.onActivity { assertThat(it.supportFragmentManager.backStackEntryCount).isEqualTo(1) }
        pressBack()
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        openSettings()
        onView(withText(R.string.c_cat_appearance)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_entry)).check(doesNotExist())
        pressBack()
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
    }

    @Test
    fun fullPagesHideTheTopBarAndBringTheirOwnTitleBar() = launch { scenario ->
        openSettings()
        onView(withId(R.id.page_back)).check(matches(isDisplayed()))
        scenario.onActivity { a ->
            assertThat(a.findViewById<View>(R.id.toolbar).visibility).isEqualTo(View.GONE)
        }
        // The page's arrow does what the back key does
        onView(withId(R.id.page_back)).perform(click())
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { a ->
            assertThat(a.findViewById<View>(R.id.toolbar).visibility).isEqualTo(View.VISIBLE)
            assertThat(a.supportActionBar?.title?.toString()).isEqualTo(a.getString(R.string.app_title_main))
            assertThat(a.findViewById<View>(R.id.home_top_buttons).visibility).isEqualTo(View.VISIBLE)
        }
    }

    @Test
    fun anAvailableUpdateIsFlaggedOnTheSettingsButton() = launch { scenario ->
        try {
            MainActivity.mHasUpdate = true
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.onActivity { a ->
                assertThat(a.findViewById<View>(R.id.home_settings_badge).visibility).isEqualTo(View.VISIBLE)
                assertThat(a.findViewById<View>(R.id.btn_home_settings).contentDescription).isEqualTo(a.getString(R.string.c_settings_update_desc))
            }
        } finally {
            MainActivity.mHasUpdate = false
        }
    }

    @Test
    fun theHomeTopBarHoldsOnlyContentsAndSettings() = launch { scenario ->
        scenario.onActivity { a ->
            assertThat(a.findViewById<View>(R.id.home_top_buttons).let { (it as android.view.ViewGroup).childCount }).isEqualTo(2)
        }
    }

    @Test
    fun homeKeepsTheTypedNumberWhileAnotherPageIsShown() = launch {
        onView(withId(R.id.n1)).perform(scrollTo(), click())
        onView(withId(R.id.n2)).perform(scrollTo(), click())
        openSettings()
        pressBack()
        onView(withId(R.id.tv_entry)).check(matches(withText(org.hamcrest.CoreMatchers.containsString("12"))))
    }

    @Test
    @QuickTest
    fun theOpenPageSurvivesRecreation() = launch { scenario ->
        openToc()
        scenario.recreate()
        onView(withId(R.id.toc_books)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_entry)).check(doesNotExist())
        scenario.onActivity { a ->
            assertThat(a.supportFragmentManager.backStackEntryCount).isEqualTo(1)
            // Home waits on the back stack under the page; recreation does not add a second one
            assertThat(a.supportFragmentManager.fragments.count { it is org.cog.hymnchtv.ui.home.HomeFragment }).isAtMost(1)
        }
        pressBack()
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { a ->
            assertThat(a.supportFragmentManager.fragments.count { it is org.cog.hymnchtv.ui.home.HomeFragment }).isEqualTo(1)
        }
    }

    @Test
    @QuickTest
    fun backFromAFullPageGoesHomeThenNeedsASecondPressToFinish() = launch { scenario ->
        openSettings()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
        scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        // The press that came back from the page does not count: the first press on home only shows the hint
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        onView(withText(R.string.c_press_back_again)).check(matches(isDisplayed()))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { assertThat(it.isFinishing).isTrue() }
    }

    @Test
    fun theSecondBackPressAfterTheWindowOnlyShowsTheHintAgain() = launch { scenario ->
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        android.os.SystemClock.sleep(BackExitGuard.WINDOW_MS + 300)
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        scenario.onActivity { assertThat(it.isFinishing).isFalse() }
    }

    @Test
    @QuickTest
    fun backClosesTheHistoryPageBeforeFinishing() {
        // the recent row is read when the home tab resumes, so the record must exist before the launch
        val db = DatabaseBackend.getInstance(ctx)
        db.historyRecords.forEach { db.deleteHymnHistory(it) }
        db.storeHymnHistory(HistoryRecord(MainActivity.HYMN_DB, 1, false))
        launch { scenario ->
            FragmentHost.eventually { onView(withId(R.id.btn_recent_more)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE))) }
            onView(withId(R.id.btn_recent_more)).perform(scrollTo(), click())
            onView(withId(R.id.history_list)).check(matches(isDisplayed()))
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            onView(withId(R.id.history_list)).check(doesNotExist())
            onView(withId(R.id.tv_entry)).check(matches(isDisplayed()))
            scenario.onActivity { assertThat(it.isFinishing).isFalse() }
        }
    }

    @Test
    @QuickTest
    fun openButtonIsShownOnHomeAndStartsDisabled() = launch {
        onView(withId(R.id.btn_open)).perform(scrollTo()).check(matches(isDisplayed())).check(matches(not(isEnabled())))
    }

    /** The toolbar reaches behind the status bar and pads for it; the home page pads its own bottom for the navigation bar. */
    @Test
    fun toolbarReachesBehindTheBarsAndHomePadsForThem() = launch { scenario ->
        scenario.onActivity { a ->
            val root = a.window.decorView
            val insets = androidx.core.view.ViewCompat.getRootWindowInsets(root)!!
                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val toolbar = a.findViewById<View>(R.id.toolbar)
            val loc = IntArray(2)
            toolbar.getLocationOnScreen(loc)
            assertThat(loc[1]).isAtMost(0)
            assertThat(toolbar.paddingTop).isAtLeast(insets.top)
            assertThat(toolbar.height).isEqualTo(toolbar.paddingTop + (48 * a.resources.displayMetrics.density).toInt())
            val content = a.findViewById<View>(R.id.home_content)
            assertThat(content.paddingBottom).isAtLeast(insets.bottom)
            // The home background reaches the bottom screen edge
            val bg = a.findViewById<View>(R.id.mainBackground)
            bg.getLocationOnScreen(loc)
            assertThat(loc[1] + bg.height).isAtLeast(root.height - 1)
            // Transparent bars: the frame colour behind them is the toolbar's own
            assertThat(a.window.statusBarColor).isEqualTo(android.graphics.Color.TRANSPARENT)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                assertThat(a.window.navigationBarColor).isEqualTo(android.graphics.Color.TRANSPARENT)
            }
        }
    }

    @Test
    fun aFullPageTakesTheSystemBarInsetsItself() = launch { scenario ->
        openSettings()
        FragmentHost.eventually {
            scenario.onActivity { a ->
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(a.window.decorView)!!
                    .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val page = a.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                assertThat(page.paddingBottom).isAtLeast(insets.bottom)
                assertThat(a.findViewById<View>(R.id.viewMain).paddingBottom).isEqualTo(0)
                val loc = IntArray(2)
                page.getLocationOnScreen(loc)
                assertThat(loc[1]).isAtMost(0)
            }
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
    fun settingsPageStillReachesHelpAboutAndUpdateCheck() = launch { scenario ->
        openSettings()
        mapOf("online_help" to R.string.help_online, "about" to R.string.about, "check_update" to R.string.c_check_update)
            .forEach { (key, title) ->
                scrollSettingsTo(scenario, key)
                onView(withText(title)).check(matches(isDisplayed()))
            }
    }

    @Test
    fun helpEntryOpensTheBuiltInHelpPage() = launch { scenario ->
        openSettings()
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
        openSettings()
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
