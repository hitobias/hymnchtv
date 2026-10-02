package org.cog.hymnchtv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.glide.AssetFile
import org.cog.hymnchtv.persistance.FileBackend
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetAndStorageTest {
    @Test
    fun assetFileOpensOwnAssets() {
        val stream = AssetFile(InstrumentationRegistry.getInstrumentation().targetContext, "url_import.txt").inputStream
        assertThat(stream).isNotNull()
        stream?.close()
    }

    @Test
    fun publicStoreDirectoryIsHymnal() {
        assertThat(FileBackend.FP_HYMNCHTV).isEqualTo("/hymnal")
    }
}
