/*
 * hymnchtv: COG hymns' lyrics viewer and player client
 * Copyright 2020 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cog.hymnchtv.persistance;

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;

import android.content.Context;
import android.database.SQLException;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.cog.hymnchtv.MediaType;
import org.cog.hymnchtv.hymnhistory.HistoryRecord;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.room.HymnchtvDatabase;
import org.cog.hymnchtv.persistance.room.entity.EnglishLyricsEntity;
import org.cog.hymnchtv.persistance.room.entity.HymnHistoryEntity;
import org.cog.hymnchtv.persistance.room.entity.MediaRecordEntity;

import timber.log.Timber;

/**
 * The <tt>DatabaseBackend</tt> is the app's data access facade over the unified Room {@link HymnchtvDatabase}
 * (file "hymnchtv.db"). The public methods keep the signatures of the former SQLite implementation.
 * <p>
 * Threading: every method should run on {@code AppExecutors.io}. TODO(1.0): the transitional main-thread call
 * sites below still query on the main thread (the database allows it for now) and must be moved to
 * {@code AppExecutors.io} before the 1.0 release (plan section 2.4). Line numbers drift; grep the method names:
 * <ul>
 * <li>MainActivity.initHistoryList -> getHistoryRecords</li>
 * <li>MainActivity history row delete (MySwipeListAdapter.remove) -> deleteHymnHistory</li>
 * <li>ContentHandler.getHymnMediaState -> getMediaRecord x4 (plan B-4)</li>
 * <li>ContentHandler options-menu item lyrcsEnglishDelete -> deleteLyricsEng</li>
 * <li>ContentHandler.getMediaUrl (share menu) -> getMediaRecord</li>
 * <li>MediaContentHandler.getMediaUris -> getMediaRecord (from ContentHandler, two call sites)</li>
 * <li>LyricsEnglishRecord.fetchLyrics -> getLyricsEnglish (from ContentView), and storeLyricsEng in its
 * WebView download callback (WebView callbacks run on the main thread)</li>
 * <li>MediaConfig.onCreate / checkEntry -> hasMediaRecord, getMediaRecord (checkEntry also runs from the
 * entry text watchers)</li>
 * <li>MediaConfig.updateMediaRecord (button_add onClick) -> hasMediaRecord</li>
 * <li>MediaConfig.saveMediaRecord -> storeMediaRecord</li>
 * <li>MediaConfig record delete dialog onConfirmClicked -> getMediaRecord, deleteMediaRecord</li>
 * </ul>
 * Everything else (url, Notion and QQ importers, MediaLinksUpdater, MainActivity store-history, record list and
 * export reads) already runs on a worker thread.
 * <p>
 * 1.0 gate: build the debug app with {@code ./gradlew -PstrictDbThread :hymnchtv:installDebug}. That sets
 * {@code BuildConfig.ALLOW_MAIN_THREAD_DB} to false, the app database is built without
 * {@code allowMainThreadQueries()}, and every remaining main-thread query throws IllegalStateException
 * ("Cannot access database on the main thread"). Walk the main flows (open a hymn, share, history list and swipe
 * delete, English lyrics show/delete, media config add/overwrite/delete/list/export/import); the gate passes when
 * nothing crashes. The gate mechanism itself is covered by {@code MainThreadQueryGateTest}. Once the list above is
 * empty, remove {@code allowMainThreadQueries()} for good.
 *
 * @author Eng Chong Meng
 */
