package org.cog.hymnchtv.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.cog.hymnchtv.MediaGuiController
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A media download that fails after the lyrics page has gone delivers its DOWNLOAD_COMPLETE broadcast to a player card
 * that is no longer attached; updating it must be a no-op rather than an IllegalStateException that kills the app.
 */
@RunWith(AndroidJUnit4::class)
class LateDownloadCallbackTest {
    @Test
    fun playIconUpdateOnADetachedPlayerCardDoesNotCrash() {
        getInstrumentation().runOnMainSync {
            MediaGuiController().showPlayIcon(true)
            MediaGuiController().showPlayIcon(false)
        }
    }
}
