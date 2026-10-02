package org.cog.hymnchtv.perf

import android.os.StrictMode

/** Debug builds only (plan B.3-1): log, never crash, on main-thread disk/network access and leaked DB objects. */
object DebugStrictMode {
    @JvmStatic
    fun install() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
