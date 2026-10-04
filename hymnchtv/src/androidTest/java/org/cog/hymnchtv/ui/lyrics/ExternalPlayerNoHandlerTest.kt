package org.cog.hymnchtv.ui.lyrics

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.mediaplayer.MediaExoPlayerFragment
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/** 1.1.1 crash: ExoPlayer cannot play a URL and no installed app can open it; this must be a toast, not an ActivityNotFoundException. */
@RunWith(AndroidJUnit4::class)
class ExternalPlayerNoHandlerTest : LyricsTestBase() {
    /** An audio/mpeg URL on a scheme nobody handles. */
    private val url = "hymnchtv-none://example/a.mp3"

    @Test
    fun startingTheUnhandledIntentDirectlyThrows() {
        // The bug being fixed: this is what playVideoUrlExt used to do unconditionally
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "audio/mpeg").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        assertThat(intent.resolveActivity(ctx.packageManager)).isNull()
        val thrown = runCatching { ctx.startActivity(intent) }.exceptionOrNull()
        assertThat(thrown).isInstanceOf(ActivityNotFoundException::class.java)
    }

    @Test
    fun noHandlerKeepsTheFragmentAndDoesNotCrash() {
        launch().use { s ->
            val result = arrayOfNulls<Boolean>(1)
            s.onActivity { activity ->
                val containerId = android.view.View.generateViewId()
                val container = FrameLayout(activity).apply { id = containerId }
                activity.addContentView(container, FrameLayout.LayoutParams(1, 1))
                val args = Bundle().apply { putString(MediaExoPlayerFragment.ATTR_MEDIA_URL, url) }
                val fragment = MediaExoPlayerFragment.getInstance(args, null)
                activity.supportFragmentManager.beginTransaction().add(containerId, fragment, TAG).commitNow()
                result[0] = fragment.playVideoUrlExt(url)
            }
            assertThat(result[0]).isFalse()
            // Let ExoPlayer's own failure (STATE_IDLE -> playVideoUrlExt) run too; nothing may crash or detach the fragment
            SystemClock.sleep(1500)
            instrumentation.waitForIdleSync()
            val alive = AtomicBoolean(false)
            s.onActivity { alive.set(it.supportFragmentManager.findFragmentByTag(TAG)?.isAdded == true) }
            assertThat(alive.get()).isTrue()
        }
    }

    private companion object {
        const val TAG = "exo_no_handler"
    }
}
