package org.cog.hymnchtv.persistance

/** Test helpers that go through the facade / Room only; no raw SQLiteDatabase of the facade is exposed. */

/** Number of media records stored for [hymnType]. */
fun DatabaseBackend.mediaCount(hymnType: String): Long = getMediaRecords(hymnType).size.toLong()

/**
 * Makes every later insert of a [hymnType] media record fail with a SQLite error (a BEFORE INSERT trigger that
 * aborts), so tests can drive the failure and rollback paths.
 */
fun DatabaseBackend.failInsertsFor(hymnType: String) {
    roomDatabase().openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_insert_$hymnType BEFORE INSERT ON media_record " +
            "WHEN NEW.hymnType = '$hymnType' BEGIN SELECT RAISE(ABORT, 'injected failure'); END"
    )
}
