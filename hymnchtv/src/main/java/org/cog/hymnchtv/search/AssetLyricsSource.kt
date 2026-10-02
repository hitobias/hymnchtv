package org.cog.hymnchtv.search

import android.content.Context
import android.content.SharedPreferences
import org.cog.hymnchtv.ContentView
import org.cog.hymnchtv.hymn.HymnRef
import org.cog.hymnchtv.lyrics.HantVariant
import org.cog.hymnchtv.lyrics.LyricsAssets
import org.cog.hymnchtv.lyrics.LyricsLang
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy
import org.cog.hymnchtv.ui.titles.AssetHymnTitles
import timber.log.Timber
import java.io.IOException
import java.util.Locale

/**
 * [LyricsSource] over the bundled lyrics assets. [reader] returns an asset's text or null when it does not exist
 * (a missing file is normal: the youth book has 139 files, the new-hymns book has gaps).
 */
class AssetLyricsSource(
    private val reader: (String) -> String?,
    private val traditionalVariant: HantVariant?,
) : LyricsSource {

    override fun simplified(ref: HymnRef): String? = pathOf(ref)?.let(reader)

    override fun traditional(ref: HymnRef): String? {
        val variant = traditionalVariant ?: return null
        val path = pathOf(ref) ?: return null
        return LyricsAssets.hantPath(path, variant)?.let(reader)
    }

    private fun pathOf(ref: HymnRef): String? =
        AssetHymnTitles.prefixOf(ref.book)?.let { "lyrics_${it}_text/$it${ref.storedNo}.txt" }

    companion object {
        /** Reads assets through [context]; the youth book is searched through its own files only (as the old search did). */
        @JvmStatic
        fun create(context: Context, traditionalVariant: HantVariant?): AssetLyricsSource {
            val assets = context.applicationContext.assets
            return AssetLyricsSource(
                reader = { path ->
                    try {
                        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } catch (e: IOException) {
                        Timber.v("No lyrics asset %s: %s", path, e.message)
                        null
                    }
                },
                traditionalVariant = traditionalVariant,
            )
        }

        /** Follows the lyrics script the reader chose (Traditional or Simplified) and the regional variant. */
        @JvmStatic
        fun forPrefs(context: Context, prefs: SharedPreferences, locale: Locale = context.resources.configuration.locales[0]): AssetLyricsSource {
            val lang = LyricsLang.fromPref(prefs.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null))
            val variant = if (LyricsLanguagePolicy.resolveShowTraditional(lang, locale)) {
                LyricsLanguagePolicy.parseVariant(prefs.getString(ContentView.PREF_CONVERSION_TYPE, null), locale)
            } else {
                null
            }
            return create(context, variant)
        }

        /** The Traditional->Simplified table; empty (search still works for Simplified queries) when the asset is unreadable. */
        @JvmStatic
        fun loadT2s(context: Context): T2sMap = try {
            context.applicationContext.assets.open(T2sMap.ASSET_PATH).bufferedReader(Charsets.UTF_8).use { T2sMap.parse(it.readLines()) }
        } catch (e: IOException) {
            Timber.w(e, "Cannot read %s; Traditional queries will not match Simplified lyrics", T2sMap.ASSET_PATH)
            T2sMap.EMPTY
        }
    }
}
