package org.cog.hymnchtv.service.androidupdate

import android.app.DownloadManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DownloadStartTest {
    private class FakeSteps(private val statusAfterRegister: Int) : DownloadStart.Steps {
        val calls = mutableListOf<String>()
        override fun enqueue(): Long = 7L.also { calls += "enqueue" }
        override fun persist(id: Long) { calls += "persist:$id" }
        override fun registerReceiver() { calls += "register" }
        override fun status(id: Long): Int = statusAfterRegister.also { calls += "status:$id" }
        override fun verifyNow(id: Long) { calls += "verify:$id" }
    }

    @Test
    fun theIdIsStoredBeforeTheReceiverListensAndTheStatusIsCheckedAfter() {
        val steps = FakeSteps(DownloadManager.STATUS_RUNNING)
        assertThat(DownloadStart.run(steps)).isEqualTo(7L)
        assertThat(steps.calls).containsExactly("enqueue", "persist:7", "register", "status:7").inOrder()
    }

    @Test
    fun aDownloadThatFinishedBeforeTheReceiverListenedIsVerifiedAtOnce() {
        val steps = FakeSteps(DownloadManager.STATUS_SUCCESSFUL)
        DownloadStart.run(steps)
        assertThat(steps.calls).containsExactly("enqueue", "persist:7", "register", "status:7", "verify:7").inOrder()
    }
}
