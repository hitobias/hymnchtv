package org.cog.hymnchtv.editor

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class EditorStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun csvLineBreaksSurviveAReadWriteRoundTrip() {
        val csv = "hymn_db,1,false,HYMN_MEDIA,https://a.example/1,\r\nhymn_bb,2,false,HYMN_MEDIA,https://a.example/2,\n\n最後一行"
        val file = tmp.newFile("export.csv").apply { writeBytes(csv.toByteArray(Charsets.UTF_8)) }
        val text = EditorStore.read(file)
        assertThat(text).isEqualTo(csv)
        EditorStore.write(file, text)
        assertThat(file.readBytes()).isEqualTo(csv.toByteArray(Charsets.UTF_8))
    }

    @Test
    fun writeReplacesLongerOldContent() {
        val file = tmp.newFile("export.csv").apply { writeText("a much longer old content") }
        EditorStore.write(file, "b")
        assertThat(EditorStore.read(file)).isEqualTo("b")
    }

    @Test
    fun theDraftLivesInTheCacheDirAndIsUsedOnlyWhenAsked() {
        val cache = tmp.newFolder("cache")
        val source = tmp.newFile("source.csv").apply { writeText("file") }
        EditorStore.saveDraft(cache, "draft")
        assertThat(EditorStore.draftFile(cache)).isEqualTo(File(cache, EditorStore.DRAFT_NAME))
        assertThat(EditorStore.load(source, cache, true)).isEqualTo(EditorStore.Loaded("draft", true))
        assertThat(EditorStore.load(source, cache, false)).isEqualTo(EditorStore.Loaded("file", false))
        EditorStore.clearDraft(cache)
        assertThat(EditorStore.load(source, cache, true)).isEqualTo(EditorStore.Loaded("file", false))
    }

    @Test(expected = IOException::class)
    fun aMissingFileThrows() {
        EditorStore.read(File(tmp.root, "none.csv"))
    }
}
