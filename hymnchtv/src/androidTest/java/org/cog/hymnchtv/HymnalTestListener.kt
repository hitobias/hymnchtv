package org.cog.hymnchtv

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.notification.RunListener

/**
 * Registered for every instrumented run by the meta-data "listener" in src/androidTest/AndroidManifest.xml (no custom
 * runner, so `am instrument ... androidx.test.runner.AndroidJUnitRunner` keeps working). Before the first test it marks
 * this version's change log as seen, so the 15 s dialog of MainActivity never covers a slow test.
 */
class HymnalTestListener : RunListener() {
    override fun testRunStarted(description: Description?) {
        ChangeLogSeen.mark(InstrumentationRegistry.getInstrumentation().targetContext)
        started = true
    }

    companion object {
        /** True once [testRunStarted] ran in this process; proves the manifest registration works. */
        @Volatile
        var started = false
            private set
    }
}
