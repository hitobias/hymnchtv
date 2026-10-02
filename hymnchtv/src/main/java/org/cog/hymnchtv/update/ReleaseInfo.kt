package org.cog.hymnchtv.update

/** A published GitHub release that follows [ReleaseConvention]. */
data class ReleaseInfo(
    val tag: String,
    val version: SemVer,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
    /** URL of the required hymnal-X.Y.Z.apk.sha256 asset. */
    val sha256Url: String,
    /** GitHub's own asset digest (lower-case hex) when provided; cross-checked against the .sha256 file. */
    val apkDigestSha256: String?,
    val notes: String,
    val htmlUrl: String,
) {
    val versionName: String get() = version.toString()
}
