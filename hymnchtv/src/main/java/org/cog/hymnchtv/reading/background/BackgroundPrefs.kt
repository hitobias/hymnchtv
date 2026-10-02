package org.cog.hymnchtv.reading.background

import android.content.SharedPreferences
import android.widget.ImageView
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.persistance.FileBackend
import org.cog.hymnchtv.utils.ThemeHelper
import org.cog.hymnchtv.utils.WallPaperUtil
import java.io.File

/** Reads the background settings and applies them; the single entry point for MainActivity and ContentHandler. */
object BackgroundPrefs {
    /** The user's cropped photo (WallPaperUtil), or null if none is stored or the file is gone. */
    @JvmStatic
    fun photoFile(prefs: SharedPreferences): File? {
        val name = runCatching { prefs.getString(MainActivity.PREF_WALLPAPER, null) }.getOrNull() ?: return null
        return FileBackend.getHymnchtvStore(WallPaperUtil.DIR_WALLPAPER + name, false)?.takeIf { it.isFile }
    }

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
