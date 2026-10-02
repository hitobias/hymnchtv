package org.cog.hymnchtv.notebook.backup

import com.google.common.truth.Truth.assertThat
import org.cog.hymnchtv.notebook.backup.SampleTables.DEVICE_A
import org.cog.hymnchtv.notebook.fakes.testUuid
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.json.JSONObject
import org.junit.Test

class BackupCodecTest {
    private val now = 1_790_733_600_000L
    private val day = 86_400_000L
    private val favDb1 = FavoriteIds.forKey(SampleTables.db1)

    private fun snapshot(tables: NotebookTables = SampleTables.full()) =
        BackupSnapshot(BackupCodec.CURRENT_SCHEMA_VERSION, now, "2.9.2", tables)

    private fun decode(text: String, limits: BackupLimits = BackupLimits()) = BackupCodec.decode(text, now, limits)

    private fun decodeOk(text: String): DecodeResult.Success {
        val result = decode(text)
        assertThat(result).isInstanceOf(DecodeResult.Success::class.java)
        return result as DecodeResult.Success
    }

    private fun failureOf(text: String, limits: BackupLimits = BackupLimits()): BackupError? =
        (decode(text, limits) as? DecodeResult.Failure)?.error

    private fun doc(vararg tables: Pair<String, String>, header: String = "\"exportedAt\":1,\"appVersionName\":\"t\"") =
        buildString {
            append("{\"format\":\"hymnchtv-notebook\",\"schemaVersion\":1,").append(header)
            tables.forEach { (name, json) -> append(",\"$name\":$json") }
            append("}")
        }

    private fun favJson(id: String, type: String = "hymn_db", no: Int = 1, isFu: Boolean = false) =
        """{"id":"$id","hymnType":"$type","hymnNo":$no,"isFu":$isFu,"createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}"""

    private fun logJson(
        id: String = testUuid(11),
        occasion: String = "HOME",
        source: String = "AUTO",
        sungAt: String = "\"sungAt\":5,",
        updatedAt: Long = 1,
        playlistId: String = "null",
        updatedBy: String = "\"updatedBy\":\"$DEVICE_A\",",
    ) = """{"id":"$id","hymnType":"hymn_db","hymnNo":1,"isFu":false,$sungAt$updatedBy"occasion":"$occasion",""" +
        """"source":"$source","playlistId":$playlistId,"createdAt":1,"updatedAt":$updatedAt,"deletedAt":null}"""

    private fun noteJson(body: String) =
        """{"id":"${testUuid(21)}","hymnType":"hymn_db","hymnNo":1,"isFu":false,"body":"$body","singLogId":null,""" +
            """"createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}"""

    @Test
    fun roundTripKeepsEveryRowAndField() {
        val original = snapshot()
        val decoded = decodeOk(BackupCodec.encode(original))
        assertThat(decoded.snapshot).isEqualTo(original)
        assertThat(decoded.skipped).isEqualTo(SkippedRows.NONE)
    }

    @Test
    fun emptyTablesRoundTrip() {
        val original = snapshot(NotebookTables.EMPTY)
        assertThat(decodeOk(BackupCodec.encode(original)).snapshot).isEqualTo(original)
    }

    @Test
    fun headerNullsAndUpdatedByAreWrittenExplicitly() {
        val json = JSONObject(BackupCodec.encode(snapshot()))
        assertThat(json.getString("format")).isEqualTo("hymnchtv-notebook")
        assertThat(json.getInt("schemaVersion")).isEqualTo(1)
        assertThat(json.getLong("exportedAt")).isEqualTo(now)
        assertThat(json.getString("appVersionName")).isEqualTo("2.9.2")
        val favorite = json.getJSONArray("favorites").getJSONObject(0)
        assertThat(favorite.has("deletedAt")).isTrue()
        assertThat(favorite.isNull("deletedAt")).isTrue()
        assertThat(favorite.getString("updatedBy")).isEqualTo(DEVICE_A)
    }

    @Test
    fun favoriteIdMustEqualTheDerivedId() {
        assertThat(decodeOk(doc("favorites" to "[${favJson(favDb1)}]")).snapshot.tables.favorites).hasSize(1)
        val foreign = decodeOk(doc("favorites" to "[${favJson(testUuid(5))}]"))
        assertThat(foreign.snapshot.tables.favorites).isEmpty()
        assertThat(foreign.skipped).isEqualTo(SkippedRows(invalid = 1))
    }

