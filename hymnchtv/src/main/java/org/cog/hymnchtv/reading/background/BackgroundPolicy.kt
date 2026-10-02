package org.cog.hymnchtv.reading.background

/**
 * Colours used on top of a background (plan A2).
 * [paperColor]: what white score paper becomes on dark backgrounds.
 * [backdropColor]: ARGB of the panel drawn behind text (0 = none); photos need it because their pixels are unknown.
 */
data class ReadingPalette(
    val textColor: Int,
    val accentColor: Int,
    val isDark: Boolean,
    val paperColor: Int,
    val backdropColor: Int = 0,
)

/** The two independently chosen backgrounds; [prefKey] stores a preset id or BackgroundPolicy.PHOTO. */
enum class BackgroundSlot(val prefKey: String, val lightDefault: BackgroundPreset) {
    MAIN("MainBackground", BackgroundPreset.DAWN),
    LYRICS("LyricsBackground", BackgroundPreset.XUAN);

    companion object {
        /** Never throws; unknown names mean LYRICS. */
        @JvmStatic
        fun fromName(name: String?): BackgroundSlot = entries.firstOrNull { it.name == name } ?: LYRICS
    }
}

sealed interface BackgroundChoice {
    data class Preset(val preset: BackgroundPreset) : BackgroundChoice
    data object Photo : BackgroundChoice
}

object BackgroundPolicy {
    /** Stored value meaning "the user's own photo" (the file is PhotoBackgroundImporter.photoFileIn). */
    const val PHOTO = "photo"

    @JvmField
    val DARK_DEFAULT = BackgroundPreset.NIGHTREAD

    /**
     * A photo can be any colour, so text sits on an 85 % #1e1e1e panel; PhotoPaletteTest proves AA contrast
     * for every photo pixel from black to white. Dim and blur are cosmetic only.
     */
    @JvmField
    val PHOTO_PALETTE = ReadingPalette(
        textColor = 0xFFF2EFE8.toInt(),
        accentColor = 0xFFE3B77A.toInt(),
        isDark = true,
        paperColor = 0xFF1E1E1E.toInt(),
        backdropColor = 0xD91E1E1E.toInt(),
    )

    @JvmStatic
    fun defaultFor(slot: BackgroundSlot, darkTheme: Boolean): BackgroundPreset =
        if (darkTheme) DARK_DEFAULT else slot.lightDefault

    /** The stored choice if still valid; otherwise the slot default (also when a chosen photo has disappeared). */
    @JvmStatic
    fun resolve(stored: String?, slot: BackgroundSlot, darkTheme: Boolean, photoAvailable: Boolean): BackgroundChoice {
        if (stored == PHOTO && photoAvailable) return BackgroundChoice.Photo
        return BackgroundChoice.Preset(BackgroundPreset.fromId(stored) ?: defaultFor(slot, darkTheme))
    }

    @JvmStatic
    fun palette(choice: BackgroundChoice): ReadingPalette = when (choice) {
        BackgroundChoice.Photo -> PHOTO_PALETTE
        is BackgroundChoice.Preset -> choice.preset.let {
            ReadingPalette(it.textColor, it.accentColor, it.isDark, it.baseColor)
        }
    }

    @JvmStatic
    fun prefValue(choice: BackgroundChoice): String = when (choice) {
        BackgroundChoice.Photo -> PHOTO
        is BackgroundChoice.Preset -> choice.preset.id
    }
}
