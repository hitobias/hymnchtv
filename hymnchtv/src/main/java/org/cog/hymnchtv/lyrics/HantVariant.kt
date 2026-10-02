package org.cog.hymnchtv.lyrics

import com.zqc.opencc.android.lib.ConversionType

/**
 * Regional Traditional Chinese lyrics variant. Each one has pre-generated assets in
 * lyrics_<type>_text<dirSuffix>/ (plan A.1.9); [conversion] is used only as a runtime fallback.
 */
enum class HantVariant(
    val conversion: ConversionType,
    val dirSuffix: String,
    /** Value stored in PREF_CONVERSION_TYPE; deliberately independent of the OpenCC enum name. */
    val prefValue: String,
) {
    TW(ConversionType.S2TW, "_hant_tw", "S2TW"),
    HK(ConversionType.S2HK, "_hant_hk", "S2HK"),
}