    @Test
    fun maliciousIdsAreSkipped() {
        val ids = listOf(testUuid(0xab).uppercase(), "1-1-1-1-1", "../../etc/passwd", "", "x".repeat(300))
        val logs = ids.map { logJson(id = it) } + logJson(id = testUuid(2), playlistId = "\"nope\"")
        val decoded = decodeOk(doc("singLogs" to logs.joinToString(",", "[", "]")))
        assertThat(decoded.snapshot.tables.singLogs).isEmpty()
        assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 6))
    }

    @Test
    fun nonCanonicalHymnKeysAreSkipped() {
        val rows = listOf(
            favJson(testUuid(1), no = 781, isFu = false),
            favJson(testUuid(2), type = "hymn_bb", no = 50),
            favJson(testUuid(3), no = 787, isFu = true),
            favJson(testUuid(4), type = "x"),
        )
        val decoded = decodeOk(doc("favorites" to rows.joinToString(",", "[", "]")))
        assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 4))
    }

    @Test
    fun futureTimestampsAreSkippedSeparately() {
        val rows = listOf(
            logJson(id = testUuid(1), updatedAt = now + day),
            logJson(id = testUuid(2), updatedAt = now + day + 1),
            logJson(id = testUuid(3), sungAt = "\"sungAt\":${now + day + 1},"),
        )
        val decoded = decodeOk(doc("singLogs" to rows.joinToString(",", "[", "]")))
        assertThat(decoded.snapshot.tables.singLogs.map { it.id }).containsExactly(testUuid(1))
        assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 0, futureTimestamp = 2))
    }

    @Test
    fun unknownEnumsDegrade() {
        val log = decodeOk(doc("singLogs" to "[${logJson(occasion = "SUNDAY_SERVICE", source = "BOT")}]"))
            .snapshot.tables.singLogs.single()
        assertThat(log.occasion).isEqualTo(Occasion.OTHER)
        assertThat(log.source).isEqualTo(SingSource.MANUAL)
    }

    @Test
    fun invalidRowsAreSkippedAndCounted() {
        val decoded = decodeOk(
            doc(
                "singLogs" to "[${logJson(id = testUuid(1), sungAt = "")},${logJson(id = testUuid(2), updatedBy = "")}]",
                "notes" to "[${noteJson("")},${noteJson("字".repeat(100_001))}]",
                "playlists" to """[{"id":"${testUuid(31)}","name":" ","createdAt":1,"updatedAt":1,"deletedAt":null,"updatedBy":"$DEVICE_A"}]""",
            ),
        )
        assertThat(decoded.skipped).isEqualTo(SkippedRows(invalid = 5))
        assertThat(decoded.snapshot.tables.rowCount).isEqualTo(0)
    }

    @Test
    fun missingTablesAreEmpty() {
        assertThat(decodeOk(doc()).snapshot.tables).isEqualTo(NotebookTables.EMPTY)
    }

    @Test
    fun wrongTypedTableRejectsTheFile() {
        assertThat(failureOf(doc("favorites" to "{}"))).isEqualTo(BackupError.WRONG_FORMAT)
        assertThat(failureOf(doc("notes" to "\"x\""))).isEqualTo(BackupError.WRONG_FORMAT)
    }

    @Test
    fun headerIsValidated() {
        listOf(
            "\"exportedAt\":\"yesterday\",\"appVersionName\":\"t\"",
            "\"appVersionName\":\"t\"",
            "\"exportedAt\":-1,\"appVersionName\":\"t\"",
            "\"exportedAt\":1,\"appVersionName\":\"${"v".repeat(65)}\"",
            "\"exportedAt\":1,\"appVersionName\":5",
        ).forEach { assertThat(failureOf(doc(header = it))).isEqualTo(BackupError.WRONG_FORMAT) }
        assertThat(failureOf("""{"format":"hymnchtv-notebook","schemaVersion":"1","exportedAt":1,"appVersionName":"t"}"""))
            .isEqualTo(BackupError.WRONG_FORMAT)
    }

    @Test
    fun tooManyRowsRejectsTheFile() {
        val rows = (1..3).joinToString(",", "[", "]") { logJson(id = testUuid(it)) }
        assertThat(failureOf(doc("singLogs" to rows), BackupLimits(maxRowsPerTable = 2))).isEqualTo(BackupError.TOO_LARGE)
    }

    @Test
    fun deepNestingIsRejectedBeforeParsing() {
        assertThat(failureOf("[".repeat(100_000))).isEqualTo(BackupError.WRONG_FORMAT)
        assertThat(failureOf(doc("extra" to "[[[[1]]]]"))).isEqualTo(BackupError.WRONG_FORMAT)
        assertThat(decodeOk(doc("notes" to "[${noteJson("[[[[[{{{ brackets in text are fine")}]")).skipped)
            .isEqualTo(SkippedRows.NONE)
    }

    @Test
    fun byteOrderMarkIsIgnored() {
        assertThat(decodeOk("﻿" + BackupCodec.encode(snapshot())).snapshot).isEqualTo(snapshot())
    }

    @Test
    fun rejectsNonJson() {
        listOf("hello", "", "[1,2]").forEach { assertThat(failureOf(it)).isEqualTo(BackupError.NOT_JSON) }
    }

    @Test
    fun rejectsOtherFormats() {
        assertThat(failureOf("""{"format":"x","schemaVersion":1}""")).isEqualTo(BackupError.WRONG_FORMAT)
        assertThat(failureOf("""{"format":"hymnchtv-notebook"}""")).isEqualTo(BackupError.WRONG_FORMAT)
    }

    @Test
    fun rejectsNewerSchema() {
        assertThat(failureOf("""{"format":"hymnchtv-notebook","schemaVersion":2}"""))
            .isEqualTo(BackupError.UNSUPPORTED_VERSION)
    }
}
