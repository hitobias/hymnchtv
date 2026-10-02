package org.cog.hymnchtv.ui.home

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.titles.HymnTitleSource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeFragmentTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        prefs.edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    @After
    fun tearDown() {
        prefs.edit().remove(HomePrefs.LAST_HYMN_TYPE).commit()
    }

    private fun withHome(fragment: HomeFragment = HomeFragment(), block: (HomeFragment) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { FragmentHost.show(it, fragment) }
            block(fragment)
        }
    }

    @Test
    fun keysFillTheEntryAndDeleteClearsIt() = withHome {
        onView(withId(R.id.n1)).perform(click())
        onView(withId(R.id.n2)).perform(click())
        onView(withId(R.id.tv_entry)).check(matches(withText("12")))
        onView(withId(R.id.n11)).perform(click())
        onView(withId(R.id.tv_entry)).check(matches(withText("")))
    }

    @Test
    fun fuKeyPrefixesTheEntry() = withHome {
        onView(withId(R.id.n10)).perform(click())
        onView(withId(R.id.n3)).perform(click())
        onView(withId(R.id.tv_entry)).check(matches(withText("附3")))
    }

    @Test
    fun typedNumberShowsItsTitleLive() = withHome {
        onView(withId(R.id.n1)).perform(click())
        FragmentHost.eventually { onView(withId(R.id.title_preview)).check(matches(withText("祂的计划（英1）"))) }
        onView(withId(R.id.n11)).perform(click())
        FragmentHost.eventually { onView(withId(R.id.title_preview)).check(matches(withText(""))) }
    }

    @Test
    fun titleComesFromTheInjectedSource() {
        val fake = HymnTitleSource { type, no -> if (no == 7) "fake-$type" else null }
        withHome(HomeFragment().apply { titleSource = fake }) {
            onView(withId(R.id.n7)).perform(click())
            FragmentHost.eventually { onView(withId(R.id.title_preview)).check(matches(withText("fake-hymn_db"))) }
        }
    }

    @Test
    fun rememberedHymnBookIsHighlighted() {
        prefs.edit().putString(HomePrefs.LAST_HYMN_TYPE, MainActivity.HYMN_BB).commit()
        withHome {
            onView(withId(R.id.bs_bb)).check { v, _ -> assertThat(v.isSelected).isTrue() }
            onView(withId(R.id.bs_db)).check { v, _ -> assertThat(v.isSelected).isFalse() }
        }
    }

    @Test
    fun addPlaylistButtonIsPresent() = withHome {
        // below the keypad: the home tab scrolls
        onView(withId(R.id.btn_add_playlist)).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
