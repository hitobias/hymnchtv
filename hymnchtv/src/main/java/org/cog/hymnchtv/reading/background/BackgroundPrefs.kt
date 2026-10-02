package org.cog.hymnchtv.reading.background

import android.content.SharedPreferences
import android.widget.ImageView
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.utils.ThemeHelper
import java.io.File

/** Reads the background settings and applies them; the single entry point for MainActivity and ContentHandler. */
object BackgroundPrefs {
    /** The user's imported photo ([PhotoBackgroundImporter]), or null if none is stored. [prefs] is kept for call-site compatibility. */
    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun photoFile(prefs: SharedPreferences): File? =
        PhotoBackgroundImporter.photoFileIn(HymnsApp.getGlobalContext().filesDir).takeIf { it.isFile }

    @JvmStatic
    fun isDarkTheme(): Boolean = ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)

    @JvmStatic
    fun resolve(prefs: SharedPreferences, slot: BackgroundSlot): BackgroundChoice = BackgroundPolicy.resolve(
        runCatching { prefs.getString(slot.prefKey, null) }.getOrNull(), slot, isDarkTheme(), photoFile(prefs) != null,
    )

    /** Shows the slot's background in [target] and returns the palette matching what is actually shown. */
    @JvmStatic
    fun applyTo(target: ImageView, prefs: SharedPreferences, slot: BackgroundSlot): ReadingPalette {
        val photo = photoFile(prefs)
        val choice = BackgroundPolicy.resolve(
            runCatching { prefs.getString(slot.prefKey, null) }.getOrNull(), slot, isDarkTheme(), photo != null,
        )
        val applied = BackgroundApplier.apply(
            target, choice, prefs, photo, BackgroundPolicy.defaultFor(slot, isDarkTheme()),
        )
        return BackgroundPolicy.palette(applied)
    }
}
