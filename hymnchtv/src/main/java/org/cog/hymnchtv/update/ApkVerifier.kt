package org.cog.hymnchtv.update

import android.content.Context
import androidx.annotation.WorkerThread
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Copies a finished download into private storage (files/updates/) while hashing it, then checks the private copy.
 * Only the private copy is ever installed, so nothing can swap the file between verification and installation.
 */
object ApkVerifier {
    const val UPDATES_DIR = "updates"

    data class Result(val check: ApkCheck, val stagedApk: File?)

    @JvmStatic
    fun updatesDir(context: Context): File = File(context.filesDir, UPDATES_DIR)

    @JvmStatic
    @WorkerThread
    fun verifyAndStage(context: Context, downloaded: File, release: ReleaseInfo, expectedSha256: String): Result {
        val staged = stage(context, downloaded, release)
        downloaded.delete()
        if (staged == null) {
            Timber.w("Update %s verification: %s", release.tag, ApkCheck.STAGING_FAILED)
            return Result(ApkCheck.STAGING_FAILED, null)
        }
        val part = staged.first
        var check = ApkTrust.check(
            release.version, expectedSha256, staged.second,
            SigningCerts.installedApp(context), SigningCerts.archive(context, part),
        )
        var verified: File? = null
        if (check == ApkCheck.OK) {
            // Only a fully verified file ever gets the final name that staged() and the installer look for.
            val finalFile = File(part.parentFile, release.apkName)
            if (part.renameTo(finalFile)) verified = finalFile else check = ApkCheck.STAGING_FAILED
        }
        if (verified == null && !part.delete()) Timber.w("Cannot delete rejected %s", part)
        Timber.i("Update %s verification: %s", release.tag, check)
        return Result(check, verified)
    }

    /**
     * Copies [downloaded] into files/updates/ while hashing it. Every I/O step is checked.
     * @return the staged file and its SHA-256, or null when any step failed (STAGING_FAILED).
     */
    private fun stage(context: Context, downloaded: File, release: ReleaseInfo): Pair<File, String>? {
        val dir = updatesDir(context)
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val old = dir.listFiles() ?: return null
        if (old.any { !it.delete() }) return null // keep at most one staged update
        val staged = File(dir, partName(release.apkName))
        return try {
            val sha = copyHashing(downloaded, staged)
            if (staged.length() != downloaded.length() || staged.length() == 0L) {
                staged.delete()
                null
            } else {
                staged to sha
            }
        } catch (e: IOException) {
            Timber.w(e, "Cannot stage %s", downloaded)
            staged.delete()
            null
        }
    }

    /** Unverified downloads live under this name (still ending in .apk so the package parser accepts it). */
    @JvmStatic
    fun partName(apkName: String): String = "$apkName.part.apk"

    /** SHA-256 (lower-case hex) of [file], read in full. Used to re-check a staged apk right before installing. */
    @JvmStatic
    @WorkerThread
    fun sha256Of(file: File): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: IOException) {
        null
    }

    /** A previously verified apk for [release], if it is still staged. */
    @JvmStatic
    fun staged(context: Context, release: ReleaseInfo): File? =
        File(updatesDir(context), release.apkName).takeIf { it.isFile }

    private fun copyHashing(from: File, to: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(from).use { input ->
            FileOutputStream(to).use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
                output.fd.sync() // write errors surface here as IOException, not later
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
