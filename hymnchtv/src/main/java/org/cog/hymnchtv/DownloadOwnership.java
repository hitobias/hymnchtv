package org.cog.hymnchtv;

import java.util.Map;

/**
 * DownloadManager broadcasts ACTION_DOWNLOAD_COMPLETE for every job enqueued by this app (media files and the
 * update apk alike), so each receiver must act only on the ids it enqueued itself.
 */
final class DownloadOwnership {
    private DownloadOwnership() {
    }

    /**
     * @return true when {@code id} is one of the download ids tracked in {@code ownDownloads}
     */
    static boolean owns(Map<Long, ?> ownDownloads, long id) {
        return ownDownloads.containsKey(id);
    }
}
