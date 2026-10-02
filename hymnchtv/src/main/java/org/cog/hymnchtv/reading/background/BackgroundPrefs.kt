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

    /** What [applyWithTokens] put on screen: the choice really shown, its reading palette and the UI tokens derived from it. */
    data class Applied(val choice: BackgroundChoice, val palette: ReadingPalette, val tokens: UiTokens)

    /**
     * Like [applyTo], and also derives the slot's [UiTokens] from the background actually shown (a photo that cannot be
     * decoded falls back to a preset, and the tokens follow it). Each slot builds its own tokens.
     */
    @JvmStatic
    fun applyWithTokens(target: ImageView, prefs: SharedPreferences, slot: BackgroundSlot): Applied {
        val applied = applyChoiceTo(target, prefs, slot)
        return Applied(applied, BackgroundPolicy.palette(applied), UiTokens.from(BackgroundPolicy.tokenInput(applied)))
    }

    /** Shows the slot's background in [target] and returns the palette matching what is actually shown. */
    @JvmStatic
    fun applyTo(target: ImageView, prefs: SharedPreferences, slot: BackgroundSlot): ReadingPalette =
        BackgroundPolicy.palette(applyChoiceTo(target, prefs, slot))

    /** Like [applyTo] but returns the choice actually shown (palette and UI tokens are both derived from it). */
    @JvmStatic
    fun applyChoiceTo(target: ImageView, prefs: SharedPreferences, slot: BackgroundSlot): BackgroundChoice {
        val photo = photoFile(prefs)
        val choice = BackgroundPolicy.resolve(
            runCatching { prefs.getString(slot.prefKey, null) }.getOrNull(), slot, isDarkTheme(), photo != null,
        )
        return BackgroundApplier.apply(
            target, choice, prefs, photo, BackgroundPolicy.defaultFor(slot, isDarkTheme()),
        )
    }
}
