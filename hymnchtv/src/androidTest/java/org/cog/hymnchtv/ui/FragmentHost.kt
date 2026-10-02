package org.cog.hymnchtv.ui

import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.test.platform.app.InstrumentationRegistry
import org.cog.hymnchtv.TestPermissions

/**
 * Shows a tab fragment on its own inside the app's existing [AppCompatActivity] (the real theme and resources),
 * because the tab host (HOST1) does not exist yet. The activity's old content is removed so ids are unambiguous.
 */
object FragmentHost {
    /** MainActivity requests media permissions at launch; the dialog would take focus from the activity (as in SmokeFlowTest). */
    fun grantLaunchPermissions(pkg: String) {
        TestPermissions.grantLaunchPermission(pkg)
        if (Build.VERSION.SDK_INT >= 33) {
            listOf("AUDIO", "IMAGES", "VIDEO").forEach { grant(pkg, "android.permission.READ_MEDIA_$it") }
        }
    }

    private fun grant(pkg: String, permission: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant $pkg $permission")
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    fun <F : Fragment> show(activity: AppCompatActivity, fragment: F): F {
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        content.removeAllViews()
        val container = FragmentContainerView(activity).apply { id = View.generateViewId() }
        content.addView(container, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        activity.supportFragmentManager.beginTransaction().replace(container.id, fragment).commitNow()
        return fragment
    }

    /** Polls [check] on the instrumentation thread until it passes or [timeoutMs] runs out (for results posted from a background thread). */
    fun eventually(timeoutMs: Long = 3000, check: () -> Unit) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                return check()
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > deadline) throw e
                Thread.sleep(50)
            }
        }
    }
}
