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

package org.cog.hymnchtv.service.androidupdate;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.MainActivity;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.update.ApkCheck;
import org.cog.hymnchtv.update.ApkVerifier;
import org.cog.hymnchtv.update.GitHubReleaseClient;
import org.cog.hymnchtv.update.ReleaseInfo;
import org.cog.hymnchtv.update.ReleaseConvention;
import org.cog.hymnchtv.update.ReleaseNotesFormatter;
import org.cog.hymnchtv.update.SemVer;
import org.cog.hymnchtv.update.UpdateCheckResult;
import org.cog.hymnchtv.update.UpdateEndpointsLoader;
import org.cog.hymnchtv.update.UpdateHttp;
import org.cog.hymnchtv.update.UpdateInstallActivity;
import org.cog.hymnchtv.update.UpdateNotifier;
import org.cog.hymnchtv.utils.CustomDialogWv;
import org.cog.hymnchtv.utils.DialogActivity;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * App update via GitHub Releases (sub-project Z). Checks releases/latest, downloads hymnal-X.Y.Z.apk with
 * DownloadManager, verifies it in the background (published SHA-256, package, version code, signing certificate)
 * and posts a notification; the user's tap opens UpdateInstallActivity. The download receiver never starts
 * activities. Every public method except {@link #removeOldDownloads()} does network or file I/O: call off the main thread.
 * The download-id store has its own lock ({@code storeLock}), so store access never waits for {@link #fetchLatest()}'s network calls.
 * Since 1.6.0 the release and SHA-256 of the download in progress are saved with its id (PendingDownload): a download that
 * finished while the process was dead is verified at the next start ({@link #resumeOrCleanOnStart()}) instead of deleted.
 *
 * @author Eng Chong Meng
 */
public class UpdateServiceImpl {
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    /** SharedPreferences entry holding enqueued download ids, separated by ",". */
    private static final String ENTRY_NAME = "apk_ids";

    private static UpdateServiceImpl mInstance = null;

    /**
     * Guards the download-id store only. Deliberately not the instance monitor: {@link #fetchLatest()} holds
     * that during network calls (up to 60 s), and the store is touched from the main thread
     * (downloadApk) and from DownloadReceiver.onReceive, which must never wait for the network.
     */
    private final Object storeLock = new Object();

    /** Guards {@code downloadReceiver}; separate from the instance monitor for the same reason as {@code storeLock}. */
    private final Object receiverLock = new Object();

    /** The id being verified, or -1: the start-up recovery and the receiver may both see one finished download. */
    private final AtomicLong verifyingId = new AtomicLong(-1L);

    private GitHubReleaseClient releaseClient = null;
    private volatile Offer latestOffer = null;
    private volatile String currentVersion = null;
    private DownloadReceiver downloadReceiver = null;
    private SharedPreferences store;

    /** A release together with the SHA-256 published for it; always replaced as one unit. */
    private static final class Offer {
        final ReleaseInfo release;
        final String sha256;

        Offer(ReleaseInfo release, String sha256) {
            this.release = release;
            this.sha256 = sha256;
        }
    }

    public static synchronized UpdateServiceImpl getInstance() {
        if (mInstance == null) {
            mInstance = new UpdateServiceImpl();
        }
        return mInstance;
    }

    /**
     * User-initiated check (About "Update", main screen update button). Call off the main thread.
     */
    public void checkForUpdates() {
        UpdateCheckResult result = fetchLatest();
        ReleaseInfo release = result.getRelease();
        if (result instanceof UpdateCheckResult.Available && release != null) {
            Offer offer = latestOffer;
            if (offer != null && offer.release == release && offer.sha256 != null) {
                offerUpdate(offer);
            }
            else {
                HymnsApp.showToastMessage(R.string.update_check_failed);
            }
        }
        else if (result instanceof UpdateCheckResult.UpToDate && release != null) {
            DialogActivity.showDialog(HymnsApp.getGlobalContext(), R.string.app_update_none,
                    R.string.update_up_to_date, currentVersion, release.getVersionName());
        }
        else if (result instanceof UpdateCheckResult.RateLimited) {
            HymnsApp.showToastMessage(R.string.update_check_rate_limited);
        }
        else if (result instanceof UpdateCheckResult.NetworkError) {
            HymnsApp.showToastMessage(R.string.update_check_network_error);
        }
        else if (result instanceof UpdateCheckResult.NoRelease) {
            HymnsApp.showToastMessage(R.string.update_check_no_release);
        }
        else {
            HymnsApp.showToastMessage(R.string.update_check_failed);
        }
    }

    /**
     * @return false only when a newer, checksum-published release is confirmed; failures count as "latest".
     */
    public boolean isLatestVersion() {
        return !(fetchLatest() instanceof UpdateCheckResult.Available);
    }

    /**
     * @return notification text for the latest release; valid after {@link #isLatestVersion()} returned false.
     */
    public String getLatestVersion() {
        Offer offer = latestOffer;
        ReleaseInfo release = (offer == null) ? null : offer.release;
        return HymnsApp.getResString(R.string.update_notification_text,
                (release == null) ? "" : release.getVersionName());
    }

    private synchronized UpdateCheckResult fetchLatest() {
        Context context = HymnsApp.getGlobalContext();
        currentVersion = VersionServiceImpl.getInstance().getCurrentVersionName();
        if (releaseClient == null) {
            releaseClient = new GitHubReleaseClient(UpdateHttp.client());
        }
        UpdateCheckResult result = releaseClient.fetchLatest(UpdateEndpointsLoader.current(context), currentVersion);
        ReleaseInfo release = result.getRelease();
        String sha = null;
        if (result instanceof UpdateCheckResult.Available && release != null) {
            sha = releaseClient.fetchExpectedSha256(release);
            if (sha == null) {
                result = new UpdateCheckResult.Failed("missing or inconsistent " + release.getApkName() + ".sha256");
            }
        }
        latestOffer = (release == null) ? null : new Offer(release, sha);
        MainActivity.mHasUpdate = result instanceof UpdateCheckResult.Available;
        Timber.i("Update check: installed %s -> %s %s", currentVersion, result.getClass().getSimpleName(),
                (release == null) ? "" : release.getTag());
        return result;
    }

    private void offerUpdate(Offer offer) {
        ReleaseInfo release = offer.release;
        Context context = HymnsApp.getGlobalContext();
        if (ApkVerifier.staged(context, release) != null) {
            // Verified earlier and still waiting: the user asked for it, the app is in the foreground.
            context.startActivity(UpdateInstallActivity.intent(context, release.getApkName(), offer.sha256));
            return;
        }
        if (isDownloadRunning()) {
            DialogActivity.showDialog(context, R.string.in_progress, R.string.download_in_progress);
            return;
        }
        Bundle args = new Bundle();
        args.putString(CustomDialogWv.ARG_MESSAGE,
                context.getString(R.string.update_new_available, release.getVersionName(), currentVersion));
        args.putString(CustomDialogWv.ARG_HISTORY,
                ReleaseNotesFormatter.toHtml(release.getNotes(), context.getString(R.string.update_none)));

        DialogActivity.showCustomDialog(context, context.getString(R.string.app_update_install),
                CustomDialogWv.class.getName(), args, context.getString(R.string.download),
                new DialogActivity.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        downloadApk(offer);
                        return true;
                    }

                    @Override
                    public void onDialogCancelled(@NotNull DialogActivity dialog) {
                    }
                }, null);
    }

    private boolean isDownloadRunning() {
        List<Long> ids = getOldDownloads();
        if (ids.isEmpty()) {
            return false;
        }
        int status = checkDownloadStatus(ids.get(ids.size() - 1));
        return status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING
                || status == DownloadManager.STATUS_PAUSED;
    }

    /**
     * Schedules the apk download into the app-specific Download directory (no storage permission needed).
     */
    private void downloadApk(Offer offer) {
        ReleaseInfo release = offer.release;
        Context context = HymnsApp.getGlobalContext();
        File target = expectedDownloadFile(release);
        if (target == null) {
            HymnsApp.showToastMessage(R.string.download_failed);
            return;
        }
        removeOldDownloads();
        if (target.exists() && !target.delete()) {
            // DownloadManager would rename the new file (…-1.apk) and we would verify the stale one: abort.
            Timber.w("Cannot delete stale %s", target);
            HymnsApp.showToastMessage(R.string.download_failed);
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(release.getApkUrl()));
        request.setTitle(release.getApkName());
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setMimeType(APK_MIME_TYPE);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, release.getApkName());

        DownloadStart.run(new DownloadStart.Steps() {
            @Override
            public long enqueue() {
                return HymnsApp.getDownloadManager().enqueue(request);
            }

            @Override
            public void persist(long id) {
                rememberDownload(new PendingDownload(id, release.getTag(), release.getApkName(), offer.sha256));
            }

            @Override
            public void registerReceiver() {
                registerDownloadReceiver(release, offer.sha256);
            }

            @Override
            public int status(long id) {
                return checkDownloadStatus(id);
            }

            @Override
            public void verifyNow(long id) {
                unregisterDownloadReceiver();
                new Thread(() -> verifyFinishedDownload(id, release, offer.sha256), "hymnal-apk-verify").start();
            }
        });
    }

    private void registerDownloadReceiver(ReleaseInfo release, String sha256) {
        synchronized (receiverLock) {
            unregisterDownloadReceiver();
            downloadReceiver = new DownloadReceiver(release, sha256);
            // DownloadManager broadcasts from another process, so the receiver must be exported. A forged broadcast
            // can only carry an id; onReceive ignores any id that is not our latest enqueued download. Spoofing our
            // own id at best starts verification early, which fails closed: denial of service only, never an install.
            ContextCompat.registerReceiver(HymnsApp.getGlobalContext(), downloadReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED);
        }
    }

    private void unregisterDownloadReceiver() {
        synchronized (receiverLock) {
            if (downloadReceiver != null) {
                HymnsApp.getGlobalContext().unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        }
    }

    /**
     * Runs on a worker thread after the download finished. Never starts an activity.
     */
    private void verifyFinishedDownload(long id, ReleaseInfo release, String sha256) {
        // Verify each finished download once: it is no longer listed after the first run, or another thread is on it
        if (!getOldDownloads().contains(id) || !verifyingId.compareAndSet(-1L, id)) {
            return;
        }
        Context context = HymnsApp.getGlobalContext();
        try {
            if (checkDownloadStatus(id) != DownloadManager.STATUS_SUCCESSFUL || sha256 == null) {
                HymnsApp.showToastMessage(R.string.download_failed);
                return;
            }
            File downloaded = expectedDownloadFile(release);
            if (downloaded == null || !downloaded.isFile()) {
                HymnsApp.showToastMessage(R.string.download_failed);
                return;
            }
            ApkVerifier.Result result = ApkVerifier.verifyAndStage(context, downloaded, release, sha256);
            if (result.getCheck() == ApkCheck.SIGNER_UNVERIFIABLE) {
                HymnsApp.showToastMessage(R.string.update_signer_unverifiable);
            }
            else if (result.getCheck() != ApkCheck.OK) {
                HymnsApp.showToastMessage(R.string.update_apk_invalid, result.getCheck().name());
            }
            else if (!UpdateNotifier.showReady(context, release.getApkName(), release.getVersionName(), sha256)) {
                HymnsApp.showToastMessage(R.string.update_ready_use_about);
            }
            else {
                HymnsApp.showToastMessage(R.string.update_downloaded_tap_notification);
            }
        }
        finally {
            removeOldDownloads();
            verifyingId.set(-1L);
        }
    }

    private class DownloadReceiver extends BroadcastReceiver {
        private final ReleaseInfo release;
        private final String sha256;

        DownloadReceiver(ReleaseInfo release, String sha256) {
            this.release = release;
            this.sha256 = sha256;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            List<Long> ids = getOldDownloads();
            if (ids.isEmpty() || ids.get(ids.size() - 1) != id) {
                return;
            }
            unregisterDownloadReceiver();
            PendingResult pending = goAsync();
            new Thread(() -> {
                try {
                    verifyFinishedDownload(id, release, sha256);
                }
                finally {
                    pending.finish();
                }
            }, "hymnal-apk-verify").start();
        }
    }

    @SuppressLint("Range")
    private int checkDownloadStatus(long id) {
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(id);
        try (Cursor cursor = HymnsApp.getDownloadManager().query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return DownloadManager.STATUS_FAILED;
            }
            return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        }
    }

    /**
     * The fixed download destination (app-specific Download dir + hymnal-X.Y.Z.apk). DownloadManager's
     * COLUMN_LOCAL_URI is not used: it may be a content:// uri on some versions.
     *
     * @return the file, or null when the directory is unavailable or the name escapes it
     */
    @Nullable
    private static File expectedDownloadFile(ReleaseInfo release) {
        File dir = HymnsApp.getGlobalContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null || !ReleaseConvention.isApkName(release.getApkName())) {
            return null;
        }
        try {
            File file = new File(dir, release.getApkName()).getCanonicalFile();
            String root = dir.getCanonicalPath() + File.separator;
            return file.getPath().startsWith(root) ? file : null;
        }
        catch (IOException e) {
            Timber.w(e, "Cannot resolve download path");
            return null;
        }
    }

    private SharedPreferences getStore() {
        synchronized (storeLock) {
            if (store == null) {
                store = HymnsApp.getGlobalContext().getSharedPreferences("store", Context.MODE_PRIVATE);
            }
            return store;
        }
    }

    /** Appends the id and saves its record in one synchronous write: the receiver must find the id once it listens. */
    private void rememberDownload(PendingDownload pending) {
        synchronized (storeLock) {
            SharedPreferences prefs = getStore();
            prefs.edit()
                    .putString(ENTRY_NAME, prefs.getString(ENTRY_NAME, "") + pending.getId() + ",")
                    .putString(PendingDownload.PREF_KEY, pending.encode())
                    .commit();
        }
    }

    /**
     * At start-up, on AppExecutors.io (replaces the plain {@link #removeOldDownloads()} call). A download recorded by the
     * last process that finished meanwhile is verified now; one still running gets its receiver back; anything else is
     * removed as before. See DownloadRecovery.
     */
    public void resumeOrCleanOnStart() {
        PendingDownload pending = PendingDownload.decode(getStore().getString(PendingDownload.PREF_KEY, null));
        if (pending == null) {
            removeOldDownloads();
            return;
        }
        List<Long> ids = getOldDownloads();
        boolean latest = !ids.isEmpty() && ids.get(ids.size() - 1) == pending.getId();
        ReleaseInfo release = pending.toRelease();
        File file = expectedDownloadFile(release);
        int status = checkDownloadStatus(pending.getId());
        SemVer installed = SemVer.parse(VersionServiceImpl.getInstance().getCurrentVersionName());
        switch (DownloadRecovery.decide(latest, status, file != null && file.isFile(), release.getVersion(), installed)) {
            case VERIFY:
                Timber.w("Verifying %s, downloaded while the app was not running", release.getApkName());
                verifyFinishedDownload(pending.getId(), release, pending.getSha256());
                break;
            case WAIT:
                registerDownloadReceiver(release, pending.getSha256());
                // it may have finished between the query and the registration
                if (checkDownloadStatus(pending.getId()) == DownloadManager.STATUS_SUCCESSFUL) {
                    unregisterDownloadReceiver();
                    verifyFinishedDownload(pending.getId(), release, pending.getSha256());
                }
                break;
            default:
                removeOldDownloads();
        }
    }

    private List<Long> getOldDownloads() {
        String storeStr = getStore().getString(ENTRY_NAME, "");
        String[] idStrs = storeStr.split(",");
        List<Long> apkIds = new ArrayList<>(idStrs.length);
        for (String idStr : idStrs) {
            try {
                if (!idStr.isEmpty()) {
                    apkIds.add(Long.parseLong(idStr));
                }
            }
            catch (NumberFormatException e) {
                Timber.e("Error parsing apk id for string: %s [%s]", idStr, storeStr);
            }
        }
        return apkIds;
    }

    /**
     * Removes old update downloads (DownloadManager deletes their files) and staged apks that are already
     * installed. Called at app start-up.
     */
    public void removeOldDownloads() {
        DownloadManager downloadManager = HymnsApp.getDownloadManager();
        synchronized (storeLock) {
            for (long id : getOldDownloads()) {
                downloadManager.remove(id);
            }
            getStore().edit().remove(ENTRY_NAME).remove(PendingDownload.PREF_KEY).apply();
        }

        SemVer installed = SemVer.parse(VersionServiceImpl.getInstance().getCurrentVersionName());
        File[] staged = ApkVerifier.updatesDir(HymnsApp.getGlobalContext()).listFiles();
        if (staged != null && installed != null) {
            for (File apk : staged) {
                SemVer version = SemVer.parse(apk.getName().replaceFirst("^hymnal-", "").replaceFirst("\\.apk$", ""));
                if (version == null || version.compareTo(installed) <= 0) {
                    Timber.d("Deleting staged %s", apk.getName());
                    apk.delete();
                }
            }
        }
    }
}
