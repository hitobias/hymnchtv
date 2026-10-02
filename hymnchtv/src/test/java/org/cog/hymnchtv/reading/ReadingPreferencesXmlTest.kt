package org.cog.hymnchtv.reading

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.lyrics.HantVariant
import org.cog.hymnchtv.lyrics.LyricsLang
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.reading.background.BackgroundSlot
import org.cog.hymnchtv.reading.background.PhotoBackground
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Keeps reading_preferences.xml and reading_arrays.xml in step with the keys, enums and defaults the code reads. */
class ReadingPreferencesXmlTest {
    private val res = File(checkNotNull(System.getProperty("hymnchtv.resDir")) { "hymnchtv.resDir not set" })

    private fun parse(path: String): Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, path))

    private fun prefs(): Map<String, Element> {
        val nodes = parse("xml/reading_preferences.xml").getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
            .filter { it.hasAttribute("app:key") }
            .associateBy { it.getAttribute("app:key") }
    }

    private fun array(name: String): List<String> {
        val arrays = parse("values/reading_arrays.xml").getElementsByTagName("string-array")
        val node = (0 until arrays.length).map { arrays.item(it) as Element }.single { it.getAttribute("name") == name }
        val items = node.getElementsByTagName("item")
        return (0 until items.length).map { items.item(it).textContent.trim() }
    }

    @Test
    fun keysAreTheOnesTheCodeReads() {
        assertThat(prefs().keys).containsExactly(
            ReadingPrefKeys.DISPLAY_MODE,
            LyricsLanguagePolicy.PREF_LYRICS_DEFAULT,
            "ConversionType", // ContentView.PREF_CONVERSION_TYPE (Java constant, not visible to JVM tests)
            ReadingPrefKeys.LYRICS_FONT_SIZE,
            ReadingPrefKeys.LYRICS_FONT,
            BackgroundSlot.MAIN.prefKey,
            BackgroundSlot.LYRICS.prefKey,
            PhotoBackground.PREF_DIM,
            PhotoBackground.PREF_BLUR,
            ReadingPrefKeys.PAGE_ANIMATION,
            ReadingPrefKeys.MENU_SHOW,
            ReadingPrefKeys.KEEP_SCREEN_ON,
        )
    }

    @Test
    fun openingTheScreenWritesNothing() {
        // androidx.preference persists a preference's defaultValue the first time the screen opens, which would freeze
        // defaults that ReadingPrefs computes (e.g. PageAnimation off on low-RAM phones). So: no defaultValue anywhere,
        // and every preference is non-persistent; the fragment writes only when the user changes a value.
        prefs().forEach { (key, element) ->
            assertWithMessage("$key defaultValue").that(element.hasAttribute("app:defaultValue")).isFalse()
            assertWithMessage("$key persistent").that(element.getAttribute("app:persistent")).isEqualTo("false")
        }
    }

    @Test
    fun seekBarRangesMatchTheCode() {
        val p = prefs()
        assertThat(p.getValue(PhotoBackground.PREF_DIM).getAttribute("app:min")).isEqualTo(PhotoBackground.DIM_MIN.toString())
        assertThat(p.getValue(PhotoBackground.PREF_DIM).getAttribute("android:max")).isEqualTo(PhotoBackground.DIM_MAX.toString())
        assertThat(p.getValue(PhotoBackground.PREF_BLUR).getAttribute("app:min")).isEqualTo("0")
        assertThat(p.getValue(PhotoBackground.PREF_BLUR).getAttribute("android:max")).isEqualTo(PhotoBackground.BLUR_MAX.toString())
    }

    @Test
    fun listValuesAreTheEnumNames() {
        assertThat(array("display_mode_values")).containsExactlyElementsIn(DisplayMode.entries.map { it.name }).inOrder()
        assertThat(array("font_size_values")).containsExactlyElementsIn(LyricsFontSize.entries.map { it.name }).inOrder()
        assertThat(array("lyrics_font_values")).containsExactlyElementsIn(LyricsFont.entries.map { it.name }).inOrder()
        assertThat(array("lyrics_lang_values")).containsExactlyElementsIn(LyricsLang.entries.map { it.name }).inOrder()
        assertThat(array("conversion_values")).containsExactlyElementsIn(HantVariant.entries.map { it.prefValue }).inOrder()
    }

    @Test
    fun everyListHasOneLabelPerValue() {
        listOf("display_mode", "font_size", "lyrics_font", "lyrics_lang", "conversion").forEach {
            assertThat(array("${it}_entries")).hasSize(array("${it}_values").size)
        }
    }
}
