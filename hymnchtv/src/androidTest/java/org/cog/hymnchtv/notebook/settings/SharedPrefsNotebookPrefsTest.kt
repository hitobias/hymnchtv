package org.cog.hymnchtv.notebook.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedPrefsNotebookPrefsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val raw: SharedPreferences
        get() = context.getSharedPreferences(NotebookPrefs.FILE_NAME, Context.MODE_PRIVATE)
    private val rawDevice: SharedPreferences
        get() = context.getSharedPreferences(NotebookPrefs.DEVICE_FILE_NAME, Context.MODE_PRIVATE)

    private fun clear() {
        raw.edit().clear().commit()
        rawDevice.edit().clear().commit()
    }

    @Before
    fun clearBefore() = clear()

    @After
    fun clearAfter() = clear()

    @Test
    fun defaults() {
        val prefs = SharedPrefsNotebookPrefs(context)
        assertThat(prefs.autoRecordEnabled).isFalse()
        assertThat(prefs.lastChosenOccasion).isNull()
    }

    @Test
    fun persistsAcrossInstances() {
        SharedPrefsNotebookPrefs(context).apply {
            setAutoRecordEnabled(true)
            setLastChosenOccasion(Occasion.SMALL_GROUP)
        }
        val again = SharedPrefsNotebookPrefs(context)
        assertThat(again.autoRecordEnabled).isTrue()
        assertThat(again.lastChosenOccasion).isEqualTo(Occasion.SMALL_GROUP)
    }

    @Test
    fun illegalStoredValuesNeverThrow() {
        raw.edit()
            .putString(NotebookPrefs.KEY_AUTO_RECORD, "yes")
            .putInt(NotebookPrefs.KEY_LAST_OCCASION, 3)
            .commit()
        val prefs = SharedPrefsNotebookPrefs(context)
        assertThat(prefs.autoRecordEnabled).isEqualTo(NotebookPrefs.DEFAULT_AUTO_RECORD)
        assertThat(prefs.lastChosenOccasion).isNull()

        raw.edit().putString(NotebookPrefs.KEY_LAST_OCCASION, "SUNDAY").commit()
        assertThat(prefs.lastChosenOccasion).isNull()
    }

    @Test
    fun deviceIdIsACanonicalUuidStableAcrossInstancesAndKeptOutOfNotebookXml() {
        val first = SharedPrefsNotebookPrefs(context).deviceId()
        assertThat(NotebookValidation.uuid(first)).isEqualTo(first)
        assertThat(SharedPrefsNotebookPrefs(context).deviceId()).isEqualTo(first)
        assertThat(rawDevice.getString(NotebookPrefs.KEY_DEVICE_ID, null)).isEqualTo(first)
        assertThat(raw.contains(NotebookPrefs.KEY_DEVICE_ID)).isFalse()
    }

    @Test
    fun corruptDeviceIdIsReplaced() {
        rawDevice.edit().putString(NotebookPrefs.KEY_DEVICE_ID, "NOT-A-UUID").commit()
        val id = SharedPrefsNotebookPrefs(context).deviceId()
        assertThat(id).isNotEqualTo("NOT-A-UUID")
        assertThat(NotebookValidation.uuid(id)).isEqualTo(id)
    }
}
