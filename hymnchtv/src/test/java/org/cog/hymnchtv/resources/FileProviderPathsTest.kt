package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.share.MediaShareFiles
import org.junit.Test
import java.io.File

/** FileProvider roots (res/xml/file_paths.xml) used by share, media config, log upload and the updater. */
class FileProviderPathsTest {
    private val xml = File("src/main/res/xml/file_paths.xml").readText()

    @Test
    fun shareFilesLiveInTheCacheDirectory() {
        assertThat(xml).containsMatch("""<cache-path\s+name="share"\s+path="share/"\s*/>""")
    }

    /** 1.6.0: no broad roots; media config copies files outside Download/hymnal/ into the share cache (MediaShareFiles). */
    @Test
    fun noBroadRootsAreLeft() {
        assertThat(xml).doesNotContainMatch("""<external-path[^>]*path="/"""")
        assertThat(xml).doesNotContain("<root-path")
    }

    @Test
    fun theRootsOtherFeaturesNeedAreKept() {
        assertThat(xml).containsMatch("""<external-path\s+name="hymnal"\s+path="${MediaShareFiles.HYMNAL_ROOT}"\s*/>""")
        assertThat(xml).containsMatch("""<files-path\s+name="updates"\s+path="updates/"\s*/>""")
    }
}
