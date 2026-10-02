package org.cog.hymnchtv.update

/** Release naming rules shared by the app updater and tools/release.sh. Keep both in sync. */
object ReleaseConvention {
    private val APK_NAME = Regex("""^hymnal-(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)\.apk$""")

    @JvmStatic
    fun tagFor(version: SemVer): String = "v$version"

    @JvmStatic
    fun apkName(version: SemVer): String = "hymnal-$version.apk"

    @JvmStatic
    fun sha256Name(version: SemVer): String = apkName(version) + ".sha256"

    /** True only for a bare file name like hymnal-1.1.0.apk (no path, no suffix). */
    @JvmStatic
    fun isApkName(name: String?): Boolean = name != null && APK_NAME.matches(name)
}
