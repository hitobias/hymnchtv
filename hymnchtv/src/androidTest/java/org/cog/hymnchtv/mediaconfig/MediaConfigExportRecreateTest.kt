package org.cog.hymnchtv.mediaconfig

import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.R
import org.cog.hymnchtv.TestPermissions
import org.cog.hymnchtv.concurrent.IoGate
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The background export outcome must reach the screen that exists when it arrives, e.g. after a rotation. */
@RunWith(AndroidJUnit4::class)
class MediaConfigExportRecreateTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = DatabaseBackend.getInstance(ctx)
    private val seeded =
        MediaRecord(MainActivity.HYMN_DB, 9003, false, MediaType.HYMN_MEDIA, "https://example.org/9003", null)
    private var exportedPath: String? = null

    /** API 24-28: FileBackend.getHymnchtvStore() reads MainActivity.getInstance(), which is null unless MainActivity exists. */
    private var mainScenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        TestPermissions.grantLaunchPermission(ctx.packageName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            mainScenario = ActivityScenario.launch(MainActivity::class.java)
        }
        backend.storeMediaRecord(seeded)
    }

    @After
    fun tearDown() {
        mainScenario?.close()
        mainScenario = null
        backend.deleteMediaRecord(seeded)
        exportedPath?.let { File(it).delete() }
    }

    private fun ActivityScenario<MediaConfig>.importFileText(): String {
        var text = ""
        onActivity { text = it.findViewById<EditText>(R.id.importFile).text.toString() }
        return text
    }

    @Test
    fun exportOutcomeReachesTheRecreatedScreenOnceAndOnlyOnce() {
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            // The export result is posted while the old screen is already gone: hold the io thread until recreated.
            val gate = IoGate.close()
            try {
                scenario.onActivity { it.findViewById<View>(R.id.button_export).performClick() }
                scenario.recreate()
            } finally {
                gate.release()
            }

            val deadline = SystemClock.elapsedRealtime() + 10_000
            var text = scenario.importFileText()
            while (SystemClock.elapsedRealtime() < deadline && !text.contains("hymn_link-")) {
                Thread.sleep(50)
                text = scenario.importFileText()
            }
            exportedPath = text.takeIf { it.contains("hymn_link-") }
            assertThat(text).contains("hymn_link-")

            // Delivered once: nothing is left to show on the next screen.
            assertThat(MediaConfig.isExportOutcomePending()).isFalse()

            // A successful export opens the file in RichTextEditor; close it again (the full export is large and the
            // editor cannot be stopped in the background with that much saved state).
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            Thread.sleep(500)
        }
    }
}
