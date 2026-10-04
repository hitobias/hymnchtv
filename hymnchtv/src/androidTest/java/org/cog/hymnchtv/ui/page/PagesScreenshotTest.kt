package org.cog.hymnchtv.ui.page

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.reading.ReadingSettingsFragment
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.cog.hymnchtv.ui.FragmentHost
import org.cog.hymnchtv.ui.settings.SettingsFragment
import org.cog.hymnchtv.ui.toc.TocFragment
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Not a check: screenshots of the settings, reading settings and contents pages on four backgrounds (spec 7). Runs only with
 * `-e screenshotTag <name>`; files land in the app's external files under vis-theme (pull with adb).
 */
@RunWith(AndroidJUnit4::class)
class PagesScreenshotTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private val looks = listOf("light" to "paper_white", "dark" to "nightread", "bean-green" to "eye_green", "photo" to BackgroundPolicy.PHOTO)
    private val pages: List<Pair<String, () -> Fragment>> = listOf(
        "settings" to ::SettingsFragment, "reading" to ::ReadingSettingsFragment, "toc" to ::TocFragment,
    )

    private fun shoot(file: File) {
        val pfd = instrumentation.uiAutomation.executeShellCommand("screencap -p ${file.absolutePath}")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        check(file.length() > 0) { "screencap wrote nothing to $file" }
    }

    @Test
    fun takePageScreenshots() {
        val tag = InstrumentationRegistry.getArguments().getString("screenshotTag")
        assumeTrue("pass -e screenshotTag to take screenshots", !tag.isNullOrBlank())
        FragmentHost.grantLaunchPermissions(ctx.packageName)
        val dir = File(checkNotNull(ctx.getExternalFilesDir(null)), "vis-theme").apply { mkdirs() }
        val bytes = instrumentation.context.assets.open("test_photo_bg.jpg").use { it.readBytes() }
        check(PhotoBackgroundImporter.import(ctx, Uri.fromFile(File(ctx.cacheDir, "p.jpg").apply { writeBytes(bytes) })))
        try {
            for ((lookName, stored) in looks) {
                prefs.edit().putString(BackgroundSlot.MAIN.prefKey, stored).commit()
                for ((pageName, make) in pages) {
                    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                        scenario.onActivity { FragmentHost.show(it, make()) }
                        SystemClock.sleep(1500)
                        instrumentation.waitForIdleSync()
                        shoot(File(dir, "${tag}_${pageName}_$lookName.png"))
                    }
                }
            }
        } finally {
            prefs.edit().remove(BackgroundSlot.MAIN.prefKey).commit()
            PhotoBackgroundImporter.photoFileIn(ctx.filesDir).delete()
        }
    }
}
