package org.cog.hymnchtv.persistance

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PathTrustTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val ext get() = File(tmp.root, "storage/emulated/0").apply { mkdirs() }
    private val data get() = File(tmp.root, "data/user/0/app").apply { mkdirs() }

    @Test
    fun onlySystemProvidersMayNameAPath() {
        for (a in listOf("media", "downloads", "com.android.providers.downloads.documents", "com.android.externalstorage.documents",
            "com.android.providers.media.documents")) assertThat(PathTrust.isTrustedAuthority(a)).isTrue()
        for (a in listOf("com.evil.provider", "org.cog.hymnchtv.files", "", null)) assertThat(PathTrust.isTrustedAuthority(a)).isFalse()
    }

    @Test
    fun traversalIntoTheAppDataDirIsRejected() {
        val db = File(data, "databases/x.db").apply { parentFile!!.mkdirs(); writeText("secret") }
        val tmpDir = File(ext, "Download/hymnal/tmp").apply { mkdirs() }
        val sneaky = tmpDir.path + "/../../../../../../data/user/0/app/databases/x.db"
        assertThat(File(sneaky).canonicalFile).isEqualTo(db.canonicalFile)
        assertThat(PathTrust.isAcceptable(sneaky, listOf(data), listOf(ext))).isFalse()
        assertThat(PathTrust.isAcceptable(db.path, listOf(data), listOf(ext))).isFalse()
    }

    @Test
    fun aPlainExternalFileIsAccepted() {
        val f = File(ext, "Download/a.csv").apply { parentFile!!.mkdirs(); writeText("x") }
        assertThat(PathTrust.isAcceptable(f.path, listOf(data), listOf(ext))).isTrue()
    }

    @Test
    fun aPathOutsideExternalStorageIsRejected() {
        assertThat(PathTrust.isAcceptable(File(tmp.root, "other/a.csv").path, listOf(data), listOf(ext))).isFalse()
        assertThat(PathTrust.isAcceptable("", listOf(data), listOf(ext))).isFalse()
    }

    @Test
    fun isUnderUsesTheCanonicalPath() {
        val dir = File(ext, "Download/hymnal/tmp").apply { mkdirs() }
        assertThat(PathTrust.isUnder(File(dir, "a.csv").path, dir)).isTrue()
        assertThat(PathTrust.isUnder(dir.path + "/../../x.csv", dir)).isFalse()
        // a name that merely contains "tmp" is not in the tmp dir
        assertThat(PathTrust.isUnder(File(ext, "Download/tmp-x/a.csv").path, dir)).isFalse()
        assertThat(PathTrust.isUnder(File(dir, "a.csv").path, null)).isFalse()
    }
}
