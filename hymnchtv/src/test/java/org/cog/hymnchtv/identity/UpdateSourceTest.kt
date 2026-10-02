package org.cog.hymnchtv.identity

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class UpdateSourceTest {
    @Test
    fun pointsAtThisRepositorysReleases() {
        assertThat(UpdateSource.RELEASES_LATEST_API)
            .isEqualTo("https://api.github.com/repos/hitobias/hymnchtv/releases/latest")
        assertThat(UpdateSource.RELEASE_DOWNLOAD_PREFIX)
            .isEqualTo("https://github.com/hitobias/hymnchtv/releases/download/")
    }

    /** Proves tools/z-dev.init.gradle put a real org.json on the JVM test classpath. */
    @Test
    fun realOrgJsonIsAvailableInUnitTests() {
        assertThat(JSONObject("""{"a":1}""").getInt("a")).isEqualTo(1)
    }
}
