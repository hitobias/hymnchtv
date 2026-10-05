package org.cog.hymnchtv.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** The update service reads the lock state with only the application context (no activity after an alarm cold start). */
@RunWith(AndroidJUnit4::class)
class DeviceLockTest {
    @Test
    fun readsTheLockStateWithTheApplicationContextAlone() {
        DeviceLock.isLocked(ApplicationProvider.getApplicationContext<Application>())
    }
}
