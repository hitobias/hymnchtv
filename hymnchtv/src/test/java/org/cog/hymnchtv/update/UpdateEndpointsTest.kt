package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.identity.UpdateSource
import org.junit.Test

class UpdateEndpointsTest {
    private val override = "api=http://10.0.2.2:8000/latest.json\nprefix=http://10.0.2.2:8000/\n"

    @Test
    fun releaseBuildsIgnoreOverrideFile() {
        assertThat(UpdateEndpoints.resolve(false, override)).isEqualTo(UpdateEndpoints.PRODUCTION)
    }

    @Test
    fun debugWithoutUsableOverrideUsesProduction() {
        listOf(null, "", "# comment only", "prefix=http://10.0.2.2:8000/", "api=").forEach {
            assertThat(UpdateEndpoints.resolve(true, it)).isEqualTo(UpdateEndpoints.PRODUCTION)
        }
    }

    @Test
    fun debugOverrideReplacesBothEndpoints() {
        assertThat(UpdateEndpoints.resolve(true, " api = http://10.0.2.2:8000/latest.json \r\nprefix=http://10.0.2.2:8000/"))
            .isEqualTo(UpdateEndpoints("http://10.0.2.2:8000/latest.json", "http://10.0.2.2:8000/"))
    }

    @Test
    fun debugOverrideWithoutPrefixKeepsProductionPrefix() {
        assertThat(UpdateEndpoints.resolve(true, "api=http://10.0.2.2:8000/latest.json"))
            .isEqualTo(UpdateEndpoints("http://10.0.2.2:8000/latest.json", UpdateSource.RELEASE_DOWNLOAD_PREFIX))
    }
}
