package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.home.HomeFragment
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Number keys and book buttons tick when pressed (spec 5b-4), through the system's touch-feedback setting. */
@RunWith(AndroidJUnit4::class)
class KeyHapticsTest {
    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() {
        PickerTestSupport.cleanUp()
    }

    private fun shell(command: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    /** A view that records how it was asked to tick instead of ticking. */
    private class Recorder(context: Context) : View(context) {
        val calls = mutableListOf<Pair<Int, Int>>()

        override fun performHapticFeedback(feedbackConstant: Int, flags: Int): Boolean {
            calls += feedbackConstant to flags
            return true
        }
    }

    @Test fun theTickIsAKeyboardTapWithoutOverridingTheSystemSetting() {
        val view = Recorder(ApplicationProvider.getApplicationContext())
        KeyHaptics.keyTap(view)
        assertThat(view.calls).containsExactly(HapticFeedbackConstants.KEYBOARD_TAP to 0)
    }

    @Test fun digitsAndBookButtonsTickWhenPressed() = PickerTestSupport.launch { scenario ->
        val ticked = mutableListOf<Int>()
        scenario.onActivity { a ->
            (a.supportFragmentManager.findFragmentById(R.id.fragment_container) as HomeFragment).haptic = { ticked += it.id }
        }
        onView(withId(R.id.bs_bb)).perform(scrollTo(), click())
        onView(withId(R.id.n7)).perform(scrollTo(), click())
        onView(withId(R.id.n11)).perform(scrollTo(), click())
        assertThat(ticked).containsExactly(R.id.bs_bb, R.id.n7, R.id.n11).inOrder()
    }
}
