package org.cog.hymnchtv.update

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.service.androidupdate.PendingDownload
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Start-up recovery of the update download (1.6.0) on the real DownloadManager; the decision itself is DownloadRecoveryTest. */
@RunWith(AndroidJUnit4::class)
class DownloadRecoveryStartTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    /** UpdateServiceImpl's preferences; "apk_ids" is its ENTRY_NAME. */
    private val store = ctx.getSharedPreferences("store", Context.MODE_PRIVATE)

    @After
    fun clean() {
        store.edit().remove("apk_ids").remove(PendingDownload.PREF_KEY).commit()
    }

    @Test
    fun aRecordOfADownloadManagerNoLongerKnowsIsCleanedUp() {
        val id = 987_654_321L
        store.edit().putString("apk_ids", "$id,")
            .putString(PendingDownload.PREF_KEY, PendingDownload(id, "v99.0.0", "hymnal-99.0.0.apk", "a".repeat(64)).encode())
            .commit()
        UpdateServiceImpl.getInstance().resumeOrCleanOnStart()
        assertThat(store.contains(PendingDownload.PREF_KEY)).isFalse()
        assertThat(store.contains("apk_ids")).isFalse()
    }

    @Test
    fun withoutARecordStartUpCleansAsBefore() {
        store.edit().putString("apk_ids", "123456789,").commit()
        UpdateServiceImpl.getInstance().resumeOrCleanOnStart()
        assertThat(store.contains("apk_ids")).isFalse()
    }
}
