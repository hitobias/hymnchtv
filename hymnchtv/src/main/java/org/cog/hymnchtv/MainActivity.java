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
package org.cog.hymnchtv;

import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_DUMMY;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.os.PowerManager;
import android.app.KeyguardManager;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.IntentCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;


import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.cog.hymnchtv.concurrent.AppExecutors;
import org.cog.hymnchtv.hymnhistory.HistoryRecord;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.reading.ReadingPrefKeys;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.toc.YbCrossRef;
import org.cog.hymnchtv.ui.host.MainChrome;
import org.cog.hymnchtv.ui.host.MainHost;
import org.cog.hymnchtv.ui.host.MainNavigator;
import org.cog.hymnchtv.ui.motion.Motion;
import org.cog.hymnchtv.utils.DialogActivity;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

/**
 * MainActivity: the hymnchtv app main user interface. It only hosts the home page and the pages opened from it (contents,
 * settings, search, history; see {@link MainHost}), which are fragments under {@code ui/}. It also keeps the static entry points other classes use
 * to open lyrics, and handles the share intents.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class MainActivity extends BaseActivity implements LifecycleEventObserver,
        ActivityCompat.OnRequestPermissionsResultCallback, MainNavigator {
    public static final String ATTR_HYMN_TYPE = "hymn_type";
    public static final String ATTR_HYMN_NUMBER = "hymn_number";
    public static final String ATTR_MEDIA_URI = "media_uri";
    public static final String ATTR_MEDIA_TYPE = "media_type";

    public static final String ATTR_SEARCH = "search";
    public static final String ATTR_PAGE = "page";
    public static final String ATTR_AUTO_PLAY = "autoPlay";
    public static final String ATTR_ENGLISH_NO = "englishNo";

    public static final String HYMN_DB = "hymn_db";
    public static final String HYMN_BB = "hymn_bb";
    public static final String HYMN_ER = "hymn_er";
    public static final String HYMN_XB = "hymn_xb";
    public static final String HYMN_XG = "hymn_xg";
    public static final String HYMN_YB = "hymn_yb";

    public static final String PREF_MENU_SHOW = ReadingPrefKeys.MENU_SHOW;
    public static final String PREF_SETTINGS = "Settings";

    public static final String PREF_MEDIA_HYMN = "MediaHymn";

    public static final Map<Integer, Integer> HYMN_YB_ALT = Map.of(
            104, 272,
            148, 273,
            150, 274,
            151, 275
    );

    // A cross-reference table for YB hymn
    // YB -> other hymn book cross-reference: loaded lazily and thread-safely, prewarmed off the main thread in onCreate.
    public static final Map<Integer, String> ybXTable = YbCrossRef.TABLE;

    private static String mHymnType = HYMN_DB;
    private static int mHymnNo = -1;

    public static boolean mHasUpdate = false;
    /**
     * Indicate if aTalk is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    private static MainActivity mInstance;

    private MainHost mainHost;

    /** Delayed work that captures this activity; cleared in onDestroy so a recreated activity is not kept alive. */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onCreate(Bundle savedInstanceState) {
        mInstance = this;
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        // DayNight is applied globally by HymnsApp (ThemePrefs.applyStored)
        super.onCreate(savedInstanceState);
        org.cog.hymnchtv.ui.theme.SystemBars.enable(this);
        Motion.applyHostWindow(getWindow(), this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        setContentView(R.layout.activity_main_host);
        MainChrome.installInsets(findViewById(R.id.viewMain));
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_title_main);
        }

        mainHost = new MainHost(this);
        mainHost.attach(savedInstanceState);
        MainChrome.apply(this, getSharedPreferences(PREF_SETTINGS, 0));

        AppExecutors.io("yb-xref-prewarm", YbCrossRef::prewarm);

        // Request all the permissions required by Hymnchtv; only valid if user does not manually disallow it.
        PermissionUtils.checkHymnPermissionAndRequest(this);

        // allow 15 seconds for first launch login to complete before showing history log if the activity is still active
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            runOnUiThread(() -> mHandler.postDelayed(() -> {
                // a recreated (e.g. rotated) activity is not finishing but its window token is already gone
                if (!isFinishing() && !isDestroyed()) {
                    cl.getLogDialog().show();
                }
            }, 15000));
        }

        handleIntent(getIntent());
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    /**
     * Called when new <tt>Intent</tt> is received(this <tt>Activity</tt> is launched in <tt>singleTask</tt> mode.
     *
     * @param intent new <tt>Intent</tt> data.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Handle share intent to extract the share text content or URIS
     *
     * @param intent <tt>Activity</tt> <tt>Intent</tt>.
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        final String action = intent.getAction();
        final String type = intent.getType();

        String mediaLink = null;
        if (Intent.ACTION_SEND.equals(action) && (type != null)) {
            if ("text/plain".equals(type)) {
                mediaLink = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
            else {
                mediaLink = getFile(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class));
            }
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && (type != null)) {
            final ArrayList<Uri> uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            if (uris != null && !uris.isEmpty())
                mediaLink = getFile(uris.get(0));
            else
                HymnsApp.showToastMessage(R.string.file_does_not_exist);
        }

        if (mediaLink != null) {
            intent = new Intent(this, MediaConfig.class);
            Bundle bundle = new Bundle();
            bundle.putString(ATTR_MEDIA_URI, mediaLink);
            bundle.putString(ATTR_HYMN_TYPE, mHymnType);
            bundle.putInt(ATTR_HYMN_NUMBER, mHymnNo);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    @Override
    public void openToc(@NonNull String book, @NonNull String page) {
        if (mainHost != null) {
            mainHost.openToc(book, page);
        }
    }

    @Override
    public void openSettings() {
        if (mainHost != null) {
            mainHost.openSettings();
        }
    }

    @Override
    public void openHistory() {
        if (mainHost != null) {
            mainHost.openHistory();
        }
    }

    @Override
    public void openSearch(String book) {
        if (mainHost != null) {
            mainHost.openSearch(book);
        }
    }

    /** The top bar's back arrow (shown on every page but the home page) does what the back key does. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The home background or the theme may have changed in the settings tab or a picker meanwhile
        MainChrome.apply(this, getSharedPreferences(PREF_SETTINGS, 0));
        // An update found by the checker is flagged on the settings button, where "check for updates" lives
        mainHost.setUpdateAvailable(mHasUpdate);
    }

    @Override
    protected void onDestroy() {
        // The process lifecycle outlives every activity: without this, each recreated (e.g. rotated) MainActivity
        // stays reachable with its whole tab host, until a small heap (48 MB on API 24 phones) runs out.
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public static MainActivity getInstance() {
        return mInstance;
    }

    // ========= LifecycleEventObserver implementations ======= //
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (Lifecycle.Event.ON_START == event) {
            isForeground = true;
            Timber.d("APP FOREGROUNDED");
        }
        else if (Lifecycle.Event.ON_STOP == event) {
            isForeground = false;
            Timber.d("APP BACKGROUNDED");
        }
    }

    /**
     * Returns true if the device is locked or screen turned off (in case password not set)
     */
    public static boolean isDeviceLocked() {
        boolean isLocked;

        // First we check the locked state
        KeyguardManager keyguardManager = (KeyguardManager) mInstance.getSystemService(Context.KEYGUARD_SERVICE);
        boolean inKeyguardRestrictedInputMode = keyguardManager.isKeyguardLocked();

        if (inKeyguardRestrictedInputMode) {
            isLocked = true;
        }
        else {
            // If password is not set in the settings, the inKeyguardRestrictedInputMode() returns false,
            // so we need to check if screen on for this case
            PowerManager powerManager = (PowerManager) mInstance.getSystemService(Context.POWER_SERVICE);
            isLocked = !powerManager.isInteractive();
        }
        Timber.d("Android device is %s.", isLocked ? "locked" : "unlocked");
        return isLocked;
    }

    /** @return local path of the shared file (a fresh copy for content uris), or null after telling the user */
    private String getFile(Uri uri) {
        String path = (uri == null) ? null : FilePathHelper.getFilePath(this, uri);
        if (path != null && new File(path).exists()) {
            return path;
        }
        HymnsApp.showToastMessage(R.string.file_does_not_exist);
        return null;
    }

    /**
     * Save the user selected hymn into the history table, and
     * Show the content of user selected hymnType and hymnNo
     *
     * @param ctx Context
     * @param hymnType lyrics content of the hymnType
     * @param hymnNo the content of hymnNo to display
     * @param autoPlay start media playback if true after the lyrics content is shown
     * @param engNo optional english hymn no to show if present
     */
    public static void showContent(Context ctx, String hymnType, int hymnNo, boolean autoPlay, Integer... engNo) {
        showContent(ctx, hymnType, hymnNo, autoPlay, engNo.length == 0 ? -1 : engNo[0], null);
    }

    /**
     * Same as above, from an activity: the page opens with a fade, and with the hymn number flying from
     * {@code sharedNumber} (the home preview) when it is given; see Motion.
     */
    public static void showContent(Activity activity, String hymnType, int hymnNo, boolean autoPlay, int engNo, View sharedNumber) {
        showContent((Context) activity, hymnType, hymnNo, autoPlay, engNo, sharedNumber);
    }

    private static void showContent(Context ctx, String hymnType, int hymnNo, boolean autoPlay, int engNo, View sharedNumber) {
        // Save the user selection into history record; the title lookup reads assets, so both run off the main thread
        if (HYMN_BB_DUMMY != hymnNo) {
            boolean isFu = MediaRecord.isFu(hymnType, hymnNo);
            Context appContext = ctx.getApplicationContext();
            AppExecutors.io("store-history", () -> DatabaseBackend.getInstance(appContext)
                    .storeHymnHistory(new HistoryRecord(hymnType, hymnNo, isFu)));
        }

        Intent intent = new Intent(ctx, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_HYMN_TYPE, hymnType);
        bundle.putInt(ATTR_HYMN_NUMBER, hymnNo);
        bundle.putBoolean(ATTR_AUTO_PLAY, autoPlay);
        bundle.putInt(ATTR_ENGLISH_NO, engNo);
        bundle.putBoolean(Motion.EXTRA_SHARED_NUMBER, sharedNumber != null && Motion.enabled(ctx));

        intent.putExtras(bundle);
        Bundle options = ctx instanceof Activity ? Motion.contentOptions((Activity) ctx, sharedNumber) : null;
        ctx.startActivity(intent, options);
    }

    public static String getHymnType(String hymnTN) {
        if (hymnTN.startsWith("er")) {
            return HYMN_ER;
        }
        if (hymnTN.startsWith("xb")) {
            return HYMN_XB;
        }
        else if (hymnTN.startsWith("xg")) {
            return HYMN_XG;
        }
        else if (hymnTN.startsWith("yb")) {
            return HYMN_YB;
        }
        else {
            return hymnTN.startsWith("db") ? HYMN_DB : HYMN_BB;
        }
    }

    /**
     * The top page of the back stack closes first (overlay, then a full page); on the bare home page a second press within
     * two seconds closes the app (see {@link MainHost#onBackPressed()}).
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (!mainHost.onBackPressed()) {
                finish();
            }
        }
    };

    /**
     * Update both the hymnType and hymnNo for share auto-fill
     *
     * @param hymnType Update HymnType as given
     * @param hymnNo Update HymnNo as given
     */
    public static void setHymnTypeNo(String hymnType, int hymnNo) {
        mHymnType = hymnType;
        mHymnNo = hymnNo;
    }

    /**
     * Checks if the result contains a {@link PackageManager#PERMISSION_GRANTED} result for a
     * permission from a runtime permissions request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                String permission = permissions[i];
                String message = getResources().getString(R.string.permission_app_rational,
                        permission.substring(permission.lastIndexOf(".") + 1));

                if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                    message = getResources().getString(R.string.permission_storage_required);
                }
                else if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
                    message = getResources().getString(R.string.permission_notifications_required);
                }
                DialogActivity.showDialog(HymnsApp.getGlobalContext(),
                        getResources().getString(R.string.permission_request), message);
            }
        }
    }

    public void onInfoButtonClicked() {
        Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
        myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myAppSettings);
    }
}
