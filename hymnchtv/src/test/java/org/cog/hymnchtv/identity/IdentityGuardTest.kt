package org.cog.hymnchtv.identity

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Shipped content must not point at the original author's endpoints or contact data, and the UI must not show any
 * web link to this repository (sub-project Z). Scans sources, resources, text assets, Gradle files, AboutLibraries
 * config and the README.
 */
class IdentityGuardTest {
    private val srcMain = File(checkNotNull(System.getProperty("hymnchtv.assetsDir"))).parentFile
    private val module = srcMain.parentFile.parentFile
    private val repoRoot = module.parentFile

    private val upstream = listOf(
        "cmeng-git.github.io", "raw.githubusercontent.com/cmeng-git", "github.com/cmeng-git",
        "cmeng.gm@", "gmail.com", "play.google.com", "createPackageContext(\"org.cog.hymnchtv\"",
    )
    private val uiLinks = listOf("github.com/hitobias", "#readme")
    private val textAsset = setOf("txt", "json", "xml", "html", "htm", "properties", "csv")

    private fun scan(files: Sequence<File>, forbidden: List<String>): List<String> =
        files.flatMap { file ->
            file.readLines().asSequence().mapIndexedNotNull { index, line ->
                forbidden.firstOrNull { it in line }?.let { "${file.relativeTo(repoRoot)}:${index + 1}: $it" }
            }
        }.toList()

    private fun tree(dir: File, extensions: Set<String>) =
        dir.walkTopDown().filter { it.isFile && it.extension.lowercase() in extensions }

    @Test
    fun shippedCodeResourcesAndBuildFilesHaveNoUpstreamEndpointsOrContacts() {
        val files = tree(File(srcMain, "java"), setOf("java", "kt")) +
            tree(File(srcMain, "res"), setOf("xml")) +
            sequenceOf(File(srcMain, "AndroidManifest.xml")) +
            tree(File(srcMain, "assets"), textAsset) +
            tree(File(module, "aboutlibraries-config"), setOf("json")) +
            sequenceOf(File(module, "build.gradle"), File(repoRoot, "build.gradle"), File(repoRoot, "settings.gradle"))
        assertThat(scan(files, upstream)).isEmpty()
    }

    @Test
    fun uiResourcesAndAssetsShowNoRepositoryLinks() {
        val files = tree(File(srcMain, "res"), setOf("xml")) + tree(File(srcMain, "assets"), textAsset)
        assertThat(scan(files, uiLinks)).isEmpty()
    }

    @Test
    fun readmeHasNoUpstreamSiteOrStoreLinks() {
        assertThat(scan(sequenceOf(File(repoRoot, "README.md")), listOf("cmeng-git.github.io", "play.google.com", "youtube.com/watch"))).isEmpty()
    }
}
