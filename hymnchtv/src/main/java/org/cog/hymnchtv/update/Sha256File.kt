package org.cog.hymnchtv.update

/**
 * Reads the hymnal-X.Y.Z.apk.sha256 release asset. Strict `shasum -a 256` text-mode format: exactly one line
 * `<64 hex>` + two spaces + the exact apk name, optionally followed by a single "\n". Anything else is rejected.
 */
object Sha256File {
    private val HEX = Regex("^[0-9a-fA-F]{64}$")

    /** @return lower-case hex for [apkName], or null when the text is not exactly that checksum line. */
    @JvmStatic
    fun parse(text: String?, apkName: String): String? {
        val line = text?.removeSuffix("\n") ?: return null
        if ('\n' in line || '\r' in line) return null
        val expectedSuffix = "  $apkName"
        if (!line.endsWith(expectedSuffix)) return null
        val hash = line.removeSuffix(expectedSuffix)
        return if (HEX.matches(hash)) hash.lowercase() else null
    }

    /** The published checksum, required; GitHub's asset digest, when present, must agree with it. */
    @JvmStatic
    fun reconcile(fileHash: String?, githubDigest: String?): String? = when {
        fileHash == null -> null
        githubDigest != null && !githubDigest.equals(fileHash, ignoreCase = true) -> null
        else -> fileHash
    }
}
