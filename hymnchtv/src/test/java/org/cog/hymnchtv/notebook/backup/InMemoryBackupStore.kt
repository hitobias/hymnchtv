package org.cog.hymnchtv.notebook.backup

import org.cog.hymnchtv.notebook.backup.SampleTables.upserted

class InMemoryBackupStore(initial: NotebookTables = NotebookTables.EMPTY) : BackupStore {
    @Volatile
    var tables: NotebookTables = initial
        private set

    @Volatile
    var failNext: Boolean = false

    /** Thrown instead of IllegalStateException when [failNext] is set. */
    @Volatile
    var failure: Throwable = IllegalStateException("simulated storage failure")

    private fun maybeFail() {
        if (failNext) {
            failNext = false
            throw failure
        }
    }

    override suspend fun readAll(): NotebookTables {
        maybeFail()
        return tables
    }

    override suspend fun <R> mergeAtomically(plan: (local: NotebookTables) -> Planned<R>): R {
        maybeFail()
        val planned = plan(tables)
        tables = tables.upserted(planned.upserts)
        return planned.result
    }
}
