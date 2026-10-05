package org.cog.hymnchtv.service.androidupdate

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.update.SemVer
import org.junit.Test

class PendingDownloadTest {
    private val sha = "0123456789abcdef".repeat(4)
    private val pending = PendingDownload(42L, "v1.6.0", "hymnal-1.6.0.apk", sha)

    @Test
    fun aRecordSurvivesItsTextForm() {
        assertThat(PendingDownload.decode(pending.encode())).isEqualTo(pending)
    }

    @Test
    fun theReleaseForVerificationHasTheTagVersionAndName() {
        val release = pending.toRelease()
        assertThat(release.tag).isEqualTo("v1.6.0")
        assertThat(release.version).isEqualTo(SemVer(1, 6, 0))
        assertThat(release.apkName).isEqualTo("hymnal-1.6.0.apk")
        assertThat(release.versionName).isEqualTo("1.6.0")
    }

    @Test
    fun anythingMalformedIsNoRecord() {
        val bad = listOf(
            null, "", "42", "42|v1.6.0|hymnal-1.6.0.apk", "x|v1.6.0|hymnal-1.6.0.apk|$sha", "-1|v1.6.0|hymnal-1.6.0.apk|$sha",
            "42|1.6|hymnal-1.6.apk|$sha", "42|v1.6.0|hymnal-1.5.0.apk|$sha", "42|v1.6.0|../hymnal-1.6.0.apk|$sha",
            "42|v1.6.0|hymnal-1.6.0.apk|${sha.uppercase()}", "42|v1.6.0|hymnal-1.6.0.apk|abc", "42|v1.6.0|hymnal-1.6.0.apk|$sha|extra",
        )
        for (text in bad) assertThat(PendingDownload.decode(text)).isNull()
    }
}
