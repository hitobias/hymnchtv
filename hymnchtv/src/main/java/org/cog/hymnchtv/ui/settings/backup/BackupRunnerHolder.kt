package org.cog.hymnchtv.ui.settings.backup

/**
 * Creates the [BackupRunner] once and hands out the same one afterwards, so a job started on one visit to Settings (which runs
 * in an app-wide scope) still shows its result on the next visit.
 */
class BackupRunnerHolder<D>(private val create: () -> BackupRunner<D>) {
    private val runner: BackupRunner<D> by lazy(create)

    fun get(): BackupRunner<D> = runner
}
