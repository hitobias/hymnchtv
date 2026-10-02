package org.cog.hymnchtv.notebook.settings

import android.content.Context
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import timber.log.Timber
import java.util.UUID

/**
 * NotebookPrefs backed by notebook.xml (backed up) and notebook_device.xml (not backed up).
 * Reads never throw: wrong types fall back to defaults. deviceId() may touch disk once; call it off the main thread.
 */
class SharedPrefsNotebookPrefs(context: Context) : NotebookPrefs {
    private val prefs = context.applicationContext
        .getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE)
    private val devicePrefs = context.applicationContext
        .getSharedPreferences(NotebookPrefs.DEVICE_FILE_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedDeviceId: String? = null

    override val autoRecordEnabled: Boolean
        get() = try {
            prefs.getBoolean(NotebookPrefs.KEY_AUTO_RECORD, NotebookPrefs.DEFAULT_AUTO_RECORD)
        } catch (e: ClassCastException) {
            Timber.w(e, "Illegal %s value; using default", NotebookPrefs.KEY_AUTO_RECORD)
            NotebookPrefs.DEFAULT_AUTO_RECORD
        }

    override val lastChosenOccasion: Occasion?
        get() = try {
            Occasion.fromStorage(prefs.getString(NotebookPrefs.KEY_LAST_OCCASION, null))
        } catch (e: ClassCastException) {
            Timber.w(e, "Illegal %s value; ignoring", NotebookPrefs.KEY_LAST_OCCASION)
            null
        }

    override fun setAutoRecordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(NotebookPrefs.KEY_AUTO_RECORD, enabled).apply()
    }

    override fun setLastChosenOccasion(occasion: Occasion) {
        prefs.edit().putString(NotebookPrefs.KEY_LAST_OCCASION, occasion.name).apply()
    }

    /** Generated once per install; a corrupt value is replaced. commit() so the id is durable before first use. */
    override fun deviceId(): String = cachedDeviceId ?: synchronized(this) {
        cachedDeviceId ?: (storedDeviceId() ?: newDeviceId()).also { cachedDeviceId = it }
    }

    private fun storedDeviceId(): String? = try {
        devicePrefs.getString(NotebookPrefs.KEY_DEVICE_ID, null)
            ?.takeIf { runCatching { NotebookValidation.uuid(it) }.isSuccess }
    } catch (e: ClassCastException) {
        Timber.w(e, "Illegal %s value; regenerating", NotebookPrefs.KEY_DEVICE_ID)
        null
    }

    private fun newDeviceId(): String {
        val id = UUID.randomUUID().toString()
        devicePrefs.edit().putString(NotebookPrefs.KEY_DEVICE_ID, id).commit()
        return id
    }
}
