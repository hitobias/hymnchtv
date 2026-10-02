package org.cog.hymnchtv.update

enum class InstallStep { ALLOW_UNKNOWN_SOURCES, LAUNCH_INSTALLER }

/** Android 8.0+ needs the per-app "install unknown apps" permission before the installer can be used. */
object InstallGate {
    @JvmStatic
    fun nextStep(sdkInt: Int, canRequestPackageInstalls: Boolean): InstallStep =
        if (sdkInt >= 26 && !canRequestPackageInstalls) InstallStep.ALLOW_UNKNOWN_SOURCES else InstallStep.LAUNCH_INSTALLER
}