public class DatabaseBackend {
    /** The hymn-book codes that may be used as MediaRecord.getHymnType(); anything else is rejected. */
    private static final Set<String> KNOWN_HYMN_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(HYMN_DB, HYMN_BB, HYMN_ER, HYMN_XB, HYMN_XG, HYMN_YB)));

    private static DatabaseBackend instance = null;

    private final HymnchtvDatabase db;

    private DatabaseBackend(HymnchtvDatabase db) {
        this.db = db;
    }

    /**
     * A separate, non-singleton database file for instrumented tests and measurements; never touches the app
     * database. The caller closes it and deletes the file (Context#deleteDatabase).
     */
    @VisibleForTesting
    public static DatabaseBackend createForTest(Context context, String name) {
        return new DatabaseBackend(HymnchtvDatabase.build(context, name));
    }

    /**
     * Get the facade over the process-wide {@link HymnchtvDatabase#getInstance(Context)} and create it if new.
     *
     * @param context context
     *
     * @return DatabaseBackend instance
     */
    public static synchronized DatabaseBackend getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseBackend(HymnchtvDatabase.getInstance(context));
        }
        return instance;
    }

    /**
     * Closes the database held by this facade. For tests only: the production singleton lives as long as the
     * process and must not be closed.
     */
    @VisibleForTesting
    public void close() {
        db.close();
    }

    /**
     * Run body in one transaction: committed when it returns, rolled back when it throws (the exception is
     * rethrown as is). Nested calls join the outer transaction; if a nested body throws, the WHOLE outer
     * transaction is rolled back even when the outer body catches the exception (pinned by
     * DatabaseBackendTransactionTest). Must not be called on the main thread for bulk work.
     */
    public <T> T inTransaction(@NonNull Supplier<T> body) {
        return db.runInTransaction(body::get);
    }

    public void runInTransaction(@NonNull Runnable body) {
        inTransaction(() -> {
            body.run();
            return null;
        });
    }

    private static boolean isKnownHymnType(String hymnType) {
        return hymnType != null && KNOWN_HYMN_TYPES.contains(hymnType);
    }

    private static boolean rejectHymnType(String hymnType, String operation) {
        if (isKnownHymnType(hymnType)) {
            return false;
        }
        Timber.e("### %s rejected unknown hymnType: %s", operation, hymnType);
        return true;
    }

    /** Room raises SQLite errors as android.database.sqlite.SQLiteException, a subclass of SQLException. */
    private static boolean isSqlError(RuntimeException e) {
        return e instanceof SQLException;
    }

    private static MediaRecordEntity toEntity(MediaRecord mRecord) {
        return new MediaRecordEntity(mRecord.getHymnType(), mRecord.getHymnNo(), mRecord.isFu(),
                mRecord.getMediaType().toString(), mRecord.getMediaUri(), mRecord.getMediaFilePath());
    }

    private static MediaRecord toRecord(MediaRecordEntity entity) {
        return new MediaRecord(entity.getHymnType(), entity.getHymnNo(), entity.isFu(),
                Enum.valueOf(MediaType.class, entity.getMediaType()), entity.getMediaUri(), entity.getMediaFilePath());
    }

    private static List<MediaRecord> toRecords(List<MediaRecordEntity> entities) {
        List<MediaRecord> mediaRecords = new ArrayList<>(entities.size());
        for (MediaRecordEntity entity : entities) {
            mediaRecords.add(toRecord(entity));
        }
        return mediaRecords;
    }

    /**
     * Save the given MediaRecord (a record with the same hymnType, hymnNo, isFu and mediaType is overwritten).
     * SQL errors are logged and reported as -1 (legacy behaviour, kept for single-record UI saves); an unknown
     * hymnType is rejected the same way. Run on AppExecutors.io.
     */
    public long storeMediaRecord(MediaRecord mRecord) {
        if (rejectHymnType(mRecord.getHymnType(), "storeMediaRecord")) {
            return -1L;
        }
        long row;
        try {
            row = db.mediaRecordDao().insert(toEntity(mRecord));
        } catch (RuntimeException e) {
            if (!isSqlError(e)) {
                throw e;
            }
            Timber.e(e, "### Error in creating media record for table:hymNo: %s:%s", mRecord.getHymnType(), mRecord.getHymnNo());
            return -1L;
        }
        if (row == -1) {
            Timber.e("### Error in creating media record for table:hymNo: %s:%s", mRecord.getHymnType(), mRecord.getHymnNo());
        }
        return row;
    }

    /**
     * Same as storeMediaRecord, but SQL errors (and an unknown hymnType) propagate as
     * android.database.SQLException. Bulk imports use this inside inTransaction() so one failure rolls the
     * whole batch back. Run on AppExecutors.io.
     */
    public long storeMediaRecordOrThrow(MediaRecord mRecord) {
        if (rejectHymnType(mRecord.getHymnType(), "storeMediaRecordOrThrow")) {
            throw new SQLException("Unknown hymnType: " + mRecord.getHymnType());
        }
        // SQLite errors propagate as SQLiteException (a SQLException) and are deliberately not caught here.
        long row = db.mediaRecordDao().insert(toEntity(mRecord));
        if (row == -1) {
            throw new SQLException("Failed to store media record " + mRecord.getHymnType() + ":" + mRecord.getHymnNo());
        }
        return row;
    }

    /**
     * Check if mRecord exist in DB and update with the DB result if update if true. Run on AppExecutors.io
     * (transitional main-thread caller: ContentHandler.getHymnMediaState, see class doc).
     *
     * @param mRecord Media record to check for
     * @param update Update mRecord if true, else just return the status; i.e just to check if exist in DB
     *
     * @return mRecord present status, and mRecord is updated if update is true;
     */
    public boolean getMediaRecord(MediaRecord mRecord, boolean update) {
        if (rejectHymnType(mRecord.getHymnType(), "getMediaRecord")) {
            return false;
        }
        MediaRecordEntity entity = db.mediaRecordDao().find(mRecord.getHymnType(), mRecord.getHymnNo(),
                mRecord.isFu(), mRecord.getMediaType().toString());
        if (entity == null) {
            return false;
        }
        if (update) {
            mRecord.setMediaUri(entity.getMediaUri());
            mRecord.setFilePath(entity.getMediaFilePath());
        }
        return true;
    }

    /**
     * Get the URL for the given media record; currently use for QQ Hymn page access
     * Force url to a secure access link, android does not allow clearText web access.
     * Run on AppExecutors.io.
     *
     * @param mRecord MediaRecord for URL fetch
     *
     * @return URL for the given mediaRecord and the update mRecord; null if there is none
     */
    public String getHymnUrl(MediaRecord mRecord) {
        String url = null;
        if (getMediaRecord(mRecord, true) && mRecord.getMediaUri() != null) {
            url = mRecord.getMediaUri().replace("http:", "https:");
            mRecord.setMediaUri(url);
        }
        return url;
    }

    /**
     * Delete the given mediaRecord. Run on AppExecutors.io.
     *
     * @param mRecord mediaRecord to be deleted
     *
     * @return No of matched records get deleted
     */
    public int deleteMediaRecord(MediaRecord mRecord) {
        if (rejectHymnType(mRecord.getHymnType(), "deleteMediaRecord")) {
            return 0;
        }
        return db.mediaRecordDao().delete(mRecord.getHymnType(), mRecord.getHymnNo(), mRecord.isFu(),
                mRecord.getMediaType().toString());
    }

    /**
     * Get the media records for the given hymnType, ordered by hymnNo, isFu, mediaType. Run on AppExecutors.io.
     *
     * @param hymnType one of the MediaConfig.hymnTypeValue
     *
     * @return List of mediaRecords for the given hymnType (empty for an unknown hymnType)
     */
    public List<MediaRecord> getMediaRecords(String hymnType) {
        if (rejectHymnType(hymnType, "getMediaRecords")) {
            return new ArrayList<>();
        }
        return toRecords(db.mediaRecordDao().listByType(hymnType));
    }

    /**
     * Get all the media records (every media type) of one hymn. Run on AppExecutors.io.
     *
     * @return List of mediaRecords of the hymn (empty for an unknown hymnType)
     */
    public List<MediaRecord> getMediaRecords(String hymnType, int hymnNo, boolean isFu) {
        if (rejectHymnType(hymnType, "getMediaRecords")) {
            return new ArrayList<>();
        }
        return toRecords(db.mediaRecordDao().listByHymn(hymnType, hymnNo, isFu));
    }

    /**
     * Get the media records which contain valid links. Run on AppExecutors.io.
     *
     * @param hymnType one of the MediaConfig.hymnTypeValue
     *
     * @return List of mediaRecords for the given hymnType (empty for an unknown hymnType)
     */
    public List<MediaRecord> getMediaLinks(String hymnType) {
        if (rejectHymnType(hymnType, "getMediaLinks")) {
            return new ArrayList<>();
        }
        return toRecords(db.mediaRecordDao().linksByType(hymnType));
    }

    /**
     * Save the given HistoryRecord to the table hymn_history.
     * Purge old records in excess of (NUMBER_OF_RECORDS_IN_HISTORY - 10), purge and insert in one transaction.
     * Run on AppExecutors.io.
     *
     * @param mRecord an instance of HistoryRecord
     */
    public void storeHymnHistory(HistoryRecord mRecord) {
        HymnHistoryEntity entity = new HymnHistoryEntity(mRecord.getHymnType(), mRecord.getHymnNo(), mRecord.isFu(),
                mRecord.getHymnTitle(), mRecord.getTimeStamp());
        try {
            int purged = db.hymnHistoryDao().purgeAndInsert(entity, HistoryRecord.NUMBER_OF_RECORDS_IN_HISTORY);
            Timber.d("No of old history deleted : %s", purged);
        } catch (RuntimeException e) {
            if (!isSqlError(e)) {
                throw e;
            }
            Timber.e(e, "### Error in creating history record HymnType#hymNo: %s#%s", mRecord.getHymnType(), mRecord.getHymnNo());
        }
    }

    /**
     * Delete the given HistoryRecord in the table hymn_history. As before, matches on hymnType and hymnNo only
     * (isFu is ignored), so the Fu and the non-Fu row of a number go together.
     * Run on AppExecutors.io (transitional main-thread caller: MainActivity history row delete, see class doc).
     *
     * @param mRecord an instance of HistoryRecord
     */
    public int deleteHymnHistory(HistoryRecord mRecord) {
        return db.hymnHistoryDao().deleteByNumber(mRecord.getHymnType(), mRecord.getHymnNo());
    }

    /**
     * Fetch a list of the history record from hymn_history table for user selection, newest first.
     * Run on AppExecutors.io (transitional main-thread caller: MainActivity.initHistoryList, see class doc).
     *
     * @return List of HistoryRecord
     */
    public List<HistoryRecord> getHistoryRecords() {
        List<HymnHistoryEntity> entities = db.hymnHistoryDao().listNewestFirst();
        List<HistoryRecord> historyRecords = new ArrayList<>(entities.size());
        for (HymnHistoryEntity entity : entities) {
            historyRecords.add(new HistoryRecord(entity.getHymnType(), entity.getHymnNo(), entity.isFu(),
                    entity.getHymnTitle(), entity.getTimeStamp()));
        }
        return historyRecords;
    }

    /**
     * Save the English lyrics of a hymn (an existing one is overwritten); -1 if it could not be saved.
     * Run on AppExecutors.io.
     *
     * @param hymnNoEng English hymnNo
     * @param lyrics string containing html lyrics
     */
    public long storeLyricsEng(int hymnNoEng, String lyrics) {
        long row = -1L;
        try {
            row = db.englishLyricsDao().insert(new EnglishLyricsEntity(hymnNoEng, lyrics));
        } catch (RuntimeException e) {
            if (!isSqlError(e)) {
                throw e;
            }
            Timber.e(e, "Failed to save English lyrics for hymnNo: %s", hymnNoEng);
        }
        if (row == -1) {
            Timber.e("### Error in saving Url record for hymnNo English: %s", hymnNoEng);
        }
        return row;
    }

    /**
     * Delete the English lyrics of the given hymn. Run on AppExecutors.io (transitional main-thread caller:
     * ContentView long press, see class doc).
     *
     * @param hymnNoEng English hymnNo
     */
    public int deleteLyricsEng(int hymnNoEng) {
        return db.englishLyricsDao().delete(hymnNoEng);
    }

    /**
     * Fetch the English lyrics of the given hymn; null if there are none. Run on AppExecutors.io.
     *
     * @param hymnNoEng English hymnNo
     */
    public String getLyricsEnglish(int hymnNoEng) {
        return db.englishLyricsDao().lyrics(hymnNoEng);
    }

    /** The Room database behind this facade, for tests that inject failures or inspect pragmas. */
    @VisibleForTesting
    public HymnchtvDatabase roomDatabase() {
        return db;
    }
}
