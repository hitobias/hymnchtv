package org.cog.hymnchtv.ui.home

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one-page home (spec 2): in portrait the page fits the screen and does not scroll; only when even 48dp keys do not
 * fit (small screens, the largest system font) or in landscape may it scroll, and then nothing is cut off. The device's
 * size and font scale are set by the caller (adb shell wm size / settings put system font_scale), so these checks hold
 * for whatever configuration the test runs on.
 */
@RunWith(AndroidJUnit4::class)
class HomeOnePageTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() = PickerTestSupport.cleanUp()

    private fun scroller(a: MainActivity): HomeScrollView =
        a.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView().findViewById(R.id.viewMain)

    private val controlIds = listOf(
        R.id.tv_search, R.id.bs_db, R.id.bs_bb, R.id.bs_xb, R.id.bs_xg, R.id.bs_yb, R.id.bs_er, R.id.bs_english, R.id.btn_toc,
        R.id.previewArea, R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9, R.id.n10, R.id.n11,
        R.id.btn_open, R.id.btn_recent_more,
    )

    private fun isPortrait(a: MainActivity) = a.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    private fun config(a: MainActivity): String {
        val dm = a.resources.displayMetrics
        return "${(dm.widthPixels / dm.density).toInt()}x${(dm.heightPixels / dm.density).toInt()}dp font ${a.resources.configuration.fontScale}"
    }

    private fun waitForFit(scenario: ActivityScenario<MainActivity>) = FragmentHost.eventually {
        scenario.onActivity { a -> assertThat(scroller(a).getChildAt(0).height).isGreaterThan(0) }
    }

    @Test fun portraitPageEitherFitsTheScreenWithoutScrollingOrIsAJustifiedScroll() = PickerTestSupport.launch { scenario ->
        waitForFit(scenario)
        scenario.onActivity { a ->
            assumeTrue("portrait only", isPortrait(a))
            val s = scroller(a)
            val d = a.resources.displayMetrics.density
            val picker = a.findViewById<View>(R.id.picker_root)
            val inner = s.getChildAt(0)
            val needed = picker.height + inner.paddingTop + inner.paddingBottom
            if (!s.scrollingAllowed) {
                assertWithMessage("${config(a)}: content ${needed}px must fit the ${s.height}px viewport").that(needed).isAtMost(s.height)
                assertThat(s.canScrollVertically(1)).isFalse()
                assertThat(s.canScrollVertically(-1)).isFalse()
                controlIds.forEach { id ->
                    val v = a.findViewById<View>(id)
                    val r = Rect()
                    assertWithMessage("${a.resources.getResourceEntryName(id)} visible (${config(a)})").that(v.getGlobalVisibleRect(r)).isTrue()
                    assertWithMessage("${a.resources.getResourceEntryName(id)} fully visible (${config(a)})").that(r.height()).isEqualTo(v.height)
                }
            } else {
                // The exception: 40dp keys, a compact preview and no recent rows still do not fit
                assertWithMessage("${config(a)}: scrolling needs a reason").that(needed).isGreaterThan(s.height - (HomeFit.SAFETY_DP * d).toInt())
                assertThat(a.findViewById<View>(R.id.key_row0).height).isAtLeast((HomeFit.MIN_KEY_FLOOR_DP * d).toInt() - 1)
            }
        }
    }

    @Test fun openIsFullyOnScreenWithoutScrollingAfterTypingANumber() = PickerTestSupport.launch { scenario ->
        waitForFit(scenario)
        onView(withId(R.id.bs_db)).perform(scrollTo(), click())
        PickerTestSupport.type("1")
        FragmentHost.eventually {
            scenario.onActivity { a ->
                assumeTrue("portrait only", isPortrait(a))
                val open = a.findViewById<View>(R.id.btn_open)
                assertWithMessage("btn_open enabled (${config(a)})").that(open.isEnabled).isTrue()
                val r = Rect()
                assertWithMessage("btn_open visible (${config(a)})").that(open.getGlobalVisibleRect(r)).isTrue()
                assertWithMessage("btn_open fully visible (${config(a)}): $r of ${open.height}px").that(r.height()).isEqualTo(open.height)
                assertThat(scroller(a).scrollY).isEqualTo(0)
            }
        }
    }

    @Test fun aPageThatDoesNotScrollIgnoresASwipe() = PickerTestSupport.launch { scenario ->
        waitForFit(scenario)
        var allowed = true
        scenario.onActivity { a -> allowed = scroller(a).scrollingAllowed }
        assumeTrue("only for the one-screen case", !allowed)
        onView(isAssignableFrom(HomeScrollView::class.java)).perform(swipeUp())
        scenario.onActivity { a -> assertThat(scroller(a).scrollY).isEqualTo(0) }
    }

    @Test fun aTallPortraitScreenAtNormalFontNeverScrolls() = PickerTestSupport.launch { scenario ->
        waitForFit(scenario)
        scenario.onActivity { a ->
            val dm = a.resources.displayMetrics
            assumeTrue(
                "needs a 411x891dp (or larger) portrait screen at font 1.0, this one is ${config(a)}",
                isPortrait(a) && dm.widthPixels / dm.density >= 411 && dm.heightPixels / dm.density >= 891 && a.resources.configuration.fontScale <= 1.0f,
            )
            assertThat(scroller(a).scrollingAllowed).isFalse()
        }
    }

    @Test fun keysAreBetween40And72dpInEveryConfiguration() = PickerTestSupport.launch { scenario ->
        waitForFit(scenario)
        scenario.onActivity { a ->
            val d = a.resources.displayMetrics.density
            listOf(R.id.n0, R.id.n5, R.id.n11).forEach { id ->
                assertThat(a.findViewById<View>(id).height).isAtLeast((HomeFit.MIN_KEY_FLOOR_DP * d).toInt() - 1)
                assertThat(a.findViewById<View>(id).height).isAtMost((72 * d).toInt() + 1)
            }
        }
    }

    @Test fun landscapeMayScrollAndStillReachesTheOpenKey() = PickerTestSupport.launch { scenario ->
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        FragmentHost.eventually(timeoutMs = 8000) {
            scenario.onActivity { a ->
                assertThat(a.resources.configuration.orientation).isEqualTo(android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                assertThat(scroller(a).scrollingAllowed).isTrue()
            }
        }
        onView(withId(R.id.btn_open)).perform(scrollTo()).check(matches(isDisplayed()))
        scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    @Test fun comingBackToTheForegroundStartsANewNumber() = PickerTestSupport.launch { scenario ->
        onView(withId(R.id.bs_db)).perform(scrollTo(), click())
        PickerTestSupport.type("12")
        onView(withId(R.id.tv_entry)).check(matches(withText(containsString("12"))))
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        // The number stays shown until the next key, which starts a new one; the book is kept
        PickerTestSupport.type("3")
        onView(withId(R.id.tv_entry)).check(matches(withText(containsString("3"))))
        scenario.onActivity { a ->
            assertThat(a.findViewById<android.widget.TextView>(R.id.tv_entry).text.toString()).doesNotContain("123")
            assertThat(a.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_db).isChecked).isTrue()
        }
    }
}
