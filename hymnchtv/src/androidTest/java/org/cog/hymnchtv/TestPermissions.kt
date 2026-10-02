package org.cog.hymnchtv

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry

/** Grants what MainActivity asks for at launch via `pm grant` (UiAutomation.grantRuntimePermission needs API 28+). */
object TestPermissions {
    fun grantLaunchPermission(packageName: String) {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS
        else Manifest.permission.WRITE_EXTERNAL_STORAGE
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant $packageName $permission")
        // drain so the command has completed before the test continues
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
}
