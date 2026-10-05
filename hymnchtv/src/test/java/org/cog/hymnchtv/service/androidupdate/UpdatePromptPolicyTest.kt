package org.cog.hymnchtv.service.androidupdate

import com.google.common.truth.Truth.assertThat
import java.util.function.BooleanSupplier
import org.junit.Test

class UpdatePromptPolicyTest {
    /** Reading the lock state needed an activity: when the alarm cold-starts the process (API 24/25) there is none. */
    private val lockMustNotBeRead = BooleanSupplier { throw AssertionError("lock state read") }

    @Test
    fun inTheBackgroundTheLockStateIsNeverRead() {
        assertThat(UpdatePromptPolicy.shouldPrompt(false, false, lockMustNotBeRead)).isFalse()
    }

    @Test
    fun onceNotifiedTheLockStateIsNeverRead() {
        assertThat(UpdatePromptPolicy.shouldPrompt(true, true, lockMustNotBeRead)).isFalse()
    }

    @Test
    fun foregroundAndUnlockedPrompts() {
        assertThat(UpdatePromptPolicy.shouldPrompt(false, true) { false }).isTrue()
    }

    @Test
    fun foregroundButLockedDoesNotPrompt() {
        assertThat(UpdatePromptPolicy.shouldPrompt(false, true) { true }).isFalse()
    }
}
