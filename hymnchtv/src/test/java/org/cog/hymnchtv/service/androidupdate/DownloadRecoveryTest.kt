package org.cog.hymnchtv.service.androidupdate

import android.app.DownloadManager
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.service.androidupdate.DownloadRecovery.Action
import org.cog.hymnchtv.update.SemVer
import org.junit.Test

class DownloadRecoveryTest {
    private val newer = SemVer(1, 7, 0)
    private val installed = SemVer(1, 6, 0)

    private fun decide(latest: Boolean = true, status: Int = DownloadManager.STATUS_SUCCESSFUL, file: Boolean = true,
                       pending: SemVer? = newer, current: SemVer? = installed) =
        DownloadRecovery.decide(latest, status, file, pending, current)

    @Test
    fun aDownloadThatFinishedWhileTheAppWasDeadIsVerifiedNotDeleted() {
        assertThat(decide()).isEqualTo(Action.VERIFY)
    }

    @Test
    fun aDownloadStillRunningGetsItsReceiverBack() {
        for (s in listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED)) {
            assertThat(decide(status = s, file = false)).isEqualTo(Action.WAIT)
        }
    }

    @Test
    fun everythingElseIsCleanedUpAsBefore() {
        assertThat(decide(status = DownloadManager.STATUS_FAILED)).isEqualTo(Action.DISCARD)
        assertThat(decide(file = false)).isEqualTo(Action.DISCARD)                 // successful but the file is gone
        assertThat(decide(latest = false)).isEqualTo(Action.DISCARD)               // not the latest enqueued id
        assertThat(decide(pending = installed)).isEqualTo(Action.DISCARD)          // that version is installed now
        assertThat(decide(pending = SemVer(1, 5, 0))).isEqualTo(Action.DISCARD)
        assertThat(decide(pending = null)).isEqualTo(Action.DISCARD)
        assertThat(decide(current = null)).isEqualTo(Action.DISCARD)
    }
}
