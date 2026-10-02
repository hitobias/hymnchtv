package org.cog.hymnchtv.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Baseline regression guard for the UI refactor: enter a number, open the lyrics, flip a page, press play. */
@RunWith(AndroidJUnit4::class)
class SmokeFlowTest {
    @Test
    fun inputOpensLyricsFlipPageThenPlay() {
        // MainActivity asks for a runtime permission at launch; the dialog would take focus from the activity
        val pkg = ApplicationProvider.getApplicationContext<android.content.Context>().packageName
        TestPermissions.grantLaunchPermission(pkg)
        if (Build.VERSION.SDK_INT >= 33) {
            listOf("AUDIO", "IMAGES", "VIDEO").forEach { grant(pkg, "android.permission.READ_MEDIA_$it") }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            onView(withId(R.id.n1)).perform(click())
            // the home tab scrolls: the toolbar and bottom navigation leave little room on a small screen
            onView(withId(R.id.bs_db)).perform(scrollTo(), click())
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.viewPager)).perform(swipeLeft())
            onView(withId(R.id.viewPager)).check(matches(isDisplayed()))
            onView(withId(R.id.playback_play)).perform(click())
            scenario.onActivity { a -> assertFalse(a.isFinishing) }
        }
    }

    private fun grant(pkg: String, permission: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant $pkg $permission")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
}
