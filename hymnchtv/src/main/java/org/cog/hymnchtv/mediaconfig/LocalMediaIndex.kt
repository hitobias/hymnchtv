package org.cog.hymnchtv.mediaconfig

import java.io.File

/**
 * Local media lookup that lists each media directory at most once. A bulk import checks every record for an
 * already-downloaded file; listing the directory per record made that check dominate the import time.
 * Not thread-safe: use one instance per import.
 */
class LocalMediaIndex(private val resolveDir: (String) -> File?) {
    private val namesByDir = HashMap<String, Array<String>>()

    fun has(dir: String, hymnNo: Int): Boolean {
        val names = namesByDir.getOrPut(dir) { resolveDir(dir)?.list() ?: emptyArray() }
        val match = HymnFileName.matcher(hymnNo)
        return names.any(match)
    }
}
