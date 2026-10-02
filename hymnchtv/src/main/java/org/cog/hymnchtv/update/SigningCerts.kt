package org.cog.hymnchtv.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import org.cog.hymnchtv.BuildConfig
import java.io.File
import java.security.MessageDigest

/**
 * Signing certificates as SHA-256 hex. API 28+: GET_SIGNING_CERTIFICATES (SigningInfo; history is original first,
 * current last, per the platform docs). Below 28: GET_SIGNATURES.
 */
object SigningCerts {

    @JvmStatic
    fun installedApp(context: Context): InstalledApp {
        val info = try {
            context.packageManager.getPackageInfo(context.packageName, flags())
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        return InstalledApp(context.packageName, BuildConfig.VERSION_CODE.toLong(), info?.let { signers(it).first }.orEmpty())
    }

    @JvmStatic
    fun archive(context: Context, apk: File): ArchiveApk {
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags())
            ?: return ArchiveApk(null, null, null, null)
        // Known workaround for archive PackageInfo: point applicationInfo at the file before reading from it.
        info.applicationInfo?.let {
            it.sourceDir = apk.absolutePath
            it.publicSourceDir = apk.absolutePath
        }
        val (current, history) = signers(info)
        return ArchiveApk(
            info.packageName,
            info.versionName,
            PackageInfoCompat.getLongVersionCode(info),
            current.takeIf { it.isNotEmpty() },
            history,
        )
    }

    @Suppress("DEPRECATION")
    private fun flags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
        else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Pair<Set<String>, List<String>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet<String>() to emptyList()
            val current = signing.apkContentsSigners.orEmpty().map(::sha256).toSet()
            val history = if (signing.hasMultipleSigners()) emptyList()
            else signing.signingCertificateHistory.orEmpty().map(::sha256)
            return current to history
        }
        return info.signatures.orEmpty().map(::sha256).toSet() to emptyList()
    }

    private fun sha256(signature: Signature): String =
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
}
