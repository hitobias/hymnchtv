package org.cog.hymnchtv.update

/** Turns an HTTP response from `releases/latest` into an [UpdateCheckResult]. Pure; no I/O. */
object ReleaseResponseClassifier {
    @JvmStatic
    fun classify(
        code: Int,
        header: (String) -> String?,
        body: String?,
        currentVersionName: String?,
        downloadPrefix: String,
    ): UpdateCheckResult {
        when {
            code == 200 -> Unit
            code == 404 -> return UpdateCheckResult.NoRelease
            isRateLimited(code, header) ->
                return UpdateCheckResult.RateLimited(header("x-ratelimit-reset")?.trim()?.toLongOrNull())
            else -> return UpdateCheckResult.Failed("HTTP $code")
        }
        val current = SemVer.parse(currentVersionName)
            ?: return UpdateCheckResult.Failed("unparsable installed version: $currentVersionName")
        return when (val parsed = GitHubReleaseParser.parse(body, downloadPrefix)) {
            is ParseResult.Invalid -> UpdateCheckResult.Failed(parsed.reason)
            is ParseResult.Ok ->
                if (parsed.release.version > current) UpdateCheckResult.Available(parsed.release)
                else UpdateCheckResult.UpToDate(parsed.release)
        }
    }

    /** GitHub signals primary limits with remaining=0 and secondary limits with Retry-After (403 or 429). */
    private fun isRateLimited(code: Int, header: (String) -> String?): Boolean =
        code == 429 || (code == 403 && (header("x-ratelimit-remaining")?.trim() == "0" || header("retry-after") != null))
}
