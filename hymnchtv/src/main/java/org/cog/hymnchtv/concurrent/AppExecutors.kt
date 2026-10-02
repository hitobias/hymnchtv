package org.cog.hymnchtv.concurrent

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Process
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.Supplier

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

    /**
     * Runs [work] (a database read or write) on the shared IO thread, then hands its result to [onMain] on the
     * main thread, but only if [isAlive] still holds at that moment: a destroyed screen never gets a callback.
     * A failing [work] is logged and delivers nothing, so callers catch the errors they want to report.
     */
    @JvmStatic
    fun <T> ioThenMain(tag: String, isAlive: BooleanSupplier, work: Supplier<T>, onMain: Consumer<T>) {
        io(tag) {
            val result = work.get()
            MAIN.post { if (isAlive.asBoolean) onMain.accept(result) }
        }
    }

    /**
     * [ioThenMain] that also reports a failing [work]: [onFailure] runs on the main thread (under the same [isAlive]
     * rule), so a caller that set a "request pending" flag always gets to reset it.
     */
    @JvmStatic
    fun <T> ioThenMain(tag: String, isAlive: BooleanSupplier, work: Supplier<T>, onMain: Consumer<T>, onFailure: Runnable) {
        io(tag) {
            val result = try {
                work.get()
            } catch (e: RuntimeException) {
                Timber.e(e, "Background task '%s' failed", tag)
                MAIN.post { if (isAlive.asBoolean) onFailure.run() }
                return@io
            }
            MAIN.post { if (isAlive.asBoolean) onMain.accept(result) }
        }
    }

    /** [ioThenMain] with a failure callback for an Activity. */
    @JvmStatic
    fun <T> ioThenMain(tag: String, activity: Activity, work: Supplier<T>, onMain: Consumer<T>, onFailure: Runnable) {
        ioThenMain(tag, { !activity.isFinishing && !activity.isDestroyed }, work, onMain, onFailure)
    }

    /** [ioThenMain] for an Activity: the result is delivered only while it is neither finishing nor destroyed. */
    @JvmStatic
    fun <T> ioThenMain(tag: String, activity: Activity, work: Supplier<T>, onMain: Consumer<T>) {
        ioThenMain(tag, { !activity.isFinishing && !activity.isDestroyed }, work, onMain)
    }
}
