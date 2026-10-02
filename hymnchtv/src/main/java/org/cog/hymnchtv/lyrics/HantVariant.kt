package org.cog.hymnchtv.lyrics

import com.zqc.opencc.android.lib.ConversionType

/**
 * Regional Traditional Chinese lyrics variant. Each one has pre-generated assets in
 * lyrics_<type>_text<dirSuffix>/ (plan A.1.9); [conversion] is used only as a runtime fallback.
 */
enum class HantVariant(val conversion: ConversionType, val dirSuffix: String) {
    TW(ConversionType.S2TW, "_hant_tw"),
    HK(ConversionType.S2HK, "_hant_hk");

    /** Value stored in PREF_CONVERSION_TYPE. */
    val prefValue: String get() = conversion.name
}
