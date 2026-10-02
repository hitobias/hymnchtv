package org.cog.hymnchtv.concurrent

import android.os.Handler
import android.os.Looper
import android.os.Process
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The one background executor shared by Java code (plan B.2). It is a single thread on purpose: this wave's
 * tasks are short (ms to ~1 s), and running them in order keeps DB writes and the importers' static
 * counters consistent without extra locks. Kotlin code uses lifecycleScope + Dispatchers.IO instead;
 * never mix both styles in one class.
 */
object AppExecutors {
    private const val THREAD_NAME = "hymn-io"

    private val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, THREAD_NAME).apply { isDaemon = true }
    }

    /** Main-thread handler for posting results back to the UI. */
    @JvmField
    val MAIN: Handler = Handler(Looper.getMainLooper())

    /**
     * Runs [task] on the shared IO thread. Callers handle their expected errors themselves; anything
     * unexpected is logged with [tag] so one bad task cannot kill the process or the thread.
     */
    @JvmStatic
    fun io(tag: String, task: Runnable) {
        ioExecutor.execute {
            try {
                task.run()
            } catch (e: RuntimeException) {
                Timber.e(e, "Background task '%s' failed", tag)
            }
        }
    }
}
