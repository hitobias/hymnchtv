package org.cog.hymnchtv.ui.picker

import android.content.Context
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.AppExecutors
import org.cog.hymnchtv.hymn.EnglishXRef
import timber.log.Timber
import java.io.IOException

/** Loads the English cross-reference once, off the main thread, and keeps it for the life of the process. */
object EnglishXRefStore {
    @Volatile
    private var cached: EnglishXRef? = null

    private var loading = false
    private var failureReported = false
    private val waiting = ArrayList<() -> Unit>()

    /** What is loaded so far; [EnglishXRef.EMPTY] until [ensure] has finished. */
    fun current(): EnglishXRef = cached ?: EnglishXRef.EMPTY

    /** Calls [onLoaded] on the main thread once the table is available (at once when it already is). Main thread only. */
    fun ensure(context: Context, onLoaded: () -> Unit) {
        if (cached != null) {
            onLoaded()
            return
        }
        waiting += onLoaded
        if (loading) return
        loading = true
        val assets = context.applicationContext.assets
        AppExecutors.io("english-xref") {
            val xref = try {
                EnglishXRef.fromAssets { assets.open(EnglishXRef.ASSET) }
            } catch (e: IOException) {
                Timber.w(e, "Cannot read %s", EnglishXRef.ASSET)
                null
            }
            AppExecutors.MAIN.post {
                loading = false
                if (xref == null) {
                    if (!failureReported) {
                        failureReported = true
                        HymnsApp.showToastMessage(R.string.in_development)
                    }
                    waiting.clear()
                } else {
                    cached = xref
                    val callbacks = waiting.toList()
                    waiting.clear()
                    callbacks.forEach { it() }
                }
            }
        }
    }
}
