package org.cog.hymnchtv.mediaconfig

import android.os.SystemClock
import android.view.View
import android.widget.ListView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.IoGate
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The records list is read on AppExecutors.io and delivered to the started screen (plan section 2.4, item 2). */
@RunWith(AndroidJUnit4::class)
class MediaConfigRecordsListTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = DatabaseBackend.getInstance(ctx)
    private val seeded = listOf(
        MediaRecord(MainActivity.HYMN_DB, 9001, false, MediaType.HYMN_MEDIA, "https://example.org/9001", null),
        MediaRecord(MainActivity.HYMN_DB, 9002, false, MediaType.HYMN_MEDIA, "https://example.org/9002", null),
    )

    @Before
    fun setUp() {
        seeded.forEach { backend.storeMediaRecord(it) }
    }

    @After
    fun tearDown() {
        seeded.forEach { backend.deleteMediaRecord(it) }
    }

    @Test
    fun dbRecordsButtonFillsTheListOnlyAfterTheBackgroundRead() {
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            val expected = backend.getMediaRecords(MainActivity.HYMN_DB).size
            assertThat(expected).isAtLeast(2)

            // Occupy the io thread: a read on the main thread would fill the list at once, a background read cannot.
            val gate = IoGate.close()
            try {
                scenario.onActivity { it.findViewById<View>(R.id.button_db_records).performClick() }
                Thread.sleep(500)
                scenario.onActivity {
                    assertThat(it.findViewById<ListView>(R.id.mrListView).visibility).isNotEqualTo(View.VISIBLE)
                    assertThat(it.findViewById<ListView>(R.id.mrListView).adapter?.count ?: 0).isEqualTo(0)
                }
            } finally {
                gate.release()
            }

            val deadline = SystemClock.elapsedRealtime() + 10_000
            var count = 0
            while (SystemClock.elapsedRealtime() < deadline && count != expected) {
                scenario.onActivity { count = it.findViewById<ListView>(R.id.mrListView).adapter?.count ?: 0 }
                if (count != expected) Thread.sleep(50)
            }
            assertThat(count).isEqualTo(expected)
        }
    }
}
