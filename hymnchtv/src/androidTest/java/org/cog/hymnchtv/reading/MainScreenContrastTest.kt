package org.cog.hymnchtv.reading

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.google.android.material.button.MaterialButton
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.cog.hymnchtv.reading.background.BackgroundChoice
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundPreset
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.UiTokens
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The home screen's real view colours (spec 7.2) are the tokens of the chosen background and readable on every
 * surface they can sit on, for every preset; the open button's text and fill are read off the button itself.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenContrastTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private val disabled = intArrayOf(-android.R.attr.state_enabled)
    private val enabled = intArrayOf(android.R.attr.state_enabled)

    @Before
    fun setUp() = TestPermissions.grantLaunchPermission(ctx.packageName)

    @After
    fun tearDown() {
        prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
    }

    private fun ColorStateList.forState(s: IntArray) = getColorForState(s, defaultColor)

    @Test
    fun textAndOpenButtonAreReadableOnEveryPreset() {
        for (preset in BackgroundPreset.entries) {
            prefs.edit().putString(BackgroundSlot.MAIN.prefKey, preset.id).commit()
            val input = BackgroundPolicy.tokenInput(BackgroundChoice.Preset(preset))
            val t = UiTokens.from(input)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity {
                    // text on the card, the search box and the tone keys
                    for (id in listOf(R.id.tv_entry, R.id.tv_search, R.id.n1)) {
                        val color = it.findViewById<TextView>(id).currentTextColor
                        assertWithMessage("${preset.id} view $id").that(color).isEqualTo(t.onSurface)
                        for (s in input.swatches) for (b in UiTokens.backdropsOver(s, t.surface, t.surfaceTone)) {
                            assertWithMessage("${preset.id} view $id on ${"%08x".format(b)}")
                                .that(Wcag.contrast(color, b)).isAtLeast(4.5)
                        }
                    }
                    // the contents button: outline-action text on the solid surface
                    val toc = it.findViewById<MaterialButton>(R.id.btn_toc)
                    for (s in input.swatches) {
                        assertWithMessage("${preset.id} contents button")
                            .that(Wcag.contrast(toc.currentTextColor, UiTokens.over(s, t.surface))).isAtLeast(4.5)
                    }
                    // the open button, read from the button: enabled text on accent, disabled text on tone
                    val open = it.findViewById<MaterialButton>(R.id.btn_open)
                    val tint = open.backgroundTintList!!
                    assertWithMessage("${preset.id} open button enabled")
                        .that(Wcag.contrast(open.textColors.forState(enabled), tint.forState(enabled))).isAtLeast(4.5)
                    for (s in input.swatches) {
                        val fill = UiTokens.over(s, tint.forState(disabled))
                        assertWithMessage("${preset.id} open button disabled")
                            .that(Wcag.contrast(UiTokens.over(fill, open.textColors.forState(disabled)), fill)).isAtLeast(4.5)
                    }
                }
            }
        }
    }
}
