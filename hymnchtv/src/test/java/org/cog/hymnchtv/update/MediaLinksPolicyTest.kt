package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaLinksPolicyTest {
    @Test
    fun importsBundledListOnlyWhenItsVersionIsNewer() {
        assertThat(MediaLinksPolicy.shouldImport(-1, 1)).isTrue()
        assertThat(MediaLinksPolicy.shouldImport(1, 2)).isTrue()
        assertThat(MediaLinksPolicy.shouldImport(1, 1)).isFalse()
        assertThat(MediaLinksPolicy.shouldImport(2, 1)).isFalse()
        assertThat(MediaLinksPolicy.BUNDLED_VERSION).isAtLeast(1)
    }
}
