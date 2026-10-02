package org.cog.hymnchtv.notebook.backup

/** Data access needed by backups. Implementations must run mergeAtomically in a single transaction. */
interface BackupStore {
    /** Every row of every table, including soft-deleted rows. */
    suspend fun readAll(): NotebookTables

    /** Reads all rows, lets [plan] decide what to upsert, writes it, and returns the plan's result. */
    suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R
}

data class Planned<R>(val upserts: NotebookTables, val result: R)
