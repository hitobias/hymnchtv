package org.cog.hymnchtv.service.androidupdate

import java.util.function.BooleanSupplier

/** When the daily update check may open the update dialog by itself. */
object UpdatePromptPolicy {
    /**
     * Once per process, only in the foreground on an unlocked device. The lock state is read last: when the alarm
     * cold-starts the process (API 24/25) no activity exists, and reading it first crashed the service.
     */
    @JvmStatic
    fun shouldPrompt(alreadyNotified: Boolean, isForeground: Boolean, isDeviceLocked: BooleanSupplier): Boolean =
        !alreadyNotified && isForeground && !isDeviceLocked.asBoolean
}
