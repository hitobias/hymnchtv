package org.cog.hymnchtv.ui.notes

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.HymnTypes
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoteDraftFilesTest {
    @get:Rule val folder = TemporaryFolder()

    private val files by lazy { NoteDraftFiles(folder.newFolder("note-drafts")) }

    @Test fun aLongDraftComesBackVerbatimAndIsGoneAfterDelete() {
        val text = "  主日\n" + "長".repeat(NoteDraft.MAX_CHARS - 10)
        files.write("new-hymn_db-5", text)
        assertThat(files.read("new-hymn_db-5")).isEqualTo(text)
        files.delete("new-hymn_db-5")
        assertThat(files.read("new-hymn_db-5")).isNull()
    }

    @Test fun writingAgainReplacesTheDraft() {
        files.write(testUuid(1), "first")
        files.write(testUuid(1), "second")
        assertThat(files.read(testUuid(1))).isEqualTo("second")
    }

    @Test fun aMissingDraftIsNull() {
        assertThat(files.read(testUuid(2))).isNull()
    }

    @Test fun everyEditorSessionGetsItsOwnToken() {
        val key = HymnKey.of(HymnTypes.DB, 5)
        val first = NoteDraftFiles.newToken(null, key)
        val second = NoteDraftFiles.newToken(null, key)
        assertThat(first).startsWith("new-hymn_db-5-")
        assertThat(second).isNotEqualTo(first)
        assertThat(NoteDraftFiles.newToken(testUuid(3), key)).startsWith(testUuid(3) + "-")
        // a token is always a valid file name
        files.write(first, "a")
        files.write(NoteDraftFiles.newToken(testUuid(3), key), "b")
    }

    @Test fun onlyDraftsOlderThanTheLimitAreCleanedUp() {
        val now = 1_790_733_600_000L
        files.write("old", "x")
        files.write("fresh", "y")
        val dir = folder.root.resolve("note-drafts")
        dir.resolve("old.txt").setLastModified(now - NoteDraftFiles.MAX_AGE_MS - 1)
        dir.resolve("fresh.txt").setLastModified(now - NoteDraftFiles.MAX_AGE_MS + 60_000)
        files.deleteOlderThan(now, NoteDraftFiles.MAX_AGE_MS)
        assertThat(files.read("old")).isNull()
        assertThat(files.read("fresh")).isEqualTo("y")
    }

    @Test fun tokensThatCouldLeaveTheFolderAreRejected() {
        listOf("../x", "a/b", "", "X").forEach { bad ->
            assertThat(runCatching { files.write(bad, "t") }.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
