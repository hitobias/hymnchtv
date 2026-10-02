package org.cog.hymnchtv.ui.lyrics

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.locale.AppLanguage
import org.cog.hymnchtv.locale.LocaleStore
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Not a check: takes the lyrics page screenshots of spec 7.3 into the app's external files (pull with adb).
 * Runs only with `-e screenshotTag <name>`; screen size and rotation are device settings changed between runs.
 */
@RunWith(AndroidJUnit4::class)
class LyricsScreenshotTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    private val looks = listOf(
        "light" to "xuan",
        "dark" to "nightread",
        "photo" to BackgroundPolicy.PHOTO,
        "reading-green" to "eye_green",
        "reading-black" to "true_black",
    )

    private fun importPhoto() {
        val bytes = instrumentation.context.assets.open("test_photo_bg.jpg").use { it.readBytes() }
        val file = File(ctx.cacheDir, "test_photo_bg.jpg").apply { writeBytes(bytes) }
        check(PhotoBackgroundImporter.import(ctx, Uri.fromFile(file))) { "test photo import failed" }
    }

    private fun shoot(file: File) {
        val pfd = instrumentation.uiAutomation.executeShellCommand("screencap -p ${file.absolutePath}")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        check(file.length() > 0) { "screencap wrote nothing to $file" }
    }

    @Test
    fun takeLyricsScreenshots() {
        val tag = InstrumentationRegistry.getArguments().getString("screenshotTag")
        assumeTrue("pass -e screenshotTag to take screenshots", !tag.isNullOrBlank())
        val dir = File(checkNotNull(ctx.getExternalFilesDir(null)), "vis-lyrics").apply { mkdirs() }
        importPhoto()
        prefs.edit().putBoolean(ReadingPrefKeys.MENU_SHOW, true).commit()
        try {
            for ((langName, language) in listOf("hant" to AppLanguage.ZH_HANT, "en" to AppLanguage.EN)) {
                LocaleStore.set(ctx, language)
                for ((name, background) in looks) {
                    prefs.edit().putString(BackgroundSlot.LYRICS.prefKey, background).commit()
                    launch().use { s ->
                        s.onActivity { it.setChromeHeld(true) }
                        SystemClock.sleep(2500)
                        shoot(File(dir, "lyrics_${tag}_${name}_$langName.png"))
                        if (name == "light" && langName == "hant") {
                            s.onActivity { it.showReadingPanel() }
                            SystemClock.sleep(1200)
                            shoot(File(dir, "lyrics_${tag}_aa-panel.png"))
                        }
                    }
                    if (name == "light" || name == "dark") {
                        launch().use { s ->
                            s.advance(3_000)
                            SystemClock.sleep(2500)
                            shoot(File(dir, "lyrics_${tag}_${name}_${langName}_hidden.png"))
                        }
                    }
                }
            }
        } finally {
            prefs.edit().remove(BackgroundSlot.LYRICS.prefKey).remove(ReadingPrefKeys.MENU_SHOW).commit()
            PhotoBackgroundImporter.photoFileIn(ctx.filesDir).delete()
            LocaleStore.set(ctx, AppLanguage.SYSTEM)
        }
    }
}
