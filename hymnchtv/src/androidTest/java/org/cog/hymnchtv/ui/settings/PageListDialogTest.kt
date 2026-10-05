package org.cog.hymnchtv.ui.settings

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** 1.6.0: the list dialogs of the settings pages take the page palette, like the rows behind them. */
@RunWith(AndroidJUnit4::class)
class PageListDialogTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        FragmentHost.grantLaunchPermissions(ctx.packageName)
    }

    @Test
    fun theThemeDialogIsPaintedWithThePagePalette() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var fragment: SettingsFragment? = null
            scenario.onActivity { fragment = FragmentHost.show(it, SettingsFragment()) }
            onView(withText(R.string.theme_menu)).perform(click())
            onView(withText(R.string.theme_dark)).inRoot(isDialog()).check(matches(isDisplayed()))
            scenario.onActivity { a ->
                val palette = fragment!!.palette!!
                val dialogFragment = a.supportFragmentManager.findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG") as DialogFragment
                val dialog = dialogFragment.dialog as AlertDialog
                val background = dialog.window!!.decorView.background as InsetDrawable
                assertThat((background.drawable as GradientDrawable).color!!.defaultColor).isEqualTo(palette.card)
                assertThat(dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)!!.currentTextColor).isEqualTo(palette.onCard)
                assertThat((dialog.listView.getChildAt(0) as CheckedTextView).currentTextColor).isEqualTo(palette.onCard)
            }
        }
    }
}
