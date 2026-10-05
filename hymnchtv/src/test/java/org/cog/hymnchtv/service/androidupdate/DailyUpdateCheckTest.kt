package org.cog.hymnchtv.service.androidupdate

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class DailyUpdateCheckTest {
    private class FakeSource(
        val latest: Boolean = false,
        val foreground: Boolean = true,
        val locked: Boolean = false,
    ) : DailyUpdateCheck.Source {
        var dialogs = 0
        var lockReads = 0
        var checks = 0
        override fun check(): String? {
            checks++
            return if (latest) null else "1.7.0"
        }
        override fun isForeground() = foreground
        override fun isLocked(): Boolean {
            lockReads++
            return locked
        }
        override fun openUpdateDialog() {
            dialogs++
        }
    }

    private val shown = mutableListOf<String>()
    private val notifier = DailyUpdateCheck.Notifier { shown += it; true }

    @Test
    fun upToDateDoesNothing() {
        val source = FakeSource(latest = true)
        DailyUpdateCheck(source, notifier, AtomicBoolean()).run()
        assertThat(shown).isEmpty()
        assertThat(source.dialogs).isEqualTo(0)
    }

    @Test
    fun aNewerReleaseIsNotifiedEveryTimeButTheDialogOpensOncePerProcess() {
        val source = FakeSource()
        val check = DailyUpdateCheck(source, notifier, AtomicBoolean())
        check.run()
        check.run()
        assertThat(shown).containsExactly("1.7.0", "1.7.0")
        assertThat(source.dialogs).isEqualTo(1)
    }

    @Test
    fun inTheBackgroundOnlyTheNotificationAndTheLockIsNeverRead() {
        val source = FakeSource(foreground = false)
        DailyUpdateCheck(source, notifier, AtomicBoolean()).run()
        assertThat(shown).hasSize(1)
        assertThat(source.dialogs).isEqualTo(0)
        assertThat(source.lockReads).isEqualTo(0)
    }

    @Test
    fun aLockedDeviceGetsNoDialog() {
        val source = FakeSource(locked = true)
        DailyUpdateCheck(source, notifier, AtomicBoolean()).run()
        assertThat(source.dialogs).isEqualTo(0)
    }

    @Test
    fun oneRunFetchesTheReleaseOnceEvenWhenItOpensTheDialog() {
        val source = FakeSource()
        DailyUpdateCheck(source, notifier, AtomicBoolean()).run()
        assertThat(source.checks).isEqualTo(1)
        assertThat(source.dialogs).isEqualTo(1)
    }

    @Test
    fun aRefusedNotificationStillLetsTheDialogOpen() {
        val source = FakeSource()
        DailyUpdateCheck(source, { false }, AtomicBoolean()).run()
        assertThat(source.dialogs).isEqualTo(1)
    }
}
