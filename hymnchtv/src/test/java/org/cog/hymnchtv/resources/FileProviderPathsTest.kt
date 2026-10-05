package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** FileProvider roots (res/xml/file_paths.xml) used by share, media config, log upload and the updater. */
class FileProviderPathsTest {
    private val xml = File("src/main/res/xml/file_paths.xml").readText()

    @Test
    fun shareFilesLiveInTheCacheDirectory() {
        assertThat(xml).containsMatch("""<cache-path\s+name="share"\s+path="share/"\s*/>""")
    }

    /**
     * Not narrowed: MediaConfig shares the user's import file and media files from any external path (an SD card
     * too), LogUploadServiceImpl shares Download/hymnal/logs, and the updater hands files/updates/ to the installer.
     */
    @Test
    fun theRootsOtherFeaturesNeedAreKept() {
        assertThat(xml).containsMatch("""<external-path\s+name="external"\s+path="/"\s*/>""")
        assertThat(xml).containsMatch("""<root-path\s+name="storage"\s+path="/storage/"\s*/>""")
        assertThat(xml).containsMatch("""<files-path\s+name="updates"\s+path="updates/"\s*/>""")
    }
}
