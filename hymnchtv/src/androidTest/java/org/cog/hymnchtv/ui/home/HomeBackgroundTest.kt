package org.cog.hymnchtv.ui.home

import android.content.Context
import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.picker.PickerTestSupport
import org.cog.hymnchtv.ui.picker.PickerTestSupport.ctx
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Changing the home background in the settings shows on the home tab at once and is kept after a restart. */
@RunWith(AndroidJUnit4::class)
class HomeBackgroundTest {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before fun setUp() {
        PickerTestSupport.prepare()
        prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
    }

    @After fun tearDown() {
        prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
        PickerTestSupport.cleanUp()
    }

    /** The colour the background view paints at its top-left, found by drawing it (presets are gradients, layers or fills). */
    private fun topColor(a: MainActivity): Int? {
        val view = a.findViewById<ImageView>(R.id.mainBackground)
        val bg = view.background?.constantState?.newDrawable()?.mutate() ?: return null
        val bmp = android.graphics.Bitmap.createBitmap(16, 64, android.graphics.Bitmap.Config.ARGB_8888)
        bg.setBounds(0, 0, 16, 64)
        bg.draw(android.graphics.Canvas(bmp))
        return bmp.getPixel(2, 2)
    }

    @Test fun backgroundChosenWhileAwayShowsWhenTheHomeTabComesBack() = PickerTestSupport.launch { scenario ->
        var before: Int? = null
        scenario.onActivity { before = topColor(it) }
        onView(withId(R.id.nav_settings)).perform(click())
        prefs.edit().putString(BackgroundSlot.MAIN.prefKey, BackgroundPreset.INK.id).commit()
        onView(withId(R.id.nav_home)).perform(click())
        FragmentHost.eventually {
            scenario.onActivity { a ->
                assertThat(topColor(a)).isNotNull()
                assertThat(topColor(a)).isNotEqualTo(before)
            }
        }
    }

    @Test fun backgroundChosenInASeparateActivityShowsWhenMainActivityResumes() = PickerTestSupport.launch { scenario ->
        var before: Int? = null
        scenario.onActivity { before = topColor(it) }
        prefs.edit().putString(BackgroundSlot.MAIN.prefKey, BackgroundPreset.INK.id).commit()
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        FragmentHost.eventually { scenario.onActivity { a -> assertThat(topColor(a)).isNotEqualTo(before) } }
    }

    @Test fun backgroundIsKeptAfterARestart() {
        prefs.edit().putString(BackgroundSlot.MAIN.prefKey, BackgroundPreset.INK.id).commit()
        PickerTestSupport.launch { scenario ->
            scenario.onActivity { a -> assertThat(topColor(a)).isNotNull() }
        }
    }
}
