package org.cog.hymnchtv.persistance

import java.io.File
import java.io.IOException

/**
 * Decides which file paths of a shared uri may be used in place (no copy). The `_data` column and file:// paths come
 * from the SENDING app, so only system providers are believed, and even then the canonical path must stay on external
 * storage and out of this app's private data (a "../" path would otherwise get copied into public Download/hymnal).
 */
object PathTrust {
    private val TRUSTED_AUTHORITIES = setOf(
        "media",
        "downloads", // DownloadManager.getUriForDownloadedFile: content://downloads/all_downloads/<id>
        "com.android.providers.media.documents",
        "com.android.providers.downloads.documents",
        "com.android.externalstorage.documents",
    )

    @JvmStatic
    fun isTrustedAuthority(authority: String?): Boolean = authority != null && authority in TRUSTED_AUTHORITIES

    /** True if the canonical [path] is inside one of [allowedRoots] and inside none of [forbidden]. */
    @JvmStatic
    fun isAcceptable(path: String?, forbidden: List<File>, allowedRoots: List<File>): Boolean {
        if (path.isNullOrEmpty()) return false
        val canonical = canonical(File(path)) ?: return false
        if (forbidden.any { contains(it, canonical) }) return false
        return allowedRoots.any { contains(it, canonical) }
    }

    /** True if the canonical [path] is inside the canonical [dir] (a sibling with the same prefix is not). */
    @JvmStatic
    fun isUnder(path: String?, dir: File?): Boolean {
        if (path.isNullOrEmpty() || dir == null) return false
        val canonical = canonical(File(path)) ?: return false
        return contains(dir, canonical)
    }

    private fun contains(dir: File, canonicalFile: File): Boolean {
        val root = canonical(dir) ?: return false
        return canonicalFile.path.startsWith(root.path + File.separator)
    }

    private fun canonical(f: File): File? = try {
        f.canonicalFile
    } catch (e: IOException) {
        null
    }
}
