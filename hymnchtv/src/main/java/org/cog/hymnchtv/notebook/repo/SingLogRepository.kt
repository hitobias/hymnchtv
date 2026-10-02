package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.data.SingStats
import org.cog.hymnchtv.notebook.data.entity.SingLogEntity
import org.cog.hymnchtv.notebook.model.HymnKey
import org.cog.hymnchtv.notebook.model.Occasion
import org.cog.hymnchtv.notebook.model.SingSource

interface SingLogRepository : Repository<SingLogEntity> {
    /** Unconditional insert: manual entries (and D-1c "mark playlist sung") deliberately bypass dedupe. */
    suspend fun record(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        playlistId: String? = null,
    ): SingLogEntity

    /**
     * Atomic check-and-insert for every automatic path: inserts unless an active log of the same hymn has
     * |sungAt − existing| < windowMillis (DedupeWindow). Returns null for a duplicate. Room runs it in one
     * write transaction, so concurrent callers can never both insert.
     */
    suspend fun recordUnlessDuplicate(
        key: HymnKey,
        sungAt: Long,
        occasion: Occasion,
        source: SingSource,
        windowMillis: Long,
    ): SingLogEntity?

    suspend fun statsFor(key: HymnKey): SingStats

    /** Most recent active log by sungAt. */
    suspend fun latestFor(key: HymnKey): SingLogEntity?

    /** Active logs of one hymn, newest first. */
    suspend fun findByHymn(key: HymnKey): List<SingLogEntity>

    /** Active logs with fromInclusive <= sungAt < toExclusive, newest first. */
    suspend fun findBetween(fromInclusive: Long, toExclusive: Long): List<SingLogEntity>
}
