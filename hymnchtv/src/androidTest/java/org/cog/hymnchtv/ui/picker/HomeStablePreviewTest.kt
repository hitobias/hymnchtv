package org.cog.hymnchtv.ui.picker

import android.content.Context
import android.os.SystemClock
import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.locale.AppLanguage
import org.cog.hymnchtv.locale.LocaleStore
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * The preview card has one fixed height, so typing never moves the keypad or the open key: every key's position in the
 * scrolled content stays the same through valid, invalid, "also in" and English two-candidate states (tolerance 0).
 */
@RunWith(AndroidJUnit4::class)
class HomeStablePreviewTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    private val watched = listOf(
        R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9, R.id.n10, R.id.n11, R.id.btn_open,
        R.id.keypadArea, R.id.recentArea, R.id.previewArea,
    )

    @Before fun setUp() = PickerTestSupport.prepare()

    @After fun tearDown() {
        LocaleStore.set(ctx, AppLanguage.SYSTEM)
        PickerTestSupport.cleanUp()
    }


    private fun digitId(c: Char) = intArrayOf(R.id.n0, R.id.n1, R.id.n2, R.id.n3, R.id.n4, R.id.n5, R.id.n6, R.id.n7, R.id.n8, R.id.n9)[c - '0']

    private fun click(scenario: ActivityScenario<MainActivity>, id: Int) {
        scenario.onActivity { it.findViewById<View>(id).performClick() }
        SystemClock.sleep(STEP_MS)
        instrumentation.waitForIdleSync()
    }

    private fun positions(scenario: ActivityScenario<MainActivity>): Map<Int, Int> {
        var result = emptyMap<Int, Int>()
        scenario.onActivity { a ->
            val scroller = generateSequence(a.findViewById<View>(R.id.tv_search).parent) { it.parent }
                .filterIsInstance<NestedScrollView>().first()
            val loc = IntArray(2)
            result = watched.associateWith { id ->
                a.findViewById<View>(id).getLocationInWindow(loc)
                loc[1] + scroller.scrollY
            } + (HEIGHT_KEY to a.findViewById<View>(R.id.previewArea).height)
            android.util.Log.i("StablePreview", "previewArea height ${result[HEIGHT_KEY]!! / a.resources.displayMetrics.density}dp")
        }
        return result
    }

    private fun run(language: AppLanguage, widthDp: Int, heightDp: Int) {
        LocaleStore.set(ctx, language)
        // The size is a device setting changed by the caller (changing it inside the test has crashed the test process)
        val dm = ctx.resources.displayMetrics
        val actual = "${(dm.widthPixels / dm.density).toInt()}x${(dm.heightPixels / dm.density).toInt()}"
        assumeTrue("needs a ${widthDp}x$heightDp dp screen (run: adb shell wm size), this one is $actual", actual == "${widthDp}x$heightDp")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val steps = listOf<Pair<String, () -> Unit>>(
                "empty" to {},
                "DB 1" to { click(scenario, R.id.bs_db); click(scenario, R.id.n1) },
                "DB 19" to { click(scenario, R.id.n9) },
                "DB 190" to { click(scenario, R.id.n0) },
                "BB 40 invalid + also in" to {
                    click(scenario, R.id.bs_bb); click(scenario, R.id.n11); click(scenario, R.id.n11); click(scenario, R.id.n11)
                    "40".forEach { click(scenario, digitId(it)) }
                },
                "BB 99 invalid" to { click(scenario, R.id.n11); click(scenario, R.id.n11); "99".forEach { click(scenario, digitId(it)) } },
                "English 254 two candidates" to {
                    click(scenario, R.id.bs_english); click(scenario, R.id.n11); click(scenario, R.id.n11); click(scenario, R.id.n11)
                    "254".forEach { click(scenario, digitId(it)) }
                },
                "English 1 one candidate" to { click(scenario, R.id.n11); click(scenario, R.id.n11); click(scenario, R.id.n11); click(scenario, R.id.n1) },
                "cleared" to { scenario.onActivity { a -> a.findViewById<View>(R.id.n11).performLongClick() } },
            )
            var baseline: Map<Int, Int>? = null
            for ((name, action) in steps) {
                action()
                SystemClock.sleep(STEP_MS)
                instrumentation.waitForIdleSync()
                val now = positions(scenario)
                val first = baseline ?: now.also { baseline = it }
                assertWithMessage("$language ${widthDp}x$heightDp step '$name': key positions $now vs first $first").that(now).isEqualTo(first)
            }
        }
    }

    @Test fun traditionalChinese360x720() = run(AppLanguage.ZH_HANT, 360, 720)

    @Test fun english360x720() = run(AppLanguage.EN, 360, 720)

    @Test fun traditionalChinese320x640() = run(AppLanguage.ZH_HANT, 320, 640)

    @Test fun english320x640() = run(AppLanguage.EN, 320, 640)

    @Test fun traditionalChineseLandscape() = run(AppLanguage.ZH_HANT, 720, 360)

    @Test fun englishLandscape() = run(AppLanguage.EN, 720, 360)

    /** Not a check: with `-e stableShots <tag>` saves the typing states to the app's external files (pull with adb). */
    @Test fun screenshots() {
        val tag = InstrumentationRegistry.getArguments().getString("stableShots")
        assumeTrue("pass -e stableShots to take screenshots", !tag.isNullOrBlank())
        LocaleStore.set(ctx, AppLanguage.ZH_HANT)
        val dir = java.io.File(checkNotNull(ctx.getExternalFilesDir(null)), "stable").apply { mkdirs() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun shoot(name: String) {
                SystemClock.sleep(STEP_MS * 3)
                instrumentation.waitForIdleSync()
                val file = java.io.File(dir, "${tag}_$name.png")
                val pfd = instrumentation.uiAutomation.executeShellCommand("screencap -p ${file.absolutePath}")
                android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
            }
            click(scenario, R.id.bs_db)
            shoot("1_empty")
            for (digits in (InstrumentationRegistry.getArguments().getString("longTitle") ?: "123").map { it }) click(scenario, digitId(digits))
            shoot("2_valid_long_title")
            click(scenario, R.id.bs_bb)
            click(scenario, R.id.n11); click(scenario, R.id.n11); click(scenario, R.id.n11)
            "40".forEach { click(scenario, digitId(it)) }
            shoot("3_invalid_also_in")
        }
    }

    private companion object {
        const val HEIGHT_KEY = -1
        const val STEP_MS = 250L
    }
}
