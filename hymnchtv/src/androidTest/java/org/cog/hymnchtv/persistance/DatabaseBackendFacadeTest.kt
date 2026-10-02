package org.cog.hymnchtv.persistance

import android.database.SQLException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.MainActivity
import org.cog.hymnchtv.MediaType
import org.cog.hymnchtv.hymnhistory.HistoryRecord
import org.cog.hymnchtv.mediaconfig.MediaRecord
import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Behaviour of every facade method against a real file-backed Room database. */
@RunWith(AndroidJUnit4::class)
class DatabaseBackendFacadeTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: DatabaseBackend

    @Before
    fun setUp() {
        ctx.deleteDatabase(NAME)
        db = DatabaseBackend.createForTest(ctx, NAME)
    }

    @After
    fun tearDown() {
        db.close()
        ctx.deleteDatabase(NAME)
    }

    private fun media(no: Int, type: MediaType, uri: String?, path: String? = null, hymn: String = MainActivity.HYMN_DB, fu: Boolean = false) =
        MediaRecord(hymn, no, fu, type, uri, path)

    private fun history(no: Int, ts: Long, type: String = MainActivity.HYMN_DB, fu: Boolean = false) =
        HistoryRecord(type, no, fu, "title$no", ts)

    private fun seedHistory(count: Int, tsOf: (Int) -> Long = { it * 10L }) {
        val dao = db.roomDatabase().hymnHistoryDao()
        for (i in 1..count) dao.insert(HymnHistoryEntity(MainActivity.HYMN_DB, i, false, "title$i", tsOf(i)))
    }

    private fun historyNos() = db.historyRecords.map { it.hymnNo }

    // ---- media records ----------------------------------------------------------------------------------

    @Test
    fun storeReturnsARowIdAndGetMediaRecordUpdatesTheGivenRecord() {
        val row = db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "https://a.example/1", "/f/1"))
        assertThat(row).isGreaterThan(0L)

        val probe = media(1, MediaType.HYMN_MEDIA, null)
        assertThat(db.getMediaRecord(probe, false)).isTrue()
        assertThat(probe.mediaUri).isNull() // update=false leaves the record untouched

        assertThat(db.getMediaRecord(probe, true)).isTrue()
        assertThat(probe.mediaUri).isEqualTo("https://a.example/1")
        assertThat(probe.mediaFilePath).isEqualTo("/f/1")
    }

    @Test
    fun getMediaRecordIsFalseForAMissingRecord() {
        val probe = media(9, MediaType.HYMN_MEDIA, "keep")
        assertThat(db.getMediaRecord(probe, true)).isFalse()
        assertThat(probe.mediaUri).isEqualTo("keep")
    }

    @Test
    fun storingTheSameKeyOverwritesInsteadOfAddingARow() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "https://old"))
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "https://new", "/new"))
        val all = db.getMediaRecords(MainActivity.HYMN_DB)
        assertThat(all).hasSize(1)
        assertThat(all[0].mediaUri).isEqualTo("https://new")
        assertThat(all[0].mediaFilePath).isEqualTo("/new")
    }

    @Test
    fun keyIncludesIsFuAndMediaTypeAndHymnType() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "u1"))
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "u2", fu = true))
        db.storeMediaRecord(media(1, MediaType.HYMN_JIAOCHANG, "u3"))
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "u4", hymn = MainActivity.HYMN_BB))
        assertThat(db.mediaCount(MainActivity.HYMN_DB)).isEqualTo(3L)
        assertThat(db.mediaCount(MainActivity.HYMN_BB)).isEqualTo(1L)
    }

    @Test
    fun nullableFieldsRoundTrip() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, null, null))
        db.storeMediaRecord(media(2, MediaType.HYMN_MEDIA, "https://x", null))
        db.storeMediaRecord(media(3, MediaType.HYMN_MEDIA, null, "/only/file"))
        val all = db.getMediaRecords(MainActivity.HYMN_DB)
        assertThat(all.map { it.mediaUri }).containsExactly(null, "https://x", null).inOrder()
        assertThat(all.map { it.mediaFilePath }).containsExactly(null, null, "/only/file").inOrder()
    }

    @Test
    fun getMediaRecordsOrdersByNumberThenFuThenMediaType() {
        db.storeMediaRecord(media(2, MediaType.HYMN_MEDIA, "c"))
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "b", fu = true))
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "a"))
        db.storeMediaRecord(media(1, MediaType.HYMN_JIAOCHANG, "z"))
        val all = db.getMediaRecords(MainActivity.HYMN_DB)
        assertThat(all.map { Triple(it.hymnNo, it.isFu, it.mediaType) }).containsExactly(
            Triple(1, false, MediaType.HYMN_JIAOCHANG),
            Triple(1, false, MediaType.HYMN_MEDIA),
            Triple(1, true, MediaType.HYMN_MEDIA),
            Triple(2, false, MediaType.HYMN_MEDIA),
        ).inOrder()
    }

    @Test
    fun getMediaRecordsForOneHymnReturnsAllItsMediaTypesOnly() {
        db.storeMediaRecord(media(5, MediaType.HYMN_MEDIA, "m"))
        db.storeMediaRecord(media(5, MediaType.HYMN_JIAOCHANG, "j"))
        db.storeMediaRecord(media(5, MediaType.HYMN_MEDIA, "fu", fu = true))
        db.storeMediaRecord(media(6, MediaType.HYMN_MEDIA, "other"))
        val one = db.getMediaRecords(MainActivity.HYMN_DB, 5, false)
        assertThat(one.map { it.mediaUri }).containsExactly("j", "m").inOrder()
        assertThat(db.getMediaRecords(MainActivity.HYMN_DB, 5, true)).hasSize(1)
        assertThat(db.getMediaRecords(MainActivity.HYMN_DB, 7, false)).isEmpty()
    }

    @Test
    fun getMediaLinksReturnsOnlyWebLinks() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "https://a"))
        db.storeMediaRecord(media(2, MediaType.HYMN_MEDIA, "HTTP://upper"))
        db.storeMediaRecord(media(3, MediaType.HYMN_MEDIA, "content://local"))
        db.storeMediaRecord(media(4, MediaType.HYMN_MEDIA, null, "/file"))
        assertThat(db.getMediaLinks(MainActivity.HYMN_DB).map { it.hymnNo }).containsExactly(1, 2).inOrder()
    }

    @Test
    fun deleteMediaRecordReturnsTheNumberOfDeletedRows() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "a"))
        assertThat(db.deleteMediaRecord(media(1, MediaType.HYMN_MEDIA, null))).isEqualTo(1)
        assertThat(db.deleteMediaRecord(media(1, MediaType.HYMN_MEDIA, null))).isEqualTo(0)
        assertThat(db.mediaCount(MainActivity.HYMN_DB)).isEqualTo(0L)
    }

    @Test
    fun getHymnUrlForcesHttpsAndUpdatesTheRecord() {
        db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "http://a.example/x"))
        val probe = media(1, MediaType.HYMN_MEDIA, null)
        assertThat(db.getHymnUrl(probe)).isEqualTo("https://a.example/x")
        assertThat(probe.mediaUri).isEqualTo("https://a.example/x")
        assertThat(db.getHymnUrl(media(2, MediaType.HYMN_MEDIA, null))).isNull()
    }

    // ---- unknown hymnType -------------------------------------------------------------------------------

    @Test
    fun unknownHymnTypeIsRejectedByEveryApi() {
        val bad = media(1, MediaType.HYMN_MEDIA, "https://x", hymn = "hymn_zz")
        assertThat(db.storeMediaRecord(bad)).isEqualTo(-1L)
        assertThrows(SQLException::class.java) { db.storeMediaRecordOrThrow(bad) }
        assertThat(db.getMediaRecord(bad, true)).isFalse()
        assertThat(db.getHymnUrl(bad)).isNull()
        assertThat(db.deleteMediaRecord(bad)).isEqualTo(0)
        assertThat(db.getMediaRecords("hymn_zz")).isEmpty()
        assertThat(db.getMediaRecords("hymn_zz", 1, false)).isEmpty()
        assertThat(db.getMediaLinks("hymn_zz")).isEmpty()
        assertThat(db.roomDatabase().mediaRecordDao().listByType("hymn_zz")).isEmpty() // nothing leaked in
    }

    // ---- failure modes ----------------------------------------------------------------------------------

    @Test
    fun plainStoreReturnsMinusOneOnSqlErrorWhileOrThrowThrowsSqlException() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        val bb = media(1, MediaType.HYMN_MEDIA, "https://bb", hymn = MainActivity.HYMN_BB)
        assertThat(db.storeMediaRecord(bb)).isEqualTo(-1L)
        assertThrows(SQLException::class.java) { db.storeMediaRecordOrThrow(bb) }
        assertThat(db.mediaCount(MainActivity.HYMN_BB)).isEqualTo(0L)
    }

    @Test
    fun orThrowFailureInsideATransactionRollsBackTheWholeBatch() {
        db.failInsertsFor(MainActivity.HYMN_BB)
        assertThrows(SQLException::class.java) {
            db.runInTransaction {
                db.storeMediaRecordOrThrow(media(1, MediaType.HYMN_MEDIA, "https://a"))
                db.storeMediaRecordOrThrow(media(2, MediaType.HYMN_MEDIA, "https://b", hymn = MainActivity.HYMN_BB))
            }
        }
        assertThat(db.mediaCount(MainActivity.HYMN_DB)).isEqualTo(0L)
    }

    @Test
    fun exceptionOfTheBodyIsRethrownAsIsFromANestedTransaction() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            db.runInTransaction { db.runInTransaction { throw IllegalArgumentException("nested boom") } }
        }
        assertThat(error).hasMessageThat().isEqualTo("nested boom")
    }

    @Test
    fun nestedFailureRollsBackOuterWritesEvenWhenTheOuterBodyCatches() {
        db.runInTransaction {
            db.storeMediaRecord(media(1, MediaType.HYMN_MEDIA, "a"))
            try {
                db.runInTransaction {
                    db.storeMediaRecord(media(2, MediaType.HYMN_MEDIA, "b"))
                    throw IllegalStateException("inner")
                }
            } catch (expected: IllegalStateException) {
                // swallowed on purpose
            }
        }
        assertThat(db.mediaCount(MainActivity.HYMN_DB)).isEqualTo(0L)
    }

    // ---- history ----------------------------------------------------------------------------------------

    @Test
    fun historyIsListedNewestFirstAndRoundTrips() {
        db.storeHymnHistory(history(1, 100))
        db.storeHymnHistory(history(2, 300, MainActivity.HYMN_BB, true))
        db.storeHymnHistory(history(3, 200))
        val list = db.historyRecords
        assertThat(list.map { it.hymnNo }).containsExactly(2, 3, 1).inOrder()
        assertThat(list[0].hymnType).isEqualTo(MainActivity.HYMN_BB)
        assertThat(list[0].isFu).isTrue()
        assertThat(list[0].hymnTitle).isEqualTo("title2")
        assertThat(list[0].timeStamp).isEqualTo(300L)
    }

    @Test
    fun sameHistoryKeyOverwritesTheOlderRow() {
        db.storeHymnHistory(history(1, 100))
        db.storeHymnHistory(HistoryRecord(MainActivity.HYMN_DB, 1, false, "renamed", 500))
        val list = db.historyRecords
        assertThat(list).hasSize(1)
        assertThat(list[0].hymnTitle).isEqualTo("renamed")
        assertThat(list[0].timeStamp).isEqualTo(500L)
    }

    @Test
    fun deleteHymnHistoryIgnoresIsFuAndDeletesBothRows() {
        db.storeHymnHistory(history(7, 100, fu = false))
        db.storeHymnHistory(history(7, 200, fu = true))
        db.storeHymnHistory(history(8, 300))
        db.storeHymnHistory(history(7, 400, MainActivity.HYMN_BB))
        assertThat(db.deleteHymnHistory(history(7, 0, fu = false))).isEqualTo(2)
        assertThat(historyNos()).containsExactly(7, 8).inOrder() // BB 7 (ts 400) and DB 8 (ts 300) remain
        assertThat(db.historyRecords[0].hymnType).isEqualTo(MainActivity.HYMN_BB)
    }

    @Test
    fun history199InsertingANewKeyDoesNotPurge() {
        seedHistory(199)
        db.storeHymnHistory(history(1000, 99999))
        assertThat(db.historyRecords).hasSize(200)
    }

    @Test
    fun history200InsertingANewKeyDoesNotPurge() {
        seedHistory(200)
        db.storeHymnHistory(history(1000, 99999))
        assertThat(db.historyRecords).hasSize(201)
    }

    @Test
    fun history201PurgesTheOldestTenThenInserts() {
        seedHistory(201)
        db.storeHymnHistory(history(1000, 99999))
        val nos = historyNos()
        assertThat(nos).hasSize(192)
        assertThat(nos).doesNotContain(10)
        assertThat(nos).contains(11)
        assertThat(nos).contains(1000)
    }

    @Test
    fun history200ReplacingAnExistingKeyKeepsTheCount() {
        seedHistory(200)
        db.storeHymnHistory(history(150, 99999))
        assertThat(db.historyRecords).hasSize(200)
        assertThat(db.historyRecords[0].hymnNo).isEqualTo(150)
    }

    @Test
    fun history201ReplacingAnExistingKeyPurgesTenThenOverwrites() {
        seedHistory(201)
        db.storeHymnHistory(history(150, 99999))
        assertThat(db.historyRecords).hasSize(191)
    }

    @Test
    fun historyTiedTimestampsAtThePivotAreKept() {
        // rows 10, 11, 12 share timestamp 110; the 11th oldest row has timestamp 110 = the pivot
        seedHistory(201) { if (it in 10..12) 110L else it * 10L }
        db.storeHymnHistory(history(1000, 99999))
        val nos = historyNos().toSet()
        assertThat(nos).containsAtLeast(10, 11, 12)
        assertThat((1..9).filter { it in nos }).isEmpty() // only the 9 strictly older rows were purged
        assertThat(nos).hasSize(201 - 9 + 1)
    }

    // ---- english lyrics ---------------------------------------------------------------------------------

    @Test
    fun englishLyricsStoreOverwriteGetAndDelete() {
        assertThat(db.getLyricsEnglish(5)).isNull()
        assertThat(db.storeLyricsEng(5, "<p>one</p>")).isGreaterThan(0L)
        assertThat(db.getLyricsEnglish(5)).isEqualTo("<p>one</p>")
        db.storeLyricsEng(5, "<p>two</p>")
        assertThat(db.getLyricsEnglish(5)).isEqualTo("<p>two</p>")
        assertThat(db.deleteLyricsEng(5)).isEqualTo(1)
        assertThat(db.deleteLyricsEng(5)).isEqualTo(0)
        assertThat(db.getLyricsEnglish(5)).isNull()
    }

    @Test
    fun englishLyricsAcceptNull() {
        db.storeLyricsEng(6, null)
        assertThat(db.getLyricsEnglish(6)).isNull()
    }

    private companion object {
        const val NAME = "test-facade.db"
    }
}
