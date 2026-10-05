package org.cog.hymnchtv

import android.Manifest
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.IOException

/** Grants what MainActivity asks for at launch via `pm grant` (UiAutomation.grantRuntimePermission needs API 28+). */
object TestPermissions {
    private const val STORAGE_REMOUNT_TIMEOUT_MS = 10_000L

    fun grantLaunchPermission(packageName: String) {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS
        else Manifest.permission.WRITE_EXTERNAL_STORAGE
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant $packageName $permission")
        // drain so the command has completed before the test continues
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        if (Build.VERSION.SDK_INT < 29) awaitWritableDownloads()
    }

    /**
     * API 23-28: a runtime storage grant remounts shared storage for the running process asynchronously (vold), so
     * right after `pm grant` a write to Download/ can still fail with EACCES. Wait until a probe file can be created.
     */
    private fun awaitWritableDownloads() {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val probe = File(downloads, ".hymnal-permission-probe-" + Process.myPid())
        val end = SystemClock.uptimeMillis() + STORAGE_REMOUNT_TIMEOUT_MS
        while (!canCreate(probe)) {
            check(SystemClock.uptimeMillis() < end) { "shared storage still not writable after pm grant" }
            SystemClock.sleep(100)
        }
    }

    private fun canCreate(probe: File): Boolean = try {
        probe.parentFile?.mkdirs()
        (probe.exists() || probe.createNewFile()).also { probe.delete() }
    } catch (e: IOException) {
        false
    }
}
