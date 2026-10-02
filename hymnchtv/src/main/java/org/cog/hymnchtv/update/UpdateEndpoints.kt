package org.cog.hymnchtv.update

import org.cog.hymnchtv.identity.UpdateSource

/** Where the updater looks for releases and which download URLs it trusts. */
data class UpdateEndpoints(val apiUrl: String, val downloadPrefix: String) {
    companion object {
        @JvmField
        val PRODUCTION = UpdateEndpoints(UpdateSource.RELEASES_LATEST_API, UpdateSource.RELEASE_DOWNLOAD_PREFIX)

        /** Debug-only override file in the app's private files dir (pushed with adb for Task 13). */
        const val OVERRIDE_FILE = "update_endpoint.properties"

        /** Release builds always use [PRODUCTION]; debug builds honour `api=` / optional `prefix=` lines. */
        @JvmStatic
        fun resolve(isDebug: Boolean, overrideText: String?): UpdateEndpoints {
            if (!isDebug || overrideText == null) return PRODUCTION
            val values = overrideText.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }
                .toMap()
            val api = values["api"].orEmpty()
            if (api.isEmpty()) return PRODUCTION
            return UpdateEndpoints(api, values["prefix"].orEmpty().ifEmpty { UpdateSource.RELEASE_DOWNLOAD_PREFIX })
        }
    }
}
