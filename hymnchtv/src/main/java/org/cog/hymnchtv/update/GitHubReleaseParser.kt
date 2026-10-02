package org.cog.hymnchtv.update

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class ParseResult {
    data class Ok(val release: ReleaseInfo) : ParseResult()
    data class Invalid(val reason: String) : ParseResult()
}

/**
 * Parses a GitHub `releases/latest` response. Only assets whose browser_download_url lives under
 * [downloadPrefix] (this repository's release downloads) are considered; both the APK and its .sha256 are required.
 */
object GitHubReleaseParser {
    private val SHA256_DIGEST = Regex("^sha256:([0-9a-fA-F]{64})$")

    private data class Asset(val name: String, val url: String, val size: Long, val digest: String?)

    @JvmStatic
    fun parse(json: String?, downloadPrefix: String): ParseResult {
        if (json.isNullOrBlank()) return ParseResult.Invalid("empty body")
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            return ParseResult.Invalid("malformed json: ${e.message}")
        }
        return parseRelease(root, downloadPrefix)
    }

    private fun parseRelease(root: JSONObject, downloadPrefix: String): ParseResult {
        if (root.optBoolean("draft", false)) return ParseResult.Invalid("draft release")
        if (root.optBoolean("prerelease", false)) return ParseResult.Invalid("prerelease")
        val tag = root.stringOrNull("tag_name") ?: return ParseResult.Invalid("missing tag_name")
        val version = SemVer.parse(tag)
        if (version == null || version.preRelease.isNotEmpty() || tag != ReleaseConvention.tagFor(version)) {
            return ParseResult.Invalid("tag outside convention vX.Y.Z: $tag")
        }
        val assets = readAssets(root.optJSONArray("assets") ?: JSONArray(), downloadPrefix)
        val apkName = ReleaseConvention.apkName(version)
        val shaName = ReleaseConvention.sha256Name(version)
        val apk = assets.firstOrNull { it.name == apkName }
            ?: return ParseResult.Invalid("no $apkName asset under $downloadPrefix")
        val sha = assets.firstOrNull { it.name == shaName }
            ?: return ParseResult.Invalid("no $shaName asset under $downloadPrefix")
        return ParseResult.Ok(
            ReleaseInfo(
                tag = tag,
                version = version,
                apkName = apk.name,
                apkUrl = apk.url,
                apkSize = apk.size,
                sha256Url = sha.url,
                apkDigestSha256 = apk.digest?.let { SHA256_DIGEST.matchEntire(it)?.groupValues?.get(1)?.lowercase() },
                notes = root.stringOrNull("body").orEmpty(),
                htmlUrl = root.stringOrNull("html_url").orEmpty(),
            )
        )
    }

    private fun readAssets(array: JSONArray, downloadPrefix: String): List<Asset> =
        (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { toAsset(it, downloadPrefix) } }

    private fun toAsset(obj: JSONObject, downloadPrefix: String): Asset? {
        val name = obj.stringOrNull("name") ?: return null
        val url = obj.stringOrNull("browser_download_url") ?: return null
        val state = obj.stringOrNull("state")
        val uploaded = state == null || state == "uploaded"
        val ownedByRepo = url.startsWith(downloadPrefix) && url.endsWith("/$name") && '%' !in url && ".." !in url
        return if (uploaded && ownedByRepo) Asset(name, url, obj.optLong("size", 0L), obj.stringOrNull("digest")) else null
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
