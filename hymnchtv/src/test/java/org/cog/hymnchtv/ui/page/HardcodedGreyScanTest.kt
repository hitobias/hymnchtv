package org.cog.hymnchtv.ui.page

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * Spec 5b-2: no hard-coded grey in res/layout* or res/values*; every grey comes from UiTokens (tinted by the accent).
 * A grey is a hex literal whose red, green and blue are equal (other than pure black and white), `@android:color/darker_gray`
 * and friends, or an `@color/` reference that resolves to such a hex (the palette in colors.xml is only the definition).
 *
 * Files that other screens still use in their old form are listed in [LEGACY_ALLOWLIST] with the reason; the test fails when a
 * listed file becomes clean (remove it) and when any other file gains a grey.
 */
class HardcodedGreyScanTest {
    private val res = File("src/main/res")

    private val hex = Regex("#([0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})(?![0-9a-zA-Z])")
    private val colorDef = Regex("<color\\s+name=\"([^\"]+)\"[^>]*>\\s*([^<\\s]+)\\s*</color>")
    private val colorRef = Regex("@color/([A-Za-z0-9_.]+)")
    private val platformGreys = Regex("@android:color/(darker_gray|background_dark|background_light|tertiary_text_dark|tertiary_text_light|secondary_text_dark|secondary_text_light)")

    private fun isGreyHex(literal: String): Boolean {
        val rgb = when (literal.length) {
            3 -> literal.map { "$it$it" }
            4 -> literal.drop(1).map { "$it$it" }
            6 -> literal.chunked(2)
            8 -> literal.drop(2).chunked(2)
            else -> return false
        }.map { it.toInt(16) }
        return rgb[0] == rgb[1] && rgb[1] == rgb[2] && rgb[0] != 0 && rgb[0] != 0xFF
    }

    /** Colour resource names (from the colors files of every values folder) that resolve to a grey hex. */
    private fun greyColorNames(): Set<String> {
        val defs = HashMap<String, String>()
        res.listFiles { f -> f.isDirectory && f.name.startsWith("values") }!!.flatMap { dir ->
            dir.listFiles { f -> f.name.startsWith("colors") && f.extension == "xml" }!!.toList()
        }.forEach { f -> colorDef.findAll(f.readText()).forEach { defs[it.groupValues[1]] = it.groupValues[2] } }
        fun resolve(name: String, depth: Int = 0): Boolean {
            val v = defs[name] ?: return false
            if (depth > MAX_ALIAS_DEPTH) return false
            return if (v.startsWith("@color/")) resolve(v.removePrefix("@color/"), depth + 1) else v.startsWith("#") && isGreyHex(v.drop(1))
        }
        return defs.keys.filter { resolve(it) }.toSet()
    }

    private fun scannedFiles(): List<File> =
        res.listFiles { f -> f.isDirectory && (f.name.startsWith("layout") || f.name.startsWith("values")) }!!
            .flatMap { it.listFiles { f -> f.extension == "xml" }!!.toList() }
            // The palette definitions themselves
            .filterNot { it.parentFile.name.startsWith("values") && it.name.startsWith("colors") }

    /** Greys found in [file], as readable descriptions (comments removed first). */
    private fun greysIn(file: File, greyNames: Set<String>): List<String> {
        val text = file.readText().replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val found = ArrayList<String>()
        hex.findAll(text).filter { isGreyHex(it.groupValues[1]) }.forEach { found += it.value }
        platformGreys.findAll(text).forEach { found += it.value }
        colorRef.findAll(text).filter { it.groupValues[1] in greyNames }.forEach { found += it.value }
        return found
    }

    private fun relative(f: File) = f.relativeTo(res).path

    @Test
    fun noHardcodedGreyOutsideTheAllowlist() {
        val greys = greyColorNames()
        val offenders = scannedFiles().associate { relative(it) to greysIn(it, greys) }
            .filterValues { it.isNotEmpty() }
            .filterKeys { it !in LEGACY_ALLOWLIST }
        assertWithMessage("hard-coded greys (derive them from UiTokens instead)").that(offenders).isEmpty()
    }

    @Test
    fun allowlistHasNoStaleEntries() {
        val greys = greyColorNames()
        val dirty = scannedFiles().filter { greysIn(it, greys).isNotEmpty() }.map { relative(it) }.toSet()
        assertWithMessage("allowlisted files that no longer contain a grey: remove them").that(LEGACY_ALLOWLIST.keys - dirty).isEmpty()
    }

    @Test
    fun theSettingsAndContentsPagesAreClean() {
        val greys = greyColorNames()
        val mine = listOf(
            "layout/fragment_toc.xml", "layout/toc_group_row.xml", "layout/toc_item_row.xml", "layout/page_title_bar.xml",
            "layout/preference_themed.xml", "layout/preference_category_themed.xml", "layout/preference_seekbar_themed.xml",
            "values/type_scale.xml", "values/styles_page.xml",
        )
        mine.forEach { path ->
            val f = File(res, path)
            assertThat(f.exists()).isTrue()
            assertWithMessage(path).that(greysIn(f, greys)).isEmpty()
        }
    }

    @Test
    fun theScannerRecognisesGreys() {
        assertThat(isGreyHex("888")).isTrue()
        assertThat(isGreyHex("ff9e9e9e")).isTrue()
        assertThat(isGreyHex("424242")).isTrue()
        assertThat(isGreyHex("000000")).isFalse()
        assertThat(isGreyHex("ffffffff")).isFalse()
        assertThat(isGreyHex("2B3A67")).isFalse()
    }

    private companion object {
        const val MAX_ALIAS_DEPTH = 8

        /** Old screens that the single-page-home work does not touch; each still draws its greys from the legacy palette. */
        val LEGACY_ALLOWLIST: Map<String, String> = mapOf(
            "layout/background_picker_item.xml" to "background picker thumbnail frame (darker_gray)",
            "layout/media_config.xml" to "media settings screen, not migrated in 1.2.0",
            "layout-land/media_config.xml" to "media settings screen (landscape), not migrated in 1.2.0",
            "layout/file_xfer_ui.xml" to "share-file screen, not migrated in 1.2.0",
            "layout/http_login_dialog.xml" to "login dialog (textColorWhite), not migrated in 1.2.0",
            "values/styles.xml" to "legacy button, list and dialog styles (textColorBlack/White, background_dark/light)",
        )
    }
}
