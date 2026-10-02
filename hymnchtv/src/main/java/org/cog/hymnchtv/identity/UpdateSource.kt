package org.cog.hymnchtv.identity

/**
 * Where the in-app updater looks for releases (sub-project Z). Used only for network calls; these URLs are never
 * shown in the UI.
 */
object UpdateSource {
    const val GITHUB_REPO = "hitobias/hymnchtv"
    const val RELEASES_LATEST_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    const val RELEASE_DOWNLOAD_PREFIX = "https://github.com/$GITHUB_REPO/releases/download/"
}
