package org.cog.hymnchtv.notebook.repo

import org.cog.hymnchtv.notebook.model.SyncRecord

/**
 * Common data-access contract. Rows are immutable; every write returns the stored copy.
 * Timestamps and updatedBy are stamped by the repository (Clock + DeviceIdProvider); callers never set them.
 */
interface Repository<T : SyncRecord> {
    /** Active (not soft-deleted) rows. */
    suspend fun findAll(): List<T>

    /** Active row by id; null when missing or soft-deleted. */
    suspend fun findById(id: String): T?

    /**
     * Stores a new row. A blank id is replaced by a fresh UUID; a non-blank id must be a canonical lowercase UUID.
     * Throws IllegalArgumentException on invalid input.
     */
    suspend fun create(item: T): T

    /** Replaces an active row's content and bumps updatedAt; null when the row is missing or deleted. */
    suspend fun update(item: T): T?

    /** Soft delete (stamps deletedAt and updatedAt); false when missing or already deleted. */
    suspend fun delete(id: String): Boolean
}
