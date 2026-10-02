package org.cog.hymnchtv.notebook.model

/** Sync-ready columns shared by every notebook table (see master plan 子項目 S). Times are epoch millis. */
interface SyncRecord {
    val id: String
    val createdAt: Long
    val updatedAt: Long
    val deletedAt: Long?

    /** Device id (NotebookPrefs.deviceId()) of the last writer; merge tie-breaker. */
    val updatedBy: String
}

val SyncRecord.isActive: Boolean
    get() = deletedAt == null

/** Merge version: a delete stamped after the last update counts as the newer change. */
val SyncRecord.version: Long
    get() = maxOf(updatedAt, deletedAt ?: Long.MIN_VALUE)
