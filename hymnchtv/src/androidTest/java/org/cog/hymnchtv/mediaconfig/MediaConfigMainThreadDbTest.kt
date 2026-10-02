package org.cog.hymnchtv.mediaconfig

import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.HymnsApp
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MainThreadDbSupport.awaitUntil
import org.cog.hymnchtv.MainThreadDbSupport.launchMainActivityIfNeeded
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.R
import org.cog.hymnchtv.concurrent.IoGate
import org.cog.hymnchtv.persistance.DatabaseBackend
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Media config: the entry check, save, overwrite and delete reach the database on AppExecutors.io only. The app
 * database refuses main-thread queries, so any regression throws; the io gate additionally proves the screen
 * does not wait for the database.
 */
@RunWith(AndroidJUnit4::class)
class MediaConfigMainThreadDbTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val backend = DatabaseBackend.getInstance(ctx)
    private val hymnNo = 7
    private val link = "https://example.org/mt-test-7"
    private var mainScenario: ActivityScenario<MainActivity>? = null

    private fun key() = MediaRecord(MainActivity.HYMN_DB, hymnNo, false, MediaType.HYMN_MEDIA)

    private fun stored(): MediaRecord? = key().takeIf { backend.getMediaRecord(it, true) }

    @Before
    fun setUp() {
        mainScenario = launchMainActivityIfNeeded()
        backend.deleteMediaRecord(key())
    }

    @After
    fun tearDown() {
        backend.deleteMediaRecord(key())
        mainScenario?.close()
    }

    private fun ActivityScenario<MediaConfig>.fillEntry(uri: String?) = onActivity {
        it.findViewById<EditText>(R.id.hymnNo).setText(hymnNo.toString())
        if (uri != null) it.findViewById<EditText>(R.id.mediaUri).setText(uri)
    }

    private fun ActivityScenario<MediaConfig>.mediaUriText(): String {
        var text = ""
        onActivity { text = it.findViewById<EditText>(R.id.mediaUri).text.toString() }
        return text
    }

    @Test
    fun entryCheckFillsTheStoredLinkAfterTheBackgroundRead() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, hymnNo, false, MediaType.HYMN_MEDIA, link, null))
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            val gate = IoGate.close()
            try {
                // the fu box toggled twice ends in a check of hymn #7 (the last answer wins)
                scenario.onActivity {
                    it.findViewById<EditText>(R.id.hymnNo).setText(hymnNo.toString())
                    it.findViewById<CheckBox>(R.id.cbFu).isChecked = true
                    it.findViewById<CheckBox>(R.id.cbFu).isChecked = false
                }
                Thread.sleep(500)
                assertThat(scenario.mediaUriText()).isEmpty()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { scenario.mediaUriText() == link }).isTrue()
        }
    }

    @Test
    fun addSavesTheRecordInTheBackgroundAndIgnoresADoubleTap() {
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            scenario.fillEntry(link)
            val gate = IoGate.close()
            try {
                scenario.onActivity {
                    val add = it.findViewById<Button>(R.id.button_add)
                    add.performClick()
                    add.performClick() // a second tap while the first lookup is pending is ignored
                }
                Thread.sleep(500)
                assertThat(stored()).isNull()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { stored() != null }).isTrue()
            assertThat(stored()!!.mediaUri).isEqualTo(link)
        }
    }

    @Test
    fun addOverwritesAnExistingRecordAfterConfirmation() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, hymnNo, false, MediaType.HYMN_MEDIA, "https://example.org/old", null))
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            scenario.fillEntry(link)
            scenario.onActivity { it.findViewById<Button>(R.id.button_add).performClick() }
            // the overwrite check runs on the IO thread; the confirmation dialog appears when it has answered (slower on API 34)
            assertThat(awaitUntil { runCatching { onView(withId(R.id.okButton)).check(matches(isDisplayed())) }.isSuccess }).isTrue()
            onView(withId(R.id.okButton)).perform(click())
            assertThat(awaitUntil { stored()?.mediaUri == link }).isTrue()
        }
    }

    @Test
    fun deleteRemovesTheRecordInTheBackground() {
        backend.storeMediaRecord(MediaRecord(MainActivity.HYMN_DB, hymnNo, false, MediaType.HYMN_MEDIA, link, null))
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            scenario.fillEntry(null)
            scenario.onActivity { it.findViewById<Button>(R.id.button_delete).performClick() }
            val gate = IoGate.close()
            try {
                onView(allOf(withId(R.id.okButton), withText(R.string.delete), isAssignableFrom(Button::class.java))).perform(click())
                Thread.sleep(500)
                assertThat(stored()).isNotNull()
            } finally {
                gate.release()
            }
            assertThat(awaitUntil { stored() == null }).isTrue()
        }
    }

    @Test
    fun deleteConfirmedWhileADbRequestIsPendingShowsAnInProgressNotice() {
        ActivityScenario.launch(MediaConfig::class.java).use { scenario ->
            scenario.fillEntry(link)
            val gate = IoGate.close()
            try {
                // the overwrite check of the add button is queued behind the gate: the screen is busy
                scenario.onActivity { it.findViewById<Button>(R.id.button_add).performClick() }
                scenario.onActivity { it.findViewById<Button>(R.id.button_delete).performClick() }
                onView(allOf(withId(R.id.okButton), withText(R.string.delete), isAssignableFrom(Button::class.java))).perform(click())
                assertThat(awaitUntil {
                    HymnsApp.getLastToastMessage() == InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.in_progress)
                }).isTrue()
            } finally {
                gate.release()
            }
            // the add went through (no overwrite dialog: there was no record) and the busy delete tap did not touch it
            assertThat(awaitUntil { stored()?.mediaUri == link }).isTrue()
        }
    }
}
