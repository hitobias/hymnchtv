package org.cog.hymnchtv.lyrics

/** Asset path rules for the pre-generated Traditional Chinese lyrics (plan A.1.9). */
object LyricsAssets {
    /** "lyrics_db_text/db1.txt" + TW -> "lyrics_db_text_hant_tw/db1.txt"; null if [simplifiedPath] has no directory. */
    @JvmStatic
    fun hantPath(simplifiedPath: String, variant: HantVariant): String? {
        val slash = simplifiedPath.indexOf('/')
        if (slash <= 0) return null
        return simplifiedPath.substring(0, slash) + variant.dirSuffix + simplifiedPath.substring(slash)
    }
}
