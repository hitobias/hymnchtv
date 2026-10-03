package org.cog.hymnchtv.ui.home

import android.content.Context
import android.os.ParcelFileDescriptor
import android.net.Uri
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.locale.AppLanguage
import org.cog.hymnchtv.locale.LocaleStore
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Not a check: takes the home screenshots of spec 7.3 into the app's external files (pull with adb). Runs only with
 * `-e screenshotTag <name>`, which also prefixes the files, because screen size and rotation are device settings the
 * caller changes between runs; `-e fontScale 1.3` sets (and afterwards restores) the system font scale.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenshotTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private data class Look(val name: String, val background: String)

    private val looks = listOf(
        Look("light", "dawn"),
        Look("dark", "nightread"),
        Look("photo", BackgroundPolicy.PHOTO),
        Look("reading-green", "eye_green"),
        Look("reading-black", "true_black"),
    )

    private companion object {
        const val SETTLE_MS = 3000L
    }

    private val languages = listOf("hant" to AppLanguage.ZH_HANT, "en" to AppLanguage.EN)

    private fun importPhoto() {
        val bytes = instrumentation.context.assets.open("test_photo_bg.jpg").use { it.readBytes() }
        val file = File(ctx.cacheDir, "test_photo_bg.jpg").apply { writeBytes(bytes) }
        check(PhotoBackgroundImporter.import(ctx, Uri.fromFile(file))) { "test photo import failed" }
    }

    private fun shell(command: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    /** The shell's screencap (UiAutomation.takeScreenshot returns null while a dialog or the keyguard has the focus on API 24). */
    private fun shoot(file: File) {
        shell("screencap -p ${file.absolutePath}")
        check(file.length() > 0) { "screencap wrote nothing to $file" }
    }

    @Test
    fun takeHomeScreenshots() {
        val tag = InstrumentationRegistry.getArguments().getString("screenshotTag")
        assumeTrue("pass -e screenshotTag to take screenshots", !tag.isNullOrBlank())
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        val dir = File(checkNotNull(ctx.getExternalFilesDir(null)), "vis-home").apply { mkdirs() }
        importPhoto()
        val fontScale = InstrumentationRegistry.getArguments().getString("fontScale")
        if (fontScale != null) shell("settings put system font_scale $fontScale").also { SystemClock.sleep(SETTLE_MS) }
        try {
            for ((langName, language) in languages) {
                LocaleStore.set(ctx, language)
                for (look in looks) {
                    prefs.edit().putString(BackgroundSlot.MAIN.prefKey, look.background).commit()
                    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                        scenario.onActivity { a ->
                            // book 1 (Main), hymn 12: shows the preview card with a title
                            a.findViewById<View>(R.id.bs_db).performClick()
                            a.findViewById<View>(R.id.n1).performClick()
                            a.findViewById<View>(R.id.n2).performClick()
                        }
                        SystemClock.sleep(1500)
                        instrumentation.waitForIdleSync()
                        shoot(File(dir, "home_${tag}_${look.name}_$langName.png"))
                    }
                }
            }
        } finally {
            if (fontScale != null) shell("settings put system font_scale 1.0")
            prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
            PhotoBackgroundImporter.photoFileIn(ctx.filesDir).delete()
            LocaleStore.set(ctx, AppLanguage.SYSTEM)
        }
    }
}
