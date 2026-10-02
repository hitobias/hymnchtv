package org.cog.hymnchtv.update

import android.os.Looper
import androidx.annotation.WorkerThread
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException

/** GitHub Releases access. Network I/O: never call on the main thread. */
class GitHubReleaseClient(private val client: OkHttpClient) {

    @WorkerThread
    fun fetchLatest(endpoints: UpdateEndpoints, currentVersionName: String?): UpdateCheckResult {
        requireWorkerThread()
        val request = try {
            Request.Builder()
                .url(endpoints.apiUrl)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "Hymnal-Android/${currentVersionName ?: "unknown"}")
                .build()
        } catch (e: IllegalArgumentException) {
            return UpdateCheckResult.Failed("bad update url")
        }
        return try {
            client.newCall(request).execute().use { response ->
                ReleaseResponseClassifier.classify(
                    response.code,
                    { name -> response.header(name) },
                    response.peekBody(MAX_JSON_BYTES).string(),
                    currentVersionName,
                    endpoints.downloadPrefix,
                )
            }
        } catch (e: IOException) {
            UpdateCheckResult.NetworkError("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Downloads the release's .sha256 asset; null when missing, unreadable or inconsistent with GitHub's digest. */
    @WorkerThread
    fun fetchExpectedSha256(release: ReleaseInfo): String? {
        requireWorkerThread()
        return try {
            client.newCall(Request.Builder().url(release.sha256Url).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Checksum download failed: HTTP %s", response.code)
                    return null
                }
                val fileHash = Sha256File.parse(response.peekBody(MAX_SHA_BYTES).string(), release.apkName)
                Sha256File.reconcile(fileHash, release.apkDigestSha256)
            }
        } catch (e: IOException) {
            Timber.w(e, "Checksum download failed")
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun requireWorkerThread() {
        check(Looper.myLooper() != Looper.getMainLooper()) { "Update network I/O must run off the main thread" }
    }

    private companion object {
        const val MAX_JSON_BYTES = 1_000_000L
        const val MAX_SHA_BYTES = 4_096L
    }
}
