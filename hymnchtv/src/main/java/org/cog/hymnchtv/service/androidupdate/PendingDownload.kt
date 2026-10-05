package org.cog.hymnchtv.service.androidupdate

import org.cog.hymnchtv.update.ReleaseConvention
import org.cog.hymnchtv.update.ReleaseInfo
import org.cog.hymnchtv.update.SemVer

/**
 * The update download in progress, saved next to its DownloadManager id (1.6.0) so a process that dies mid-download can
 * still verify the finished file at the next start instead of deleting it. Holds only what verification needs.
 */
data class PendingDownload(val id: Long, val tag: String, val apkName: String, val sha256: String) {
    fun encode(): String = listOf(id.toString(), tag, apkName, sha256).joinToString(SEP)

    /** The release as far as ApkVerifier and UpdateNotifier use it (no urls, size or notes). */
    fun toRelease(): ReleaseInfo = ReleaseInfo(
        tag = tag,
        version = requireNotNull(SemVer.parse(tag)) { "bad tag $tag" },
        apkName = apkName,
        apkUrl = "",
        apkSize = 0L,
        sha256Url = "",
        apkDigestSha256 = null,
        notes = "",
        htmlUrl = "",
    )

    companion object {
        /** Key in UpdateServiceImpl's "store" preferences. */
        const val PREF_KEY = "apk_pending"
        private const val SEP = "|"
        private val SHA256 = Regex("^[0-9a-f]{64}$")

        /** @return the record, or null for anything that is not exactly a record [encode] wrote */
        @JvmStatic
        fun decode(text: String?): PendingDownload? {
            val parts = text?.split(SEP) ?: return null
            if (parts.size != 4) return null
            val id = parts[0].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            val version = SemVer.parse(parts[1]) ?: return null
            if (parts[1] != ReleaseConvention.tagFor(version)) return null
            if (parts[2] != ReleaseConvention.apkName(version)) return null
            if (!SHA256.matches(parts[3])) return null
            return PendingDownload(id, parts[1], parts[2], parts[3])
        }
    }
}
