package org.cog.hymnchtv.update

/** STAGING_FAILED is produced by ApkVerifier (I/O), never by [ApkTrust]. */
enum class ApkCheck {
    OK, CHECKSUM_MISMATCH, UNREADABLE, WRONG_PACKAGE, WRONG_VERSION, NOT_NEWER, WRONG_SIGNER, SIGNER_UNVERIFIABLE, STAGING_FAILED,
}

/** The running app: package, version code and SHA-256 of its signing certificate(s). */
data class InstalledApp(val packageName: String, val versionCode: Long, val signers: Set<String>)

/** Facts read from a downloaded APK; nulls mean the platform could not read them. */
data class ArchiveApk(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val signers: Set<String>?,
    /** Signing lineage, original first (APK Signature Scheme v3 key rotation); empty when unknown. */
    val signerHistory: List<String> = emptyList(),
)

/**
 * Decides whether a downloaded APK may be offered for installation. Android also refuses updates signed by a
 * different key; this check exists so the user gets a clear message instead of a failed install.
 */
object ApkTrust {
    @JvmStatic
    fun check(
        expectedVersion: SemVer,
        expectedSha256: String,
        actualSha256: String?,
        installed: InstalledApp,
        archive: ArchiveApk,
    ): ApkCheck {
        if (!expectedSha256.equals(actualSha256, ignoreCase = true)) return ApkCheck.CHECKSUM_MISMATCH
        val packageName = archive.packageName ?: return ApkCheck.UNREADABLE
        if (packageName != installed.packageName) return ApkCheck.WRONG_PACKAGE
        if (SemVer.parse(archive.versionName) != expectedVersion) return ApkCheck.WRONG_VERSION
        val versionCode = archive.versionCode ?: return ApkCheck.UNREADABLE
        if (versionCode <= installed.versionCode) return ApkCheck.NOT_NEWER
        // Fail closed: without both certificate sets the update is not verified, on any API level.
        val signers = archive.signers
        if (signers.isNullOrEmpty() || installed.signers.isEmpty()) return ApkCheck.SIGNER_UNVERIFIABLE
        return if (isSameSigner(installed.signers, signers, archive.signerHistory)) ApkCheck.OK else ApkCheck.WRONG_SIGNER
    }

    @JvmStatic
    fun isSameSigner(installed: Set<String>, archive: Set<String>, archiveHistory: List<String>): Boolean {
        if (installed.isEmpty() || archive.isEmpty()) return false
        if (installed == archive) return true
        // Key rotation: a single-signer update whose lineage contains the installed signer.
        return installed.size == 1 && archive.size == 1 && installed.single() in archiveHistory
    }
}
