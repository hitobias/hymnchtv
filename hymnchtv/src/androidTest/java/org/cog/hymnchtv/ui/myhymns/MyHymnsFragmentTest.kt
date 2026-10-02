package org.cog.hymnchtv.ui.myhymns

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyHymnsFragmentTest {
    @Test
    fun slotIsPresentForD1() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { FragmentHost.show(it, MyHymnsFragment()) }
            onView(withId(R.id.myHymnsContainer)).check(matches(isDisplayed()))
            onView(withId(R.id.myHymnsPlaceholder)).check(matches(isDisplayed()))
        }
    }
}
