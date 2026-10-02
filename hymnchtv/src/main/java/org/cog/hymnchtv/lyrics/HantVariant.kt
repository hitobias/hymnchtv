package org.cog.hymnchtv.lyrics

/** Regional Traditional Chinese lyrics variant with pre-generated assets in lyrics_<type>_text<dirSuffix>/ (plan A.1.9). */
enum class HantVariant(
    val dirSuffix: String,
    /** Value stored in PREF_CONVERSION_TYPE. */
    val prefValue: String,
) {
    TW("_hant_tw", "S2TW"),
    HK("_hant_hk", "S2HK"),
}
