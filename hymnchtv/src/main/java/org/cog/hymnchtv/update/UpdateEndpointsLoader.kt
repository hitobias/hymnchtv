package org.cog.hymnchtv.update

import android.content.Context
import org.cog.hymnchtv.BuildConfig
import java.io.File
import java.io.IOException

/** Reads the debug-only endpoint override from the app's private files dir on every check. */
object UpdateEndpointsLoader {
    @JvmStatic
    fun current(context: Context): UpdateEndpoints {
        if (!BuildConfig.DEBUG) return UpdateEndpoints.PRODUCTION
        val file = File(context.filesDir, UpdateEndpoints.OVERRIDE_FILE)
        val text = try {
            if (file.isFile) file.readText() else null
        } catch (e: IOException) {
            null
        }
        return UpdateEndpoints.resolve(true, text)
    }
}
