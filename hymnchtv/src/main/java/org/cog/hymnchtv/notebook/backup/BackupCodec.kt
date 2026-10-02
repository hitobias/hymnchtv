package org.cog.hymnchtv.notebook.backup

import org.cog.hymnchtv.notebook.data.entity.FavoriteEntity
import org.cog.hymnchtv.notebook.data.entity.NoteEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistEntity
import org.cog.hymnchtv.notebook.data.entity.PlaylistItemEntity
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.FavoriteIds
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.NotebookValidation
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource
import org.cog.hymnchtv.notebook.model.SyncRecord
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Versioned JSON backup format. Encoding writes every column, nulls explicitly. Decoding treats the file as
 * untrusted and never throws: file-level problems return Failure; invalid rows are skipped and counted by reason.
 */
object BackupCodec {
    const val FORMAT = "hymnchtv-notebook"
    const val CURRENT_SCHEMA_VERSION = 1

    /** Upper bound for imported playlist positions, so re-slotting after the highest slot (at most maxRowsPerTable moves) stays below 10^9 and can never overflow Int. */
    const val MAX_POSITION = 900_000_000L

    private const val KEY_FORMAT = "format"
    private const val KEY_SCHEMA_VERSION = "schemaVersion"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_APP_VERSION = "appVersionName"
    private const val FAVORITES = "favorites"
    private const val SING_LOGS = "singLogs"
    private const val NOTES = "notes"
    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_ITEMS = "playlistItems"
    private val TABLES = listOf(FAVORITES, SING_LOGS, NOTES, PLAYLISTS, PLAYLIST_ITEMS)
    private const val BOM = "﻿"

    fun encode(snapshot: BackupSnapshot): String {
        val tables = snapshot.tables
        return JSONObject()
            .put(KEY_FORMAT, FORMAT)
            .put(KEY_SCHEMA_VERSION, snapshot.schemaVersion)
            .put(KEY_EXPORTED_AT, snapshot.exportedAt)
            .put(KEY_APP_VERSION, snapshot.appVersionName)
            .put(FAVORITES, JSONArray(tables.favorites.map(::favoriteToJson)))
            .put(SING_LOGS, JSONArray(tables.singLogs.map(::singLogToJson)))
            .put(NOTES, JSONArray(tables.notes.map(::noteToJson)))
            .put(PLAYLISTS, JSONArray(tables.playlists.map(::playlistToJson)))
            .put(PLAYLIST_ITEMS, JSONArray(tables.playlistItems.map(::playlistItemToJson)))
            .toString(2)
    }

    /** [nowMillis] is the importing device's time; rows later than now + maxFutureSkewMillis are skipped. */
    fun decode(text: String, nowMillis: Long, limits: BackupLimits = BackupLimits()): DecodeResult {
        val body = text.removePrefix(BOM)
        shapeFailure(body, limits)?.let { return it }
        val root = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return DecodeResult.Failure(BackupError.NOT_JSON, e.message.orEmpty())
        }
        headerFailure(root, limits)?.let { return it }

        val arrays = TABLES.associateWith { tableArray(root, it, limits) }
        arrays.values.firstNotNullOfOrNull { it.failure }?.let { return it }
        val maxTime = nowMillis + limits.maxFutureSkewMillis
        val favorites = parseRows(arrays.getValue(FAVORITES).array) { favoriteFromJson(it, maxTime) }
        val singLogs = parseRows(arrays.getValue(SING_LOGS).array) { singLogFromJson(it, maxTime) }
        val notes = parseRows(arrays.getValue(NOTES).array) { noteFromJson(it, maxTime) }
        val playlists = parseRows(arrays.getValue(PLAYLISTS).array) { playlistFromJson(it, maxTime) }
        val items = parseRows(arrays.getValue(PLAYLIST_ITEMS).array) { playlistItemFromJson(it, maxTime) }

