package org.cog.hymnchtv

import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import de.cketti.library.changelog.ChangeLog
import org.junit.Test
import org.junit.runner.RunWith

/** The run listener marks this version's change log as seen before any test launches MainActivity. */
@RunWith(AndroidJUnit4::class)
class ChangeLogSeenTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theRunListenerIsRegisteredThroughTheTestManifest() {
        assertThat(HymnalTestListener.started).isTrue()
    }

    @Test
    fun thisVersionsChangeLogCountsAsSeen() {
        assertThat(PreferenceManager.getDefaultSharedPreferences(ctx).getInt(ChangeLogSeen.KEY, -1))
            .isEqualTo(BuildConfig.VERSION_CODE)
        // the key must be ckChangeLog's own: the library itself has to agree that nothing is new
        assertThat(ChangeLog(ctx).isFirstRun).isFalse()
    }

    @Test
    fun clearMakesTheChangeLogNewAgainAndMarkRestoresIt() {
        try {
            ChangeLogSeen.clear(ctx)
            assertThat(ChangeLog(ctx).isFirstRun).isTrue()
        } finally {
            ChangeLogSeen.mark(ctx)
        }
        assertThat(ChangeLog(ctx).isFirstRun).isFalse()
    }
}
