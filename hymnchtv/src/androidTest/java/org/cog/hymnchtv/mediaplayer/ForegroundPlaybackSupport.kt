package org.cog.hymnchtv.mediaplayer

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Shared by the foreground playback tests: a silent wav, the service's foreground state, notification keys, shell. */
object ForegroundPlaybackSupport {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context get() = instrumentation.targetContext

    /** 60 s of 8 kHz 16-bit mono silence: it cannot finish by itself during a test. */
    fun silence(file: File): Uri {
        val rate = 8000
        val data = ByteArray(rate * 2 * 60)
        val out = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + data.size); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1); putInt(rate); putInt(rate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(data.size)
        }
        out.write(header.array())
        out.write(data)
        file.writeBytes(out.toByteArray())
        return file.toUri()
    }

    /** true / false while AudioBgService runs (in the foreground or not); null when it is not running. */
    @Suppress("DEPRECATION")
    fun serviceForeground(): Boolean? =
        ctx.getSystemService(ActivityManager::class.java).getRunningServices(100)
            .firstOrNull { it.service.className == AudioBgService::class.java.name }?.foreground

    fun awaitForeground(expected: Boolean?, what: String, timeoutMs: Long = 10_000) {
        val end = SystemClock.uptimeMillis() + timeoutMs
        while (serviceForeground() != expected) {
            check(SystemClock.uptimeMillis() < end) { "timed out waiting for $what (now ${serviceForeground()})" }
            SystemClock.sleep(100)
        }
    }

    /** What a tap on a notification key sends. */
    fun notificationKey(action: String) {
        ctx.startService(Intent(ctx, AudioBgService::class.java).setAction(action))
    }

    fun playbackNotification(): android.app.Notification? =
        ctx.getSystemService(NotificationManager::class.java).activeNotifications
            .firstOrNull { it.id == PlaybackNotification.ID }?.notification

    fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
}
