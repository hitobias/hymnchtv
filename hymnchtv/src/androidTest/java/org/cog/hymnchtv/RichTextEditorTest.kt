package org.cog.hymnchtv

import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.SystemClock
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.editor.EditorStore
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** The export editor keeps no text in the saved state (API 24 crash) and saves the CSV exactly as edited. */
@RunWith(AndroidJUnit4::class)
class RichTextEditorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext
    private val file = File(ctx.cacheDir, "editor-test-export.csv")

    @After
    fun tearDown() {
        file.delete()
        EditorStore.clearDraft(ctx.cacheDir, file.absolutePath)
    }

    private fun launch(): ActivityScenario<RichTextEditor> = ActivityScenario.launch(
        Intent(ctx, RichTextEditor::class.java).putExtra(RichTextEditor.ATTR_FILE_URI, file.absolutePath))

    /** Writes ASCII CSV lines until the file holds at least [bytes] bytes, without keeping the text; returns its length. */
    private fun writeCsv(bytes: Int): Int {
        var written = 0
        file.bufferedWriter().use { out ->
            var i = 0
            while (written < bytes) {
                i++
                val line = "hymn_db,$i,false,HYMN_MEDIA,https://example.org/media/$i,\n"
                out.write(line)
                written += line.length
            }
        }
        return written
    }

    private fun ActivityScenario<RichTextEditor>.editorLength(): Int {
        var n = -1
        onActivity { n = it.findViewById<EditText>(R.id.editor).length() }
        return n
    }

    private fun ActivityScenario<RichTextEditor>.editorText(): String {
        var text = ""
        onActivity { text = it.findViewById<EditText>(R.id.editor).text.toString() }
        return text
    }

    private fun ActivityScenario<RichTextEditor>.awaitLength(expected: Int, timeoutMs: Long = 30_000): Int {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var n = editorLength()
        while (n != expected && SystemClock.elapsedRealtime() < deadline) {
            Thread.sleep(100)
            n = editorLength()
        }
        return n
    }

    private fun ActivityScenario<RichTextEditor>.append(text: String) =
        onActivity { it.findViewById<EditText>(R.id.editor).append(text) }

    /**
     * Acceptance test of the API 24 crash. The crash is the Binder transaction of the saved state when the screen is
     * stopped (TransactionTooLargeException over about 1 MB); ActivityScenario's stop/recreate does not go through that
     * transaction, so the criterion is the size of the saved state itself, written to a Parcel the way the system does.
     */
    @Test
    fun aDirtyTwoMegabyteDocumentLeavesTheSavedStateSmall() {
        val length = writeCsv(2 * 1024 * 1024)
        launch().use { scenario ->
            assertThat(scenario.awaitLength(length)).isEqualTo(length)
            scenario.append("hymn_bb,1,false,HYMN_MEDIA,https://example.org/x,\n")
            var size = Int.MAX_VALUE
            scenario.onActivity { activity ->
                val state = Bundle()
                instrumentation.callActivityOnSaveInstanceState(activity, state)
                val parcel = Parcel.obtain()
                try {
                    state.writeToParcel(parcel, 0)
                    size = parcel.dataSize()
                } finally {
                    parcel.recycle()
                }
            }
            assertThat(size).isLessThan(50_000)
            // the unsaved text went to the draft file instead
            assertThat(EditorStore.draftFile(ctx.cacheDir, file.absolutePath).length()).isAtLeast(length.toLong())
        }
    }

    /** Restore coverage only (not the crash criterion, see above): a large file is shown again after recreate. */
    @Test
    fun aLargeFileIsShownAgainAfterStopAndRecreate() {
        val length = writeCsv(2 * 1024 * 1024)
        launch().use { scenario ->
            assertThat(scenario.awaitLength(length)).isEqualTo(length)
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.recreate()
            assertThat(scenario.awaitLength(length)).isEqualTo(length)
        }
    }

    @Test
    fun unsavedChangesComeBackAfterRecreate() {
        val first = "hymn_db,1,false,HYMN_MEDIA,https://example.org/1,\n"
        val added = "hymn_db,2,false,HYMN_MEDIA,https://example.org/2,\n"
        file.writeText(first)
        launch().use { scenario ->
            assertThat(scenario.awaitLength(first.length)).isEqualTo(first.length)
            scenario.append(added)
            scenario.recreate()
            assertThat(scenario.awaitLength(first.length + added.length)).isEqualTo(first.length + added.length)
            assertThat(scenario.editorText()).isEqualTo(first + added)
            var dirty = false
            scenario.onActivity { dirty = it.hasUnsavedChanges() }
            assertThat(dirty).isTrue()
        }
    }

    @Test
    fun savingKeepsEveryLineBreakOfTheCsv() {
        val csv = "hymn_db,1,false,HYMN_MEDIA,https://example.org/1,\n\nhymn_bb,2,false,HYMN_MEDIA,https://example.org/2,\n"
        val added = "hymn_er,3,false,HYMN_MEDIA,https://example.org/3,\n"
        file.writeText(csv)
        launch().use { scenario ->
            assertThat(scenario.awaitLength(csv.length)).isEqualTo(csv.length)
            scenario.append(added)
            scenario.onActivity { it.findViewById<View>(R.id.saveButton).performClick() }
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (file.readText() != csv + added && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            assertThat(file.readText()).isEqualTo(csv + added)
        }
    }
}
