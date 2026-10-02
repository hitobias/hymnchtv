package org.cog.hymnchtv.ui.settings

import android.graphics.Color

/** The eight home-screen text colours the old main menu offered, by the stable name the settings list stores. */
object HomeTextColors {
    val NAMES = listOf("red", "blue", "white", "grey", "cyan", "yellow", "green", "black")

    /** @param black what "black" means (the app's softer grey900) @return null for an unknown [name] */
    fun colorOf(name: String, black: Int): Int? = when (name) {
        "red" -> Color.RED
        "blue" -> Color.BLUE
        "white" -> Color.WHITE
        "grey" -> Color.GRAY
        "cyan" -> Color.CYAN
        "yellow" -> Color.YELLOW
        "green" -> Color.GREEN
        "black" -> black
        else -> null
    }

    /** @return the choice name for a stored [color], or null when it is not one of the eight */
    fun nameOf(color: Int, black: Int): String? = NAMES.firstOrNull { colorOf(it, black) == color }
}
