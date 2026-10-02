package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ApkTrustTest {
    private val version = SemVer(1, 1, 0)
    private val sha = "3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd"
    private val certA = "a".repeat(64)
    private val certB = "b".repeat(64)
    private val installed = InstalledApp("com.ziontkec.hymnal", 100000L, setOf(certA))
    private val good = ArchiveApk("com.ziontkec.hymnal", "1.1.0", 101000L, setOf(certA), listOf(certA))

    private fun check(
        archive: ArchiveApk = good,
        actualSha: String? = sha,
    ) = ApkTrust.check(version, sha, actualSha, installed, archive)

    @Test
    fun acceptsTrustedUpdate() {
        assertThat(check()).isEqualTo(ApkCheck.OK)
        assertThat(check(actualSha = sha.uppercase())).isEqualTo(ApkCheck.OK)
    }

    @Test
    fun checksumIsCheckedFirst() {
        assertThat(check(actualSha = "0".repeat(64))).isEqualTo(ApkCheck.CHECKSUM_MISMATCH)
        assertThat(check(archive = good.copy(packageName = "evil"), actualSha = null)).isEqualTo(ApkCheck.CHECKSUM_MISMATCH)
    }

    @Test
    fun unreadableArchiveIsRefused() {
        assertThat(check(archive = ArchiveApk(null, null, null, null))).isEqualTo(ApkCheck.UNREADABLE)
        assertThat(check(archive = good.copy(versionCode = null))).isEqualTo(ApkCheck.UNREADABLE)
    }

    @Test
    fun rejectsOtherPackages() {
        assertThat(check(archive = good.copy(packageName = "org.cog.hymnchtv"))).isEqualTo(ApkCheck.WRONG_PACKAGE)
    }

    @Test
    fun rejectsVersionNameOtherThanTheTag() {
        listOf("1.0.0", "1.1.0-debug", "1.1.1", null).forEach {
            assertThat(check(archive = good.copy(versionName = it))).isEqualTo(ApkCheck.WRONG_VERSION)
        }
    }

    @Test
    fun requiresHigherVersionCodeThanInstalled() {
        assertThat(check(archive = good.copy(versionCode = 100000L))).isEqualTo(ApkCheck.NOT_NEWER)
        assertThat(check(archive = good.copy(versionCode = 1L))).isEqualTo(ApkCheck.NOT_NEWER)
    }

    @Test
    fun rejectsDifferentSigner() {
        assertThat(check(archive = good.copy(signers = setOf(certB), signerHistory = listOf(certB)))).isEqualTo(ApkCheck.WRONG_SIGNER)
        assertThat(check(archive = good.copy(signers = setOf(certA, certB)))).isEqualTo(ApkCheck.WRONG_SIGNER)
    }

    @Test
    fun acceptsKeyRotationLineageContainingInstalledSigner() {
        assertThat(check(archive = good.copy(signers = setOf(certB), signerHistory = listOf(certA, certB)))).isEqualTo(ApkCheck.OK)
    }

    /** Fail closed on every API level: an unverifiable signer is never reported as verified. */
    @Test
    fun unreadableSignersAreNeverTrusted() {
        assertThat(check(archive = good.copy(signers = null, signerHistory = emptyList()))).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
        assertThat(check(archive = good.copy(signers = emptySet(), signerHistory = emptyList()))).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
    }

    @Test
    fun installedSignerUnknownIsNeverTrusted() {
        val result = ApkTrust.check(version, sha, sha, installed.copy(signers = emptySet()), good)
        assertThat(result).isEqualTo(ApkCheck.SIGNER_UNVERIFIABLE)
    }
}
