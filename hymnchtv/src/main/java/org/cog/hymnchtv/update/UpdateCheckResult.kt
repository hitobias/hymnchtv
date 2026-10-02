package org.cog.hymnchtv.update

/** Outcome of one update check against GitHub Releases. */
sealed class UpdateCheckResult {
    /** The parsed release, when the check got one. */
    open val release: ReleaseInfo? get() = null

    data class Available(override val release: ReleaseInfo) : UpdateCheckResult()
    data class UpToDate(override val release: ReleaseInfo) : UpdateCheckResult()
    data object NoRelease : UpdateCheckResult()
    data class RateLimited(val resetEpochSeconds: Long?) : UpdateCheckResult()
    data class NetworkError(val message: String) : UpdateCheckResult()
    data class Failed(val reason: String) : UpdateCheckResult()
}
