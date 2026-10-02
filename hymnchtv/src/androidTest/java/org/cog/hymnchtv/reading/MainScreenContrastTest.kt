package org.cog.hymnchtv.reading

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.MainScreenColors
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The main screen's entry, search box and keys are readable on every preset background (bug: black text on dark presets). */
@RunWith(AndroidJUnit4::class)
class MainScreenContrastTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    @Before
    fun setUp() = TestPermissions.grantLaunchPermission(ctx.packageName)

    @After
    fun tearDown() {
        prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
    }

    @Test
    fun textIsReadableOnEveryPreset() {
        for (preset in BackgroundPreset.entries) {
            prefs.edit().putString(BackgroundSlot.MAIN.prefKey, preset.id).commit()
            val palette = BackgroundPolicy.palette(BackgroundChoice.Preset(preset))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    for (id in listOf(R.id.tv_entry, R.id.tv_search, R.id.n1, R.id.bs_db, R.id.btn_toc)) {
                        val color = it.findViewById<TextView>(id).currentTextColor
                        assertWithMessage("${preset.id} view $id")
                            .that(Wcag.contrast(color, palette.paperColor)).isAtLeast(MainScreenColors.MIN_TEXT_CONTRAST)
                    }
                    // the filled open button is drawn on the palette's accent, not on the paper
                    val open = it.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_open)
                    assertWithMessage("${preset.id} open button")
                        .that(Wcag.contrast(open.textColors.defaultColor, open.backgroundTintList!!.defaultColor))
                        .isAtLeast(MainScreenColors.MIN_TEXT_CONTRAST)
                }
            }
        }
    }
}
