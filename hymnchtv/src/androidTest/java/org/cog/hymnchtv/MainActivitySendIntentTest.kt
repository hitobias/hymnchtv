package org.cog.hymnchtv

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.mediaconfig.MediaConfig
import org.cog.hymnchtv.persistance.FileBackend
import org.cog.hymnchtv.share.ShareFiles
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Files other apps share with MainActivity (exported SEND): no crash on bad intents, never the stale old copy. */
@RunWith(AndroidJUnit4::class)
class MainActivitySendIntentTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    private val name = "send-intent-test.csv"
    private val source = File(ShareFiles.dir(ctx.cacheDir), name)
    private val cleanup = mutableListOf<File>()

    @Before
    fun setUp() {
        TestPermissions.grantLaunchPermission(ctx.packageName)
        source.parentFile!!.mkdirs()
    }

    @After
    fun tearDown() {
        source.delete()
        cleanup.forEach { it.delete() }
    }

    private fun send(): Intent = Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/csv")

    @Test
    fun sendWithoutAStreamDoesNotCrash() {
        ActivityScenario.launch<MainActivity>(send()).use { assertThat(it.state).isEqualTo(Lifecycle.State.RESUMED) }
    }

    @Test
    fun sendMultipleWithAnEmptyListDoesNotCrash() {
        val intent = send().setAction(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>())
        ActivityScenario.launch<MainActivity>(intent).use { assertThat(it.state).isEqualTo(Lifecycle.State.RESUMED) }
    }

    @Test
    fun reSharingAFileOfTheSameNameImportsTheNewContent() {
        // an older import of the same name is already in Download/hymnal/tmp
        // createNew=false: the path only, without the permission check that needs a live MainActivity
        val tmpDir = FileBackend.getHymnchtvStore(FileBackend.TMP, false)!!.apply { mkdirs() }
        File(tmpDir, name).also { cleanup += it }.writeText("old export")
        source.writeText("hymn_db,1,false,HYMN_MEDIA,https://example.org/v2,\n")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", source)

        val monitor = instrumentation.addMonitor(MediaConfig::class.java.name, null, false)
        try {
            val intent = send().putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ActivityScenario.launch<MainActivity>(intent).use {
                val config = instrumentation.waitForMonitorWithTimeout(monitor, 10_000)
                assertThat(config).isNotNull()
                val imported = File(config!!.intent.getStringExtra(MainActivity.ATTR_MEDIA_URI)!!).also { cleanup += it }
                assertThat(imported.parentFile).isEqualTo(tmpDir)
                assertThat(imported.readText()).contains("https://example.org/v2")
                config.finish()
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun aSharedFileIsCopiedOffTheMainThread() {
        MainActivity.sharedImportThreadForTest = null
        source.writeText("hymn_db,2,false,HYMN_MEDIA,https://example.org/v3,\n")
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", source)
        val monitor = instrumentation.addMonitor(MediaConfig::class.java.name, null, false)
        try {
            val intent = send().putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ActivityScenario.launch<MainActivity>(intent).use {
                val config = instrumentation.waitForMonitorWithTimeout(monitor, 10_000)
                assertThat(config).isNotNull()
                cleanup += File(config!!.intent.getStringExtra(MainActivity.ATTR_MEDIA_URI)!!)
                config.finish()
            }
            assertThat(MainActivity.sharedImportThreadForTest).isEqualTo("hymn-io")
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
