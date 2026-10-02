package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReleaseConventionTest {
    @Test
    fun namesFollowTheConvention() {
        val v = SemVer(1, 1, 0)
        assertThat(ReleaseConvention.tagFor(v)).isEqualTo("v1.1.0")
        assertThat(ReleaseConvention.apkName(v)).isEqualTo("hymnal-1.1.0.apk")
        assertThat(ReleaseConvention.sha256Name(v)).isEqualTo("hymnal-1.1.0.apk.sha256")
    }

    @Test
    fun recognisesOnlyPlainApkNames() {
        assertThat(ReleaseConvention.isApkName("hymnal-1.1.0.apk")).isTrue()
        listOf(null, "", "../hymnal-1.1.0.apk", "hymnal-1.1.0.apk.sha256", "x/hymnal-1.1.0.apk", "hymnal-1.1.apk")
            .forEach { assertThat(ReleaseConvention.isApkName(it)).isFalse() }
    }
}
