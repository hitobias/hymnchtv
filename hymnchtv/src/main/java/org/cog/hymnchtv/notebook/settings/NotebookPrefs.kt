package org.cog.hymnchtv.notebook.settings

import org.cog.hymnchtv.notebook.model.DeviceIdProvider
import org.cog.hymnchtv.notebook.model.Occasion

/**
 * Notebook preferences. Reads never throw.
 * - notebook.xml ([FILE_NAME]) is included in Auto Backup.
 * - notebook_device.xml ([DEVICE_FILE_NAME]) holds only the device id and is deliberately NOT backed up,
 *   so a restored phone gets a fresh id and two devices never share one.
 */
interface NotebookPrefs : DeviceIdProvider {
    val autoRecordEnabled: Boolean

    /** Occasion the user last picked by hand; null when never picked. */
    val lastChosenOccasion: Occasion?

    fun setAutoRecordEnabled(enabled: Boolean)

    fun setLastChosenOccasion(occasion: Occasion)

    companion object {
        const val FILE_NAME = "notebook"
        const val DEVICE_FILE_NAME = "notebook_device"
        const val KEY_AUTO_RECORD = "auto_record_enabled"
        const val KEY_LAST_OCCASION = "last_chosen_occasion"
        const val KEY_DEVICE_ID = "device_id"
        const val DEFAULT_AUTO_RECORD = true
    }
}
