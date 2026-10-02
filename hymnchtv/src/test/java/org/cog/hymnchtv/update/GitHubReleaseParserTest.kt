package org.cog.hymnchtv.update

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.identity.UpdateSource
import org.json.JSONObject
import org.junit.Test

class GitHubReleaseParserTest {
    private val fixture = checkNotNull(javaClass.getResource("/update/release_latest.json")).readText()
    private val prefix = UpdateSource.RELEASE_DOWNLOAD_PREFIX
    private val apk = "hymnal-1.1.0.apk"
    private val sha = "hymnal-1.1.0.apk.sha256"

    private fun parseOk(json: String = fixture): ReleaseInfo {
        val result = GitHubReleaseParser.parse(json, prefix)
        assertThat(result).isInstanceOf(ParseResult.Ok::class.java)
        return (result as ParseResult.Ok).release
    }

    private fun reasonOf(json: String?): String {
        val result = GitHubReleaseParser.parse(json, prefix)
        assertThat(result).isInstanceOf(ParseResult.Invalid::class.java)
        return (result as ParseResult.Invalid).reason
    }

    /** The fixture with [block] applied to a fresh copy. */
    private fun mutated(block: JSONObject.() -> Unit): String = JSONObject(fixture).apply(block).toString()

    private fun JSONObject.asset(name: String): JSONObject {
        val assets = getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.getString("name") == name) return asset
        }
        error("fixture has no asset $name")
    }

    @Test
    fun parsesCapturedFixture() {
        val release = parseOk()
        assertThat(release.tag).isEqualTo("v1.1.0")
        assertThat(release.version).isEqualTo(SemVer(1, 1, 0))
        assertThat(release.versionName).isEqualTo("1.1.0")
        assertThat(release.apkName).isEqualTo(apk)
        assertThat(release.apkUrl).isEqualTo("${prefix}v1.1.0/$apk")
        assertThat(release.apkSize).isEqualTo(121034567L)
        assertThat(release.sha256Url).isEqualTo("${prefix}v1.1.0/$sha")
        assertThat(release.apkDigestSha256).isEqualTo("3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd")
        assertThat(release.notes).contains("介面新增繁體中文")
    }

    @Test
    fun uppercaseDigestIsNormalised() {
        val json = mutated { asset(apk).put("digest", "sha256:3095E51C68BB82552F8AAF30FA5A77A7FE6786C35952B984181F28D923ACECCD") }
        assertThat(parseOk(json).apkDigestSha256).isEqualTo("3095e51c68bb82552f8aaf30fa5a77a7fe6786c35952b984181f28d923aceccd")
    }

    @Test
    fun missingOrMalformedDigestGivesNull() {
        assertThat(parseOk(mutated { asset(apk).remove("digest") }).apkDigestSha256).isNull()
        assertThat(parseOk(mutated { asset(apk).put("digest", JSONObject.NULL) }).apkDigestSha256).isNull()
        assertThat(parseOk(mutated { asset(apk).put("digest", "md5:abc") }).apkDigestSha256).isNull()
    }

    @Test
    fun nullBodyGivesEmptyNotes() {
        assertThat(parseOk(mutated { put("body", JSONObject.NULL) }).notes).isEmpty()
        assertThat(parseOk(mutated { remove("body") }).notes).isEmpty()
    }

    @Test
    fun rejectsDraftAndPrerelease() {
        assertThat(reasonOf(mutated { put("draft", true) })).contains("draft")
        assertThat(reasonOf(mutated { put("prerelease", true) })).contains("prerelease")
    }

    @Test
    fun rejectsTagsOutsideConvention() {
        listOf("1.1.0", "V1.1.0", "v1.1", "v1.1.0-rc.1", "v1.1.0+build", "release-1.1.0").forEach { tag ->
            assertThat(reasonOf(mutated { put("tag_name", tag) })).contains("tag")
        }
        assertThat(reasonOf(mutated { remove("tag_name") })).contains("tag_name")
    }

    @Test
    fun rejectsReleaseWithoutMatchingApk() {
        listOf("hymnal-1.0.0.apk", "hymnchtv-release.apk", "hymnal-1.1.0.APK").forEach { name ->
            val json = mutated { asset(apk).put("name", name).put("browser_download_url", "${prefix}v1.1.0/$name") }
            assertThat(reasonOf(json)).contains(apk)
        }
    }

    @Test
    fun ignoresAssetsHostedOutsideTheRepo() {
        listOf(
            "https://evil.example/$apk",
            "https://github.com/someone-else/hymnchtv/releases/download/v1.1.0/$apk",
            "${prefix}v1.1.0/../../../evil/$apk",
        ).forEach { url ->
            assertThat(reasonOf(mutated { asset(apk).put("browser_download_url", url) })).contains(apk)
        }
    }

    @Test
    fun ignoresAssetNotYetUploaded() {
        assertThat(reasonOf(mutated { asset(apk).put("state", "new") })).contains(apk)
    }

    @Test
    fun requiresChecksumAsset() {
        assertThat(reasonOf(mutated { asset(sha).put("name", "notes.txt").put("browser_download_url", "${prefix}v1.1.0/notes.txt") }))
            .contains(sha)
        assertThat(reasonOf(mutated { asset(sha).put("browser_download_url", "https://evil.example/$sha") })).contains(sha)
    }

    @Test
    fun rejectsEmptyAndMalformedJson() {
        assertThat(reasonOf(null)).contains("empty")
        assertThat(reasonOf("   ")).contains("empty")
        assertThat(reasonOf("{")).contains("malformed")
        assertThat(reasonOf("[]")).contains("malformed")
    }
}
