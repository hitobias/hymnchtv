package org.cog.hymnchtv.identity

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.BuildConfig
import org.junit.Test

class ApplicationIdentityTest {
    @Test
    fun applicationIdIsTheNewIdentity() {
        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("com.ziontkec.hymnal")
    }

    /** versionCode = X*100000 + Y*1000 + Z*10 + n (n = 0..9), so GitHub releases always upgrade cleanly. */
    @Test
    fun versionCodeFollowsVersionName() {
        val version = checkNotNull(org.cog.hymnchtv.update.SemVer.parse(BuildConfig.VERSION_NAME))
        assertThat(version.preRelease).isEmpty()
        val base = version.major * 100000L + version.minor * 1000L + version.patch * 10L
        assertThat(BuildConfig.VERSION_CODE.toLong() - base).isIn(com.google.common.collect.Range.closed(0L, 9L))
    }
}
