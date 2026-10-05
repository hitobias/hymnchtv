package org.cog.hymnchtv.ui.theme

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * Spec 5b-1 / 10: the screens this release touches take every text size from the five TextAppearance.Hymnal levels,
 * never from a literal `textSize`, a `@dimen` or the old SmallFont/TinyFont styles. Screens it does not touch are listed in
 * [LEGACY] so a new literal size cannot slip into any other layout unnoticed. The user's lyrics body size is exempt.
 */
class TypeScaleScanTest {
    private val res = File("src/main/res")

    /** Old screens (about, dialogs, media config, notebook, background picker) that keep their own sizes for now. */
    private val legacy = setOf(
        "layout/background_picker_item.xml", "layout/fragment_myhymns.xml",
        "layout/media_config.xml", "layout-land/media_config.xml", "layout/rich_text_editor.xml",
        "layout/custom_dialog_wv.xml", "layout/file_xfer_ui.xml", "layout/media_record_delete.xml",
        "layout/action_bar.xml", "layout/http_login_dialog.xml",
    )

    /** Ids whose size the reader sets (lyrics body) */
    private val userSized = setOf("lyrics_simplified", "lyrics_traditional")

    private val touched = listOf(
        "layout/hymn_picker.xml", "layout-land/hymn_picker.xml", "layout/picker_part_books.xml", "layout/picker_part_keypad.xml",
        "layout/picker_part_open.xml", "layout/picker_part_preview.xml", "layout/picker_part_recent.xml", "layout/item_recent.xml",
        "layout/fragment_search.xml", "layout/fragment_history.xml", "layout/row_search_result.xml", "layout/row_history.xml",
        "layout/row_history_header.xml", "layout/media_player_audio_ui.xml", "layout/media_select.xml", "layout/content_main.xml",
        "layout/activity_main_host.xml", "layout/fragment_home.xml", "layout/alert_dialog.xml", "layout/about.xml",
    )

    private val sizeAttr = Regex("""android:textSize\s*=\s*"[^"]*"""")
    private val oldStyles = Regex("""style\s*=\s*"@style/(SmallFont|TinyFont|MediumFont|LargeFont)"""")

    /** The offending attributes of one layout, element by element, as "id: attribute". */
    private fun offences(file: File): List<String> =
        file.readText().split("<").drop(1).flatMap { element ->
            val id = Regex("""android:id="@\+id/(\w+)"""").find(element)?.groupValues?.get(1)
            if (id in userSized) emptyList() else (sizeAttr.findAll(element) + oldStyles.findAll(element)).map { "${id ?: "?"}: ${it.value}" }.toList()
        }

    @Test fun touchedLayoutsUseOnlyTextAppearances() {
        for (path in touched) {
            val file = File(res, path)
            assertThat(file.exists()).isTrue()
            assertThat(offences(file)).isEmpty()
        }
    }

    @Test fun everyOtherLayoutWithALiteralSizeIsOnTheLegacyList() {
        val dirs = res.listFiles { f -> f.isDirectory && f.name.startsWith("layout") }.orEmpty()
        for (dir in dirs) {
            for (file in dir.listFiles { f -> f.extension == "xml" }.orEmpty()) {
                val path = "${dir.name}/${file.name}"
                if (path in touched) continue
                if (offences(file).isNotEmpty()) assertThat(legacy).contains(path)
            }
        }
    }

    @Test fun legacyListHoldsNoTouchedScreen() {
        assertThat(legacy.intersect(touched.toSet())).isEmpty()
        legacy.forEach { assertThat(File(res, it).exists()).isTrue() }
    }

    @Test fun touchedStylesDeclareNoLiteralSize() {
        val home = File(res, "values/styles_home.xml").readText()
        assertThat(Regex("""name="android:textSize"""").containsMatchIn(home)).isFalse()
        val styles = File(res, "values/styles.xml").readText()
        for (name in listOf("PlayerSourceButton")) {
            val block = Regex("""<style name="$name".*?</style>""", RegexOption.DOT_MATCHES_ALL).find(styles)!!.value
            assertThat(block).doesNotContain("android:textSize")
        }
    }

    @Test fun theFiveLevelsAreTheOnlyDeclaredSizes() {
        val scale = File(res, "values/type_scale.xml").readText()
        val sizes = Regex("""<dimen name="type_(\w+)">""").findAll(scale).map { it.groupValues[1] }.toSet()
        assertThat(sizes).containsExactly("display", "title", "heading", "body", "caption")
    }
}
