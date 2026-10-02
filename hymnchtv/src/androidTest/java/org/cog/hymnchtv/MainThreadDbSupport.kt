package org.cog.hymnchtv

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry

/** Shared by the tests that prove a screen reads and writes the database off the main thread. */
object MainThreadDbSupport {
    /** Polls [condition] on the test thread until it holds or [timeoutMs] passes; returns the last answer. */
    fun awaitUntil(timeoutMs: Long = 10_000, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return condition()
    }

    /**
     * API 24-28: FileBackend.getHymnchtvStore() reads MainActivity.getInstance(), which is null unless MainActivity
     * exists. Returns the scenario to close, or null on later APIs.
     */
    fun launchMainActivityIfNeeded(): ActivityScenario<MainActivity>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        TestPermissions.grantLaunchPermission(packageName)
        return ActivityScenario.launch(MainActivity::class.java)
    }
}
