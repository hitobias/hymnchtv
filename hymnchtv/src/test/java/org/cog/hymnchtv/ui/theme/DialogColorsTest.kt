package org.cog.hymnchtv.ui.theme

import com.google.common.truth.Truth.assertWithMessage
import org.cog.hymnchtv.reading.background.Wcag
import org.junit.Test
import java.io.File

/** Spec rev 3 section 5: every dialog text colour is AA (4.5:1) on what it is drawn on, light and dark. */
class DialogColorsTest {
    private val res = File("src/main/res")

    private fun colors(dir: String): Map<String, Int> =
        Regex("""<color name="(dialog_\w+)">#([0-9A-Fa-f]{6,8})</color>""")
            .findAll(File(res, "$dir/colors_dialog.xml").readText())
            .associate { m -> m.groupValues[1] to (m.groupValues[2].let { if (it.length == 6) "FF$it" else it }).toLong(16).toInt() }

    private val pairs = listOf(
        "dialog_on_surface" to "dialog_surface",
        "dialog_on_surface_muted" to "dialog_surface",
        "dialog_on_primary" to "dialog_primary",
        "dialog_on_tonal" to "dialog_tonal",
        "dialog_on_tonal" to "dialog_surface",   // chip text never sits on a lighter plate than the card
        "dialog_primary" to "dialog_surface",    // the filled button's edge (non-text: 3:1)
    )

    private fun check(dir: String) {
        val c = colors(dir)
        assertWithMessage("$dir has all colours").that(c.keys).containsAtLeastElementsIn(pairs.flatMap { listOf(it.first, it.second) }.toSet())
        for ((fg, bg) in pairs) {
            val min = if (fg == "dialog_primary") 3.0 else 4.5
            assertWithMessage("$dir: $fg on $bg").that(Wcag.contrast(c.getValue(fg), c.getValue(bg))).isAtLeast(min)
        }
    }

    @Test fun lightColoursAreAa() = check("values")

    @Test fun darkColoursAreAa() = check("values-night")
}
