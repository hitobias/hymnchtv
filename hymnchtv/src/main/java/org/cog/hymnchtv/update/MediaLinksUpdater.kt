package org.cog.hymnchtv.update

import android.content.Context
import android.content.SharedPreferences
import android.database.SQLException
import androidx.annotation.WorkerThread
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.mediaconfig.MediaConfig
import timber.log.Timber
import java.io.IOException

/**
 * Imports the media links bundled in the APK (assets/url_import.txt) on first run and whenever
 * [MediaLinksPolicy.BUNDLED_VERSION] increases. No network. Relies on the static, synchronous
 * MediaConfig.importUrlRecords(InputStream, boolean) (contract with sub-project B).
 */
object MediaLinksUpdater {
    @JvmStatic
    @WorkerThread
    @Synchronized
    fun importBundledIfNeeded(context: Context) {
        val installed = installedVersion(context)
        if (!MediaLinksPolicy.shouldImport(installed, MediaLinksPolicy.BUNDLED_VERSION)) return
        try {
            val result = context.assets.open(MediaConfig.ASSET_URL_IMPORT_FILE).use { MediaConfig.importUrlRecords(it, false) }
            if (!MediaLinksPolicy.shouldRecordVersion(result.total)) {
                // Read errors are swallowed by MediaConfig and surface as an empty result: retry next start.
                Timber.w("Bundled media links import saw no records; version not recorded")
                return
            }
            prefs(context).edit().putInt(MediaConfig.PREF_VERSION_URL, MediaLinksPolicy.BUNDLED_VERSION).apply()
            Timber.i("Imported bundled media links v%s (was %s)", MediaLinksPolicy.BUNDLED_VERSION, installed)
        } catch (e: IOException) {
            Timber.w(e, "Bundled media links import failed")
        } catch (e: SQLException) {
            // The batch import throws instead of logging per record (B-9a); it already rolled back. The stored
            // version stays unchanged, so the import is retried on the next start.
            Timber.e(e, "Bundled media links import failed")
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(MainActivity.PREF_SETTINGS, Context.MODE_PRIVATE)

    /** An illegal stored value counts as "never imported" instead of crashing. */
    private fun installedVersion(context: Context): Int = try {
        prefs(context).getInt(MediaConfig.PREF_VERSION_URL, MediaConfig.URL_IMPORT_VERSION)
    } catch (e: ClassCastException) {
        MediaConfig.URL_IMPORT_VERSION
    }
}
