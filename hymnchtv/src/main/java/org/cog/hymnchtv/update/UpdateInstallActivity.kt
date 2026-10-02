package org.cog.hymnchtv.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import org.cog.hymnchtv.R
import org.cog.hymnchtv.persistance.FileBackend
import timber.log.Timber
import java.io.File

/**
 * Invisible trampoline opened by the user (notification tap or the About "Update" button): asks for the
 * "install unknown apps" permission on Android 8.0+ when needed, then hands the verified private copy to the
 * system installer through this app's FileProvider.
 */
class UpdateInstallActivity : Activity() {
    private var askedForUnknownSources = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askedForUnknownSources = savedInstanceState?.getBoolean(STATE_ASKED, false) ?: false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_ASKED, askedForUnknownSources)
    }

    override fun onResume() {
        super.onResume()
        val apk = stagedApk()
        if (apk == null) {
            toast(R.string.update_install_missing)
            finish()
            return
        }
        val canRequest = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()
        when (InstallGate.nextStep(Build.VERSION.SDK_INT, canRequest)) {
            InstallStep.ALLOW_UNKNOWN_SOURCES -> {
                if (askedForUnknownSources) {
                    toast(R.string.update_install_needs_permission)
                    finish()
                    return
                }
                askedForUnknownSources = true
                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            }
            InstallStep.LAUNCH_INSTALLER -> {
                launchInstaller(apk)
                finish()
            }
        }
    }

    private fun stagedApk(): File? {
        val name = intent.getStringExtra(EXTRA_APK_NAME)
        if (!ReleaseConvention.isApkName(name)) return null
        return File(ApkVerifier.updatesDir(this), checkNotNull(name)).takeIf { it.isFile }
    }

    private fun launchInstaller(apk: File) {
        try {
            val uri = FileBackend.getUriForFile(this, apk)
            startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, APK_MIME_TYPE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
            UpdateNotifier.cancel(this)
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "No package installer")
            toast(R.string.update_install_missing)
        } catch (e: SecurityException) {
            Timber.e(e, "No FileProvider uri for %s", apk)
            toast(R.string.update_install_missing)
        }
    }

    private fun toast(resId: Int) = Toast.makeText(this, resId, Toast.LENGTH_LONG).show()

    companion object {
        const val EXTRA_APK_NAME = "apk_name"
        private const val STATE_ASKED = "asked_unknown_sources"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

        @JvmStatic
        fun intent(context: Context, apkName: String): Intent =
            Intent(context, UpdateInstallActivity::class.java)
                .putExtra(EXTRA_APK_NAME, apkName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
