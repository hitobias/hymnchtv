package org.cog.hymnchtv.resources

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/** Nothing requested or allowed that no code path uses. */
class ManifestHygieneTest {
    private val main = File("src/main")

    @Test
    fun unusedPermissionsAreNotRequested() {
        val manifest = File(main, "AndroidManifest.xml").readText()
        for (p in listOf("USE_FULL_SCREEN_INTENT", "REQUEST_DELETE_PACKAGES", "MANAGE_EXTERNAL_STORAGE")) {
            assertThat(manifest).doesNotContain("android.permission.$p\"")
        }
        // the updater hands apks to the installer
        assertThat(manifest).contains("android.permission.REQUEST_INSTALL_PACKAGES\"")
    }

    @Test
    fun cleartextDomainsAreBareHostNames() {
        val xml = File(main, "res/xml/network_security_config.xml").readText()
        val domains = Regex("<domain[^>]*>([^<]*)</domain>").findAll(xml).map { it.groupValues[1].trim() }.toList()
        assertThat(domains).contains("witness-lee-hymns.org")
        domains.forEach { assertThat(it).matches("[a-z0-9.-]+") }
    }

    @Test
    fun webViewHasNoLooseSettings() {
        val src = File(main, "java/org/cog/hymnchtv/webview/WebViewFragment.java").readText()
        assertThat(src).doesNotContain("addJavascriptInterface")
        assertThat(src).doesNotContain("MIXED_CONTENT_ALWAYS_ALLOW")
        assertThat(src).doesNotContain("MIXED_CONTENT_COMPATIBILITY_MODE")
        assertThat(src).contains("MIXED_CONTENT_NEVER_ALLOW")
        assertThat(src).doesNotContain("setAllowUniversalAccessFromFileURLs")
    }
}
