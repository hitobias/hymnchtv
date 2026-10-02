package org.cog.hymnchtv.reading

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.GridView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.ContentHandler
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.R
import org.cog.hymnchtv.reading.background.BackgroundPolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackgroundImporter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** In photo mode every text drawn on the photo has the PHOTO_PALETTE panel behind it (Codex P2). API 24 and 34. */
@RunWith(AndroidJUnit4::class)
class PhotoBackdropTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val prefs = ctx.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)
    private val keys = listOf(BackgroundSlot.MAIN.prefKey, BackgroundSlot.LYRICS.prefKey)
    private lateinit var photo: File

    @Before
    fun setUp() {
        // the permission MainActivity asks for at launch; granted through UiAutomation (no androidx.test.rules dependency)
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS
        else Manifest.permission.WRITE_EXTERNAL_STORAGE
        InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(ctx.packageName, permission)
        photo = PhotoBackgroundImporter.photoFileIn(ctx.filesDir)
        photo.parentFile?.mkdirs()
        val white = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        photo.outputStream().use { white.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        prefs.edit()
            .putString(BackgroundSlot.MAIN.prefKey, BackgroundPolicy.PHOTO)
            .putString(BackgroundSlot.LYRICS.prefKey, BackgroundPolicy.PHOTO)
            .commit()
    }

    @After
    fun tearDown() {
        val editor = prefs.edit()
        keys.forEach { editor.remove(it) }
        editor.commit()
        photo.delete()
    }

    private fun assertPanel(view: View, what: String) {
        val panel = view.background
        assertWithMessage(what).that(panel).isInstanceOf(GradientDrawable::class.java)
        assertWithMessage(what).that((panel as GradientDrawable).color?.defaultColor)
            .isEqualTo(BackgroundPolicy.PHOTO_PALETTE.backdropColor)
    }

    private fun <A : android.app.Activity, T> ActivityScenario<A>.read(block: (A) -> T): T {
        val ref = AtomicReference<T>()
        onActivity { ref.set(block(it)) }
        return ref.get()
    }

    @Test
    fun lyricsPageTextSitsOnThePanel() {
        val extras = Bundle().apply {
            putString(MainActivity.ATTR_HYMN_TYPE, MainActivity.HYMN_DB)
            putInt(MainActivity.ATTR_HYMN_NUMBER, 1)
        }
        ActivityScenario.launch<ContentHandler>(Intent(ctx, ContentHandler::class.java).putExtras(extras)).use { scenario ->
            val end = SystemClock.uptimeMillis() + 10_000
            fun page(a: ContentHandler) =
                a.supportFragmentManager.fragments.filterIsInstance<ContentView>().singleOrNull { it.isResumed }?.view
            while (scenario.read { page(it) == null }) {
                check(SystemClock.uptimeMillis() < end) { "no lyrics page" }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
            scenario.onActivity {
                val page = page(it)!!
                assertPanel(page.findViewById(R.id.lyrics_simplified), "lyrics_simplified")
                assertPanel(page.findViewById(R.id.lyrics_traditional), "lyrics_traditional")
                assertPanel(page.findViewById(R.id.lyrics_english), "lyrics_english")
            }
        }
    }

    @Test
    fun mainScreenHintSitsOnThePanel() {
        // MainActivity may show its changelog dialog; the hint is still in the activity's own window
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertPanel(it.findViewById<TextView>(R.id.tv_hint), "tv_hint") }
        }
    }

    @Test
    fun pickerPhotoPreviewSampleSitsOnThePanel() {
        val intent = BackgroundPickerActivity.intent(ctx, BackgroundSlot.LYRICS)
        ActivityScenario.launch<BackgroundPickerActivity>(intent).use { scenario ->
            scenario.onActivity {
                val grid = it.findViewById<GridView>(R.id.backgroundGrid)
                val adapter = grid.adapter
                val photoCell = adapter.getView(adapter.count - 1, null, grid)   // "your photo" is the last cell
                assertPanel(photoCell.findViewById(R.id.bgSample), "picker photo sample")
                val presetCell = adapter.getView(0, null, grid)
                assertThat(presetCell.findViewById<View>(R.id.bgSample).background).isNull()
            }
        }
    }
}
