package org.cog.hymnchtv.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.View
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaGuiController
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.ReadingPrefKeys
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.cog.hymnchtv.ui.lyrics.LyricsTestBase
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Not a check: takes the collapsible player screenshots into the app's external files (pull with adb).
 * Runs only with `-e screenshotTag <name>`.
 */
@RunWith(AndroidJUnit4::class)
class PlayerScreenshotTest : LyricsTestBase() {
    private val prefs get() = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private val looks = listOf("light" to "xuan", "dark" to "nightread", "photo" to BackgroundPolicy.PHOTO)

    private fun shoot(file: File) {
        val pfd = instrumentation.uiAutomation.executeShellCommand("screencap -p ${file.absolutePath}")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        check(file.length() > 0) { "screencap wrote nothing to $file" }
    }

    private fun silence(): Uri {
        val rate = 8000
        val data = ByteArray(rate * 2 * 60)
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }
        return File(ctx.cacheDir, "shot-silence.wav").apply { writeBytes(header.array() + data) }.toUri()
    }

    private fun ActivityScenario<ContentHandler>.settle(ms: Long = 1200) = SystemClock.sleep(ms)

    private fun rotate(s: ActivityScenario<ContentHandler>, landscape: Boolean) {
        s.onActivity {
            it.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        SystemClock.sleep(2500)
    }

    @Test
    fun takePlayerScreenshots() {
        val tag = InstrumentationRegistry.getArguments().getString("screenshotTag")
        assumeTrue("pass -e screenshotTag to take screenshots", !tag.isNullOrBlank())
        val dir = File(checkNotNull(ctx.getExternalFilesDir(null)), "vis-player").apply { mkdirs() }
        val bytes = instrumentation.context.assets.open("test_photo_bg.jpg").use { it.readBytes() }
        val photo = File(ctx.cacheDir, "test_photo_bg.jpg").apply { writeBytes(bytes) }
        check(PhotoBackgroundImporter.import(ctx, Uri.fromFile(photo)))
        prefs.edit().putBoolean(ReadingPrefKeys.MENU_SHOW, true).commit()
        try {
            for ((name, background) in looks) {
                prefs.edit().putString(BackgroundSlot.LYRICS.prefKey, background).commit()
                launch().use { s ->
                    s.onActivity { it.setChromeHeld(true) }
                    s.settle(2500)
                    shoot(File(dir, "${tag}_${name}_portrait_expanded_idle.png"))
                    s.onActivity { it.findViewById<View>(R.id.btn_player_collapse).performClick() }
                    s.settle()
                    shoot(File(dir, "${tag}_${name}_portrait_collapsed_idle.png"))
                    s.onActivity { it.findViewById<View>(R.id.playerCapsule).findViewById<View>(R.id.capsuleNote).performClick() }
                    s.settle()
                    val ctl = s.read { it.supportFragmentManager.findFragmentById(R.id.mediaPlayer) as MediaGuiController }
                    s.onActivity { ctl.playUriForTest(silence()) }
                    s.settle(9000)
                    shoot(File(dir, "${tag}_${name}_portrait_expanded_playing.png"))
                    s.onActivity { it.findViewById<View>(R.id.btn_player_collapse).performClick() }
                    s.settle()
                    shoot(File(dir, "${tag}_${name}_portrait_collapsed_playing.png"))
                    rotate(s, landscape = true)
                    shoot(File(dir, "${tag}_${name}_landscape_capsule_playing.png"))
                    s.onActivity { it.findViewById<View>(R.id.capsuleExpand).performClick() }
                    s.settle()
                    shoot(File(dir, "${tag}_${name}_landscape_expanded_playing.png"))
                    s.onActivity { it.findViewById<View>(R.id.btn_player_collapse).performClick() }
                    s.settle()
                    ctl.stopPlay()
                    s.settle(1500)
                    shoot(File(dir, "${tag}_${name}_landscape_capsule_idle.png"))
                    rotate(s, landscape = false)
                    s.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
                }
            }
        } finally {
            prefs.edit().remove(BackgroundSlot.LYRICS.prefKey).commit()
        }
    }
}
