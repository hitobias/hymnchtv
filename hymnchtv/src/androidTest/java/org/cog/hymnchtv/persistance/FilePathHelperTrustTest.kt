package org.cog.hymnchtv.persistance

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.TestPermissions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** A path named by the sending app is never used in place when it leads into our private data. */
@RunWith(AndroidJUnit4::class)
class FilePathHelperTrustTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val tmpDir get() = FileBackend.getHymnchtvStore(FileBackend.TMP, true)!!
    private val cleanup = mutableListOf<File>()

    @Before
    fun setUp() {
        TestPermissions.grantLaunchPermission(ctx.packageName)
    }

    @After
    fun tearDown() {
        cleanup.forEach { it.delete() }
    }

    private fun privateFile(): File = File(ctx.filesDir, "secret.db").apply { writeText("secret"); cleanup += this }

    @Test
    fun aTraversalDataColumnYieldsAFreshCopyNotThePrivatePath() {
        val secret = privateFile()
        val lie = tmpDir.path + "/../../../../../../../../data/user/0/${ctx.packageName}/files/secret.db"
        assertThat(File(lie).canonicalPath).isEqualTo(secret.canonicalPath)
        val uri = Uri.parse("content://com.ziontkec.hymnal.test.lying/x").buildUpon()
            .appendQueryParameter("data", lie).appendQueryParameter("name", "lying-copy.csv")
            .appendQueryParameter("body", "harmless").build()

        val path = FilePathHelper.getFilePath(ctx, uri)!!.also { cleanup += File(it) }
        assertThat(File(path).canonicalPath).isNotEqualTo(secret.canonicalPath)
        assertThat(File(path).parentFile!!.canonicalPath).isEqualTo(tmpDir.canonicalPath)
        assertThat(File(path).readText()).isEqualTo("harmless")
    }

    @Test
    fun aFileUriIsCopiedNotUsedDirectly() {
        val secret = privateFile()
        val path = FilePathHelper.getFilePath(ctx, Uri.fromFile(secret))!!.also { cleanup += File(it) }
        assertThat(File(path).canonicalPath).isNotEqualTo(secret.canonicalPath)
        assertThat(File(path).parentFile!!.canonicalPath).isEqualTo(tmpDir.canonicalPath)
    }
}
