package org.cog.hymnchtv.ui.lyrics

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.notebook.NotebookTestSupport
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.ui.FragmentHost
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D-1 F4 wiring on the lyrics page: a completed playback logs the hymn only while the switch is on. The 2-minute visibility
 * timer itself is covered by SingTrackerTest on the JVM (an instrumented test would have to wait two minutes).
 */
@RunWith(AndroidJUnit4::class)
class SingLogHooksTest : LyricsTestBase() {
    private val db5 = HymnKey.of(HymnTypes.DB, 5)

    @Before fun reset() = NotebookTestSupport.resetSingLogs(db5)

    @After fun cleanUp() {
        NotebookTestSupport.resetSingLogs(db5)
        NotebookTestSupport.resetAutoRecord()
    }

    @Test fun aCompletedPlaybackIsLoggedWhenTheSwitchIsOn() {
        NotebookTestSupport.setAutoRecord(true)
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.onActivity { it.onPlaybackCompleted() }
            FragmentHost.eventually(5000) {
                val logs = NotebookTestSupport.singLogs(db5)
                assertThat(logs).hasSize(1)
                assertThat(logs.single().source).isEqualTo(SingSource.AUTO)
            }
            // a second completion within three hours is the same singing
            s.onActivity { it.onPlaybackCompleted() }
            SystemClock.sleep(1000)
            assertThat(NotebookTestSupport.singLogs(db5)).hasSize(1)
        }
    }

    @Test fun nothingIsLoggedWhileTheSwitchIsOff() {
        NotebookTestSupport.resetAutoRecord()
        launch(MainActivity.HYMN_DB, 5).use { s ->
            s.onActivity { it.onPlaybackCompleted() }
            SystemClock.sleep(1500)
            assertThat(NotebookTestSupport.singLogs(db5)).isEmpty()
        }
    }
}
