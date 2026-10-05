package org.cog.hymnchtv.editor

import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
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
        val uri = source.absolutePath
        EditorStore.saveDraft(cache, uri, "draft")
        assertThat(EditorStore.draftFile(cache, uri).parentFile).isEqualTo(cache)
        assertThat(EditorStore.load(source, cache, uri, true)).isEqualTo(EditorStore.Loaded("draft", true))
        assertThat(EditorStore.load(source, cache, uri, false)).isEqualTo(EditorStore.Loaded("file", false))
        EditorStore.clearDraft(cache, uri)
        assertThat(EditorStore.load(source, cache, uri, true)).isEqualTo(EditorStore.Loaded("file", false, true))
    }

    @Test
    fun draftsOfDifferentFilesAreIndependent() {
        val cache = tmp.newFolder("cache")
        val a = tmp.newFile("a.csv").apply { writeText("fileA") }
        val b = tmp.newFile("b.csv").apply { writeText("fileB") }
        EditorStore.saveDraft(cache, a.absolutePath, "draftA")
        EditorStore.saveDraft(cache, b.absolutePath, "draftB")
        assertThat(EditorStore.draftFile(cache, a.absolutePath)).isNotEqualTo(EditorStore.draftFile(cache, b.absolutePath))
        EditorStore.clearDraft(cache, a.absolutePath)
        assertThat(EditorStore.load(b, cache, b.absolutePath, true)).isEqualTo(EditorStore.Loaded("draftB", true))
        assertThat(EditorStore.load(a, cache, a.absolutePath, true).fromDraft).isFalse()
    }

    @Test
    fun aDraftIsMissingWhenAskedForButAbsent() {
        val cache = tmp.newFolder("cache")
        val source = tmp.newFile("source.csv").apply { writeText("file") }
        assertThat(EditorStore.load(source, cache, source.absolutePath, true).draftMissing).isTrue()
        assertThat(EditorStore.load(source, cache, source.absolutePath, false).draftMissing).isFalse()
    }

    private fun canCreateIn(dir: File): Boolean = try {
        File(dir, "probe").createNewFile().also { File(dir, "probe").delete() }
    } catch (e: IOException) {
        false
    }

    @Test
    fun aFailedWriteLeavesTheOriginalAndNoTempFile() {
        val dir = tmp.newFolder("dir")
        val file = File(dir, "export.csv").apply { writeText("original") }
        dir.setWritable(false)
        try {
            assumeTrue(!canCreateIn(dir))
            try {
                EditorStore.write(file, "new")
                throw AssertionError("expected IOException")
            } catch (expected: IOException) {
            }
        } finally {
            dir.setWritable(true)
        }
        assertThat(file.readText()).isEqualTo("original")
        assertThat(dir.list()!!.toList()).containsExactly("export.csv")
    }

    @Test
    fun aFailedReplaceLeavesNoTempFile() {
        val dir = tmp.newFolder("dir")
        val target = File(dir, "export.csv").apply { mkdir() }
        File(target, "child").writeText("x")
        try {
            EditorStore.write(target, "new")
            throw AssertionError("expected IOException")
        } catch (expected: IOException) {
        }
        assertThat(dir.list()!!.toList()).containsExactly("export.csv")
        assertThat(File(target, "child").readText()).isEqualTo("x")
    }

    @Test(expected = IOException::class)
    fun aMissingFileThrows() {
        EditorStore.read(File(tmp.root, "none.csv"))
    }

    @Test
    fun aFileAtTheEditLimitIsLoaded() {
        val cache = tmp.newFolder("cache")
        val source = tmp.newFile("source.csv").apply { writeBytes(ByteArray(EditorStore.MAX_EDIT_BYTES.toInt()) { 'a'.code.toByte() }) }
        val loaded = EditorStore.load(source, cache, source.absolutePath, false)
        assertThat(loaded.tooLarge).isFalse()
        assertThat(loaded.text.length.toLong()).isEqualTo(EditorStore.MAX_EDIT_BYTES)
    }

    /** API 24 has a 32 MB heap: a larger file in an EditText ran out of memory, so it is refused unread. */
    @Test
    fun aFileOverTheEditLimitIsRefusedWithoutReadingIt() {
        val cache = tmp.newFolder("cache")
        val source = tmp.newFile("source.csv").apply { writeBytes(ByteArray(EditorStore.MAX_EDIT_BYTES.toInt() + 1)) }
        assertThat(EditorStore.load(source, cache, source.absolutePath, false)).isEqualTo(EditorStore.Loaded("", false, false, true))
    }

    @Test
    fun aFileThatIsNotUtf8IsRefusedAndLeftAlone() {
        val gbk = "hymn_db,1,詩歌".toByteArray(charset("GBK"))
        val file = tmp.newFile("gbk.csv").apply { writeBytes(gbk) }
        val loaded = EditorStore.load(file, tmp.root, file.path, preferDraft = false)
        assertThat(loaded.notUtf8).isTrue()
        assertThat(loaded.text).isEmpty()
        assertThat(file.readBytes()).isEqualTo(gbk)
    }

    @Test
    fun utf8WithABomAndAnEmptyFileAreFine() {
        val bom = tmp.newFile("bom.csv").apply { writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a".toByteArray()) }
        assertThat(EditorStore.load(bom, tmp.root, bom.path, preferDraft = false).notUtf8).isFalse()
        val empty = tmp.newFile("empty.csv")
        assertThat(EditorStore.load(empty, tmp.root, empty.path, preferDraft = false).notUtf8).isFalse()
    }

    @Test(expected = EditorStore.NotUtf8Exception::class)
    fun readRefusesMalformedUtf8() {
        EditorStore.read(tmp.newFile("bad.csv").apply { writeBytes(byteArrayOf(0xC3.toByte(), 0x28)) })
    }
}
