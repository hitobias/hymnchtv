package org.cog.hymnchtv.update

/** Media links ship only inside the APK (assets/url_import.txt); a new list means a new app release. */
object MediaLinksPolicy {
    /** Version of the bundled url_import.txt. Increase it whenever that file changes. */
    const val BUNDLED_VERSION = 1

    @JvmStatic
    fun shouldImport(installedVersion: Int, bundledVersion: Int): Boolean = bundledVersion > installedVersion
}