        val snapshot = BackupSnapshot(
            schemaVersion = root.getInt(KEY_SCHEMA_VERSION),
            exportedAt = root.getLong(KEY_EXPORTED_AT),
            appVersionName = root.getString(KEY_APP_VERSION),
            tables = NotebookTables(favorites.rows, singLogs.rows, notes.rows, playlists.rows, items.rows),
        )
        val skipped = favorites.skipped + singLogs.skipped + notes.skipped + playlists.skipped + items.skipped
        return DecodeResult.Success(snapshot, skipped)
    }

    // ---- file-level validation ----

    private fun headerFailure(root: JSONObject, limits: BackupLimits): DecodeResult.Failure? {
        if (root.optString(KEY_FORMAT) != FORMAT) {
            return DecodeResult.Failure(BackupError.WRONG_FORMAT, "format=${root.opt(KEY_FORMAT)}")
        }
        val version = root.opt(KEY_SCHEMA_VERSION)
        if (version !is Int || version < 1) return DecodeResult.Failure(BackupError.WRONG_FORMAT, "schemaVersion=$version")
        if (version > CURRENT_SCHEMA_VERSION) {
            return DecodeResult.Failure(BackupError.UNSUPPORTED_VERSION, "schemaVersion=$version")
        }
        val exportedAt = root.opt(KEY_EXPORTED_AT)
        if (exportedAt !is Number || exportedAt.toLong() < 0) {
            return DecodeResult.Failure(BackupError.WRONG_FORMAT, "exportedAt=$exportedAt")
        }
        val appVersion = root.opt(KEY_APP_VERSION)
        if (appVersion !is String || appVersion.length > limits.maxAppVersionLength) {
            return DecodeResult.Failure(BackupError.WRONG_FORMAT, "appVersionName is not a short string")
        }
        return null
    }

    private class TableArray(val array: JSONArray?, val failure: DecodeResult.Failure?)

    private fun tableArray(root: JSONObject, name: String, limits: BackupLimits): TableArray =
        when (val value = root.opt(name)) {
            null, JSONObject.NULL -> TableArray(null, null)
            is JSONArray ->
                if (value.length() > limits.maxRowsPerTable) {
                    TableArray(null, DecodeResult.Failure(BackupError.TOO_LARGE, "$name has ${value.length()} rows"))
                } else {
                    TableArray(value, null)
                }
            else -> TableArray(null, DecodeResult.Failure(BackupError.WRONG_FORMAT, "$name is not an array"))
        }

    /** One pass outside strings: rejects nesting deeper than the limit or more than maxNodes values, before any parsing. */
    private fun shapeFailure(text: String, limits: BackupLimits): DecodeResult.Failure? {
        var depth = 0
        var commas = 0
        var inString = false
        var escaped = false
        for (c in text) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{', '[' -> if (++depth > limits.maxNestingDepth) {
                    return DecodeResult.Failure(BackupError.WRONG_FORMAT, "Nesting deeper than ${limits.maxNestingDepth}")
                }
                '}', ']' -> depth--
                ',' -> if (++commas >= limits.maxNodes) {
                    return DecodeResult.Failure(BackupError.TOO_LARGE, "More than ${limits.maxNodes} values")
                }
            }
        }
        return null
    }

    // ---- row-level parsing ----

    private class FutureTimestampException : IllegalArgumentException("Timestamp too far in the future")

    private class RowResult<T>(val row: T?, val future: Boolean)

    private class Parsed<T>(val rows: List<T>, val skipped: SkippedRows)

    private fun <T : Any> parseRows(array: JSONArray?, parse: (JSONObject) -> T): Parsed<T> {
        if (array == null) return Parsed(emptyList(), SkippedRows.NONE)
        val results = (0 until array.length()).map { index -> parseRow { parse(array.getJSONObject(index)) } }
        return Parsed(
            rows = results.mapNotNull { it.row },
            skipped = SkippedRows(
                invalid = results.count { it.row == null && !it.future },
                futureTimestamp = results.count { it.future },
            ),
        )
    }

    private inline fun <T : Any> parseRow(parse: () -> T): RowResult<T> = try {
        RowResult(parse(), future = false)
    } catch (e: FutureTimestampException) {
        RowResult(null, future = true)
    } catch (e: JSONException) {
        RowResult(null, future = false)
    } catch (e: IllegalArgumentException) {
        RowResult(null, future = false)
    }

    private fun time(value: Long, maxTime: Long): Long {
        require(value >= 0) { "Negative timestamp" }
        if (value > maxTime) throw FutureTimestampException()
        return value
    }

    private class Sync(val id: String, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?, val updatedBy: String)

    private fun JSONObject.sync(maxTime: Long): Sync = Sync(
        id = getString("id"),
        createdAt = time(getLong("createdAt"), maxTime),
        updatedAt = time(getLong("updatedAt"), maxTime),
        deletedAt = nullableLong("deletedAt")?.let { time(it, maxTime) },
        updatedBy = NotebookValidation.uuid(getString("updatedBy")),
    ).also {
        require(it.createdAt <= it.updatedAt) { "createdAt is after updatedAt" }
        require(it.deletedAt == null || it.deletedAt >= it.updatedAt) { "deletedAt is before updatedAt" }
    }

    private fun JSONObject.hymn() = HymnKey(getString("hymnType"), getInt("hymnNo"), getBoolean("isFu"))

    private fun JSONObject.nullableLong(name: String): Long? = if (isNull(name)) null else getLong(name)

    private fun JSONObject.optionalUuid(name: String): String? =
        if (isNull(name)) null else NotebookValidation.uuid(getString(name))

    private fun favoriteFromJson(json: JSONObject, maxTime: Long): FavoriteEntity {
        val sync = json.sync(maxTime)
        val hymn = json.hymn()
        // The id is derived from the hymn; a different id would break the unique hymn index and cross-device merges.
        require(sync.id == FavoriteIds.forKey(hymn)) { "Favorite id does not match its hymn" }
        return FavoriteEntity(sync.id, hymn, sync.createdAt, sync.updatedAt, sync.deletedAt, sync.updatedBy)
    }

    private fun singLogFromJson(json: JSONObject, maxTime: Long): SingLogEntity {
        val sync = json.sync(maxTime)
        return SingLogEntity(
            id = NotebookValidation.uuid(sync.id),
            hymn = json.hymn(),
            sungAt = time(json.getLong("sungAt"), maxTime),
            occasion = Occasion.fromStorage(json.optString("occasion")) ?: Occasion.OTHER,
            source = SingSource.fromStorage(json.optString("source")) ?: SingSource.MANUAL,
            playlistId = json.optionalUuid("playlistId"),
            createdAt = sync.createdAt,
            updatedAt = sync.updatedAt,
            deletedAt = sync.deletedAt,
            updatedBy = sync.updatedBy,
        )
    }

    private fun noteFromJson(json: JSONObject, maxTime: Long): NoteEntity {
        val sync = json.sync(maxTime)
        return NoteEntity(
            id = NotebookValidation.uuid(sync.id),
            hymn = json.hymn(),
            body = NotebookValidation.noteBody(json.getString("body")),
            singLogId = json.optionalUuid("singLogId"),
            createdAt = sync.createdAt,
            updatedAt = sync.updatedAt,
            deletedAt = sync.deletedAt,
            updatedBy = sync.updatedBy,
        )
    }

    private fun playlistFromJson(json: JSONObject, maxTime: Long): PlaylistEntity {
        val sync = json.sync(maxTime)
        return PlaylistEntity(
            id = NotebookValidation.uuid(sync.id),
            name = NotebookValidation.playlistName(json.getString("name")),
            createdAt = sync.createdAt,
            updatedAt = sync.updatedAt,
            deletedAt = sync.deletedAt,
            updatedBy = sync.updatedBy,
        )
    }

    private fun playlistItemFromJson(json: JSONObject, maxTime: Long): PlaylistItemEntity {
        val sync = json.sync(maxTime)
        val position = json.getLong("position")
        require(position in 0..MAX_POSITION) { "Position out of range" }
        return PlaylistItemEntity(
            id = NotebookValidation.uuid(sync.id),
            playlistId = NotebookValidation.uuid(json.getString("playlistId")),
            position = position.toInt(),
            hymn = json.hymn(),
            createdAt = sync.createdAt,
            updatedAt = sync.updatedAt,
            deletedAt = sync.deletedAt,
            updatedBy = sync.updatedBy,
        )
    }

    // ---- encoding ----

    private fun JSONObject.putSync(row: SyncRecord): JSONObject = put("id", row.id)
        .put("createdAt", row.createdAt)
        .put("updatedAt", row.updatedAt)
        .put("deletedAt", row.deletedAt ?: JSONObject.NULL)
        .put("updatedBy", row.updatedBy)

    private fun JSONObject.putHymn(key: HymnKey): JSONObject =
        put("hymnType", key.hymnType).put("hymnNo", key.hymnNo).put("isFu", key.isFu)

    private fun favoriteToJson(row: FavoriteEntity) = JSONObject().putSync(row).putHymn(row.hymn)

    private fun singLogToJson(row: SingLogEntity) = JSONObject().putSync(row).putHymn(row.hymn)
        .put("sungAt", row.sungAt)
        .put("occasion", row.occasion.name)
        .put("source", row.source.name)
        .put("playlistId", row.playlistId ?: JSONObject.NULL)

    private fun noteToJson(row: NoteEntity) = JSONObject().putSync(row).putHymn(row.hymn)
        .put("body", row.body)
        .put("singLogId", row.singLogId ?: JSONObject.NULL)

    private fun playlistToJson(row: PlaylistEntity) = JSONObject().putSync(row).put("name", row.name)

    private fun playlistItemToJson(row: PlaylistItemEntity) = JSONObject().putSync(row)
        .put("playlistId", row.playlistId)
        .put("position", row.position)
        .putHymn(row.hymn)
}
