package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.identity.UpdateSource
import org.junit.Test

class ReleaseResponseClassifierTest {
    private val fixture = checkNotNull(javaClass.getResource("/update/release_latest.json")).readText()

    private fun headers(vararg pairs: Pair<String, String>): (String) -> String? {
        val map = pairs.associate { it.first.lowercase() to it.second }
        return { name -> map[name.lowercase()] }
    }

    private fun classify(
        code: Int,
        body: String? = fixture,
        current: String? = "1.0.0",
        header: (String) -> String? = headers(),
    ) = ReleaseResponseClassifier.classify(code, header, body, current, UpdateSource.RELEASE_DOWNLOAD_PREFIX)

    @Test
    fun newerReleaseIsAvailable() {
        val result = classify(200)
        assertThat(result).isInstanceOf(UpdateCheckResult.Available::class.java)
        assertThat(result.release?.versionName).isEqualTo("1.1.0")
    }

    @Test
    fun sameOrNewerInstalledVersionIsUpToDate() {
        listOf("1.1.0", "1.1.1", "2.0.0").forEach {
            assertThat(classify(200, current = it)).isInstanceOf(UpdateCheckResult.UpToDate::class.java)
        }
    }

    @Test
    fun comparesVersionsNumerically() {
        assertThat(classify(200, current = "1.0.10")).isInstanceOf(UpdateCheckResult.Available::class.java)
    }

    @Test
    fun notFoundMeansNoRelease() {
        assertThat(classify(404, body = """{"message":"Not Found"}""")).isEqualTo(UpdateCheckResult.NoRelease)
    }

    @Test
    fun primaryRateLimitCarriesResetTime() {
        val result = classify(403, body = "{}", header = headers("X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "1893456000"))
        assertThat(result).isEqualTo(UpdateCheckResult.RateLimited(1893456000L))
    }

    @Test
    fun status429IsRateLimitedEvenWithoutHeaders() {
        assertThat(classify(429, body = "")).isEqualTo(UpdateCheckResult.RateLimited(null))
    }

    @Test
    fun secondaryRateLimitUsesRetryAfter() {
        assertThat(classify(403, body = "", header = headers("Retry-After" to "60"))).isEqualTo(UpdateCheckResult.RateLimited(null))
    }

    @Test
    fun otherForbiddenIsFailure() {
        assertThat(classify(403, body = "")).isEqualTo(UpdateCheckResult.Failed("HTTP 403"))
    }

    @Test
    fun serverErrorIsFailure() {
        assertThat(classify(502, body = "bad gateway")).isEqualTo(UpdateCheckResult.Failed("HTTP 502"))
    }

    @Test
    fun malformedBodyIsFailure() {
        val result = classify(200, body = "{")
        assertThat((result as UpdateCheckResult.Failed).reason).contains("malformed")
    }

    @Test
    fun unparsableInstalledVersionIsFailure() {
        assertThat(classify(200, current = "abc")).isInstanceOf(UpdateCheckResult.Failed::class.java)
        assertThat(classify(200, current = null)).isInstanceOf(UpdateCheckResult.Failed::class.java)
    }
}
