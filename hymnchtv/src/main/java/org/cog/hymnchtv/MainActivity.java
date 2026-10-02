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

import static org.cog.hymnchtv.HymnToc.TOC_ENGLISH;
import static org.cog.hymnchtv.HymnToc.hymnTocPage;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_DUMMY;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_YB_NO_MAX;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.core.view.ViewCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.LocaleManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.os.LocaleListCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.util.EncodingUtils;
import org.cog.hymnchtv.hymnhistory.HistoryRecord;
import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.reading.ReadingPrefKeys;
import org.cog.hymnchtv.reading.ReadingSettingsActivity;
import org.cog.hymnchtv.reading.background.BackgroundDrawables;
import org.cog.hymnchtv.reading.background.BackgroundPolicy;
import org.cog.hymnchtv.reading.background.BackgroundPrefs;
import org.cog.hymnchtv.reading.background.BackgroundSlot;
import org.cog.hymnchtv.reading.background.MainScreenColors;
import org.cog.hymnchtv.reading.background.ReadingPalette;
import org.cog.hymnchtv.persistance.FilePathHelper;
import org.cog.hymnchtv.persistance.PermissionUtils;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.DialogActivity;
import org.cog.hymnchtv.utils.HymnNoValidate;
import org.cog.hymnchtv.locale.AppLanguage;
import org.cog.hymnchtv.locale.LocaleStore;
import org.cog.hymnchtv.utils.MySwipeListAdapter;
import org.cog.hymnchtv.utils.ThemeHelper;
import org.cog.hymnchtv.utils.ThemeHelper.Theme;
import org.cog.hymnchtv.utils.TouchListener;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

/**
 * MainActivity: The hymnchtv app main user interface.
 *
 * @author Eng Chong Meng
 * @author wayfarer
 */
public class MainActivity extends BaseActivity implements AdapterView.OnItemSelectedListener, LifecycleEventObserver,
        ActivityCompat.OnRequestPermissionsResultCallback {
    public static String HYMNCHTV_FAQ = "https://cmeng-git.github.io/hymnchtv/faq.html";
    private final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());

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
    public static final String PREF_TEXT_COLOR = "TextColor";
    public static final String PREF_TEXT_SIZE = "TextSize";
    public static final String PREF_THEME = "Theme";
    public static final String PREF_LOCALE = "Locale";

    public static final String PREF_MEDIA_HYMN = "MediaHymn";
    private static final String mTocECFile = "lyrics_toc/toc_all_eng2ch.txt";
    public static final String mTocYB = "lyrics_toc/toc_yb_toc.txt";

    private static final int FONT_SIZE_DEFAULT = 35;

    public static final Map<Integer, Integer> HYMN_YB_ALT = Map.of(
            104, 272,
            148, 273,
            150, 274,
            151, 275
    );

    // A cross-reference table for YB hymn
    public static final Map<Integer, String> ybXTable = new HashMap<>();

    private static String mHymnType = HYMN_DB;
    private static int mHymnNo = -1;

    public static boolean mHasUpdate = false;
    /**
     * Indicate if aTalk is in the foreground (true) or background (false)
     */
    public static boolean isForeground = false;

    private Button btn_n0;
    private Button btn_n1;
    private Button btn_n2;
    private Button btn_n3;
    private Button btn_n4;
    private Button btn_n5;
    private Button btn_n6;
    private Button btn_n7;
    private Button btn_n8;
    private Button btn_n9;

    private Button btn_fu;
    private Button btn_del;

    private Button btn_db;
    private Button btn_bb;
    private Button btn_er;
    private Button btn_xb;
    private Button btn_xg;
    private Button btn_yb;

    private Button btn_search;
    private Button btn_update;
    private Button btn_english;

    private Spinner mTocSpinner;
    private MySwipeListAdapter<HistoryRecord> mHistoryAdapter;
    private ListView mHistoryListView;
    private TextView mTocSpinnerItem;
    private TextView mEntry;
    private EditText tv_Search;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

    private boolean autoClear = false;
    private boolean isFu = false;
    // Indicate that a TOC item has been selected
    private boolean isToc = false;

    private int mFontSize = FONT_SIZE_DEFAULT;
    private int mFsDelta = FONT_SIZE_DEFAULT - 10;

    // Default to a valid COLOR just in case (see initUserSettings()).
    private int mFontColor = Color.BLACK;

    // Palette of the main background actually on screen, and the search box's stock underline background
    private ReadingPalette mPalette = BackgroundPolicy.PHOTO_PALETTE;
    private Drawable mSearchDefaultBg;

    private String sNumber = "";
    private String mTocPage;

    private static MainActivity mInstance;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void onCreate(Bundle savedInstanceState) {
        mInstance = this;
        // Must setTheme() before super.onCreate(), otherwise not working
        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();

        String theme = mSharedPref.getString(PREF_THEME, ThemeHelper.DEFAULT_THEME.toString());
        setAppTheme(theme, false);

        super.onCreate(savedInstanceState);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        setContentView(R.layout.main);
        registerForContextMenu(findViewById(R.id.viewMain));

        mHistoryListView = findViewById(R.id.historyListView);
        mHistoryListView.setVisibility(View.GONE);

        initButton();
        if (savedInstanceState != null) {
            sNumber = savedInstanceState.getString(STATE_NUMBER, "");
            isFu = savedInstanceState.getBoolean(STATE_IS_FU, false);
            isToc = savedInstanceState.getBoolean(STATE_IS_TOC, false);
            autoClear = savedInstanceState.getBoolean(STATE_AUTO_CLEAR, false);
            mEntry.setText(savedInstanceState.getString(STATE_ENTRY_TEXT, sNumber));
        }
        initUserSettings();
        createYbXTable();

        // Request all the permissions required by Hymnchtv; only valid if user does not manually disallow it.
        PermissionUtils.checkHymnPermissionAndRequest(this);

        // allow 15 seconds for first launch login to complete before showing history log if the activity is still active
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
            runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isFinishing()) {
                    cl.getLogDialog().show();
                }
            }, 15000));

            /*
             * Disable importUrlAssetFile for on start; rely on updateServiceImpl instead.
             * Likely the DB has already been updated when user is prompt to update apk.
             * See MediaConfig#URL_IMPORT_VERSION value setting.
             */
            // MediaConfig.importUrlAssetFile();
        }

        // 儿童诗歌
        btn_er.setOnClickListener(v -> onHymnButtonClicked(HYMN_ER, false));
        // 补充本
        btn_bb.setOnClickListener(v -> onHymnButtonClicked(HYMN_BB, false));
        // 大本诗歌
        btn_db.setOnClickListener(v -> onHymnButtonClicked(HYMN_DB, false));
        // 新歌颂咏
        btn_xb.setOnClickListener(v -> onHymnButtonClicked(HYMN_XB, false));
        // 新詩歌本
        btn_xg.setOnClickListener(v -> onHymnButtonClicked(HYMN_XG, false));
        // 青年诗歌
        btn_yb.setOnClickListener(v -> onHymnButtonClicked(HYMN_YB, false));

        btn_yb.setOnLongClickListener(v -> {
            onHymnButtonClicked(HYMN_YB, true);
            return true;
        });

        // English Hymn
        btn_english.setOnClickListener(v -> {
            showHymnFromEng(false);
        });
        btn_english.setOnLongClickListener(v -> {
            showHymnFromEng(true);
            return true;
        });

        // Numeric number entry handlers for 0~9
        btn_n0.setOnClickListener(this::onNumberClick);
        btn_n1.setOnClickListener(this::onNumberClick);
        btn_n2.setOnClickListener(this::onNumberClick);
        btn_n3.setOnClickListener(this::onNumberClick);
        btn_n4.setOnClickListener(this::onNumberClick);
        btn_n5.setOnClickListener(this::onNumberClick);
        btn_n6.setOnClickListener(this::onNumberClick);
        btn_n7.setOnClickListener(this::onNumberClick);
        btn_n8.setOnClickListener(this::onNumberClick);
        btn_n9.setOnClickListener(this::onNumberClick);

        btn_fu.setOnClickListener(v -> {
            isFu = true;
            sNumber = "";
            autoClear = false;
            onNumberClick(v);
        });

        btn_del.setOnClickListener(v -> {
            sNumber = "";
            mEntry.setText(sNumber);
            isToc = false;
            isFu = false;
        });

        btn_search.setOnClickListener(v -> {
            String sValue = tv_Search.getText().toString();
            sValue = sValue.trim();
            if (TextUtils.isEmpty(sValue)) {
                HymnsApp.showToastMessage(R.string.error_search_empty);
                return;
            }

            Intent intent = new Intent();
            intent.setClass(this, ContentSearch.class);
            Bundle bundle = new Bundle();
            bundle.putString(ATTR_SEARCH, sValue);
            intent.putExtras(bundle);
            startActivity(intent);
        });

        btn_update.setOnClickListener(v -> {
            new Thread() {
                @Override
                public void run() {
                    UpdateServiceImpl.getInstance().checkForUpdates();
                }
            }.start();
        });

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
        super.onStart();
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
            if (uris != null)
                mediaLink = getFile(uris.get(0));
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

    private static final String STATE_NUMBER = "state_number";
    private static final String STATE_IS_FU = "state_is_fu";
    private static final String STATE_IS_TOC = "state_is_toc";
    private static final String STATE_AUTO_CLEAR = "state_auto_clear";
    private static final String STATE_ENTRY_TEXT = "state_entry_text";

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_NUMBER, sNumber);
        outState.putBoolean(STATE_IS_FU, isFu);
        outState.putBoolean(STATE_IS_TOC, isToc);
        outState.putBoolean(STATE_AUTO_CLEAR, autoClear);
        outState.putString(STATE_ENTRY_TEXT, mEntry.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        autoClear = true;
        if (mHasUpdate) {
            btn_update.setVisibility(View.VISIBLE);
            // adding the color to be shown
            ObjectAnimator animator = ObjectAnimator.ofInt(btn_update, "textColor", Color.BLUE, Color.RED, Color.GREEN);

            // duration of one color
            animator.setDuration(15000);
            animator.setEvaluator(new ArgbEvaluator());
            // color will be show in reverse manner
            animator.setRepeatCount(Animation.REVERSE);
            // It will be repeated up to infinite time
            animator.setRepeatCount(Animation.INFINITE);
            // animator.start(); stop animate
        }
        else {
            btn_update.setVisibility(View.GONE);
        }
        configureToolBar();
    }

    /**
     * Configure the main activity action bar using
     * a. Android actionBar
     * b. Customer action_bar layout
     */
    private void configureToolBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
                    | ActionBar.DISPLAY_USE_LOGO
                    | ActionBar.DISPLAY_SHOW_TITLE);

            // ensure actual logo size is ~64x64
            actionBar.setLogo(R.drawable.logo_hymnchtv);
            actionBar.setTitle(R.string.app_title_main);
        }
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

    private String getFile(Uri uri) {
        File inFile = new File(FilePathHelper.getFilePath(this, uri));
        if (inFile.exists()) {
            return inFile.getPath();
        }
        else
            HymnsApp.showToastMessage(R.string.file_does_not_exist);

        return null;
    }

    // 目录 Spinner selector handler
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTocPage = hymnTocPage.get(position);
        isToc = (position > 0);

        if (isToc) {
            sNumber = "";
            // mEntry.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
            mEntry.setText(mTocPage);
        }
        else if (TextUtils.isEmpty(sNumber)) {
            // mEntry.setFilters(new InputFilter[] { new InputFilter.LengthFilter(4) });
            mEntry.setText("");
        }
        initTocSpinnerItem();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * Routine to handle Fu and all numeric buttons click
     *
     * @param btnView fu and number buttons views
     */
    private void onNumberClick(View btnView) {
        // Auto clear sNumber to "" if this is first resume
        if (autoClear) {
            autoClear = false;
            isFu = false;
            sNumber = "";
        }

        sNumber = sNumber + ((Button) btnView).getText();
        mEntry.setText(sNumber);

        // Re-init TOC and Search Fields to default on hymn number entry
        if (sNumber.length() == 1) {
            mTocSpinner.setSelection(0);
            tv_Search.setText("");
        }
    }

    /**
     * Handler for user hymnType button clicks;
     * Show TOC is selected else the content for the hymnNo if valid
     *
     * @param hymnType the button being clicked
     */
    private void onHymnButtonClicked(String hymnType, boolean altSelect) {
        if (!isToc) {
            sNumber = mEntry.getText().toString();
            if (isFu) {
                sNumber = sNumber.substring(1);
            }
            if (TextUtils.isEmpty(sNumber)) {
                sNumber = "0";
            }

            int hymnNo = Integer.parseInt(sNumber);
            if (isFu) {
                hymnNo += HYMN_DB.equals(hymnType) ? HYMN_DB_NO_MAX : HYMN_YB_NO_MAX;
            }

            if (HYMN_YB.equals(hymnType) && altSelect) {
                Integer hymnNoAlt = HYMN_YB_ALT.get(hymnNo);
                if (hymnNoAlt != null) {
                    hymnNo = hymnNoAlt;
                }
            }

            int nui = HymnNoValidate.validateHymnNo(hymnType, hymnNo, isFu);
            if (nui != -1) {
                mHymnType = hymnType;
                mHymnNo = hymnNo;
                showContent(this, hymnType, nui, false);
            }
            // Only clear the user entry hymnNo if user entry is Fu and HymnType is not HYMN_DB
            else if (isFu && !hymnType.equals(HYMN_DB)) {
                sNumber = "";
                mEntry.setText(sNumber);
                isFu = false;
            }
            else {
                autoClear = true;
            }
        }
        else {
            showHymnToc(hymnType);
        }
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
        // Save the user selection into history record
        boolean isFu = MediaRecord.isFu(hymnType, hymnNo);
        HistoryRecord historyRecord = new HistoryRecord(hymnType, hymnNo, isFu);
        if (HYMN_BB_DUMMY != hymnNo) {
            DatabaseBackend.getInstance(ctx).storeHymnHistory(historyRecord);
        }

        Intent intent = new Intent(ctx, ContentHandler.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_HYMN_TYPE, hymnType);
        bundle.putInt(ATTR_HYMN_NUMBER, hymnNo);
        bundle.putBoolean(ATTR_AUTO_PLAY, autoPlay);
        bundle.putInt(ATTR_ENGLISH_NO, engNo.length == 0 ? -1 : engNo[0]);

        intent.putExtras(bundle);
        ctx.startActivity(intent);
    }

    /**
     * Show the TOC of the user selected hymn type
     *
     * @param hymnType Hymn Toc
     */
    private void showHymnToc(String hymnType) {
        // "英中对照" not implemented for HYMN_ER or HYMN_XB
        if (TOC_ENGLISH.equals(mTocPage) && (HYMN_ER.equals(hymnType) || HYMN_XB.equals(hymnType))) {
            HymnsApp.showToastMessage(R.string.en2ch_hymn_same);
            return;
        }

        Intent intent = new Intent(this, HymnToc.class);
        Bundle bundle = new Bundle();
        bundle.putString(ATTR_HYMN_TYPE, hymnType);
        bundle.putString(ATTR_PAGE, mTocPage);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * Generate the expandable TOC list from the given tocFile sorted by the stroke or pinyin
     *
     * @param dbPage true to access the DB page instead of BB if existed.
     */
    private void showHymnFromEng(boolean dbPage) {
        if (isToc) {
            return;
        }

        sNumber = mEntry.getText().toString();
        if (TextUtils.isEmpty(sNumber)) {
            sNumber = "0";
        }
        else if (isFu) {
            sNumber = sNumber.substring(1);
        }

        int hymnEng = Integer.parseInt(sNumber);
        String sEngNo = String.format(Locale.CHINA, "\\^ %04d:.+?", hymnEng);
        try {
            InputStream in2 = getResources().getAssets().open(mTocECFile);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            int idx = 0; // trace next mList index for access to DB page.
            for (String item : mList) {
                idx++;
                if (item.matches(sEngNo)) {
                    String hymnTN = item.replaceAll(".+? #(.+?)", "$1");
                    String hymnType = getHymnType(hymnTN);
                    int hymnNo = Integer.parseInt(hymnTN.substring(2));

                    // Set up to display DB page if dbPage and content exist.
                    if (dbPage && mList[idx].matches(sEngNo)) {
                        hymnTN = mList[idx].replaceAll(".+? #(.+?)", "$1");
                        hymnType = getHymnType(hymnTN);
                        hymnNo = Integer.parseInt(hymnTN.substring(2));
                    }
                    showContent(this, hymnType, hymnNo, false, hymnEng);
                    return;
                }
            }
            // Pass in a non-existence HYMN_BB_DUMMY for Chinese hymnNo
            showContent(this, HYMN_BB, HYMN_BB_DUMMY, false, hymnEng);
        }
        catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
            HymnsApp.showToastMessage(R.string.in_development);
        }
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
     * Hde the HistoryList View and return to main, or pop fragment if any; else close app
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if (mHistoryListView.getVisibility() == View.VISIBLE) {
                mEntry.setHint(R.string.hint_hymn_number_enter);
                mHistoryListView.setVisibility(View.GONE);
            }
            else if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                finish();
            }
            else {
                getSupportFragmentManager().popBackStack();
            }
        }
    };

    /**
     * Initial the option item menu
     *
     * @param menu the menu container
     *
     * @return true always
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        /*
        if (BuildConfig.DEBUG) {
             menu.findItem(R.id.sn_convert).setVisible(true);
        } */
        initLanguage(menu);
        return true;
    }

    /**
     * Pop up the main menu if user long press on the main UI
     *
     * @param menu menu
     * @param v view
     * @param menuInfo info
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        initLanguage(menu);
    }

    /**
     * Handler for the Context item clicked; use the same handlers as Option Item clicked
     *
     * @param item Option Item
     *
     * @return the handle state
     */
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        return onOptionsItemSelected(item);
    }

    /**
     * Handler for the option item clicked
     *
     * @param item menu Item
     *
     * @return the handle state
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        // === Set app theme ===
        int itemId = item.getItemId();
        if (itemId == R.id.themeDark) {
            setAppTheme(Theme.DARK.toString(), true);
            return true;
        }
        else if (itemId == R.id.themeLight) {
            setAppTheme(Theme.LIGHT.toString(), true);
            return true;
        }
        else if (itemId == R.id.localeSystem) {
            setAppLocale(AppLanguage.SYSTEM);
            return true;
        }
        else if (itemId == R.id.localeChinese) {
            setAppLocale(AppLanguage.ZH_HANS);
            return true;
        }
        else if (itemId == R.id.localeChineseHant) {
            setAppLocale(AppLanguage.ZH_HANT);
            return true;
        }
        else if (itemId == R.id.readingSettings) {
            mStartForResult.launch(new Intent(this, ReadingSettingsActivity.class));
            return true;
        }
        else if (itemId == R.id.localeEnglish) {
            setAppLocale(AppLanguage.EN);
            return true;

            // === Set font size ===
        }
        else if (itemId == R.id.small) {
            mFontSize = FONT_SIZE_DEFAULT - 5;
            setFontSize(mFontSize, true);
            return true;
        }
        else if (itemId == R.id.middle) {
            mFontSize = FONT_SIZE_DEFAULT;
            setFontSize(mFontSize, true);
            return true;
        }
        else if (itemId == R.id.lager) {
            mFontSize = FONT_SIZE_DEFAULT + 5;
            setFontSize(mFontSize, true);
            return true;
        }
        else if (itemId == R.id.xlager) {
            mFontSize = FONT_SIZE_DEFAULT + 10;
            setFontSize(mFontSize, true);
            return true;
        }
        else if (itemId == R.id.inc) {
            mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) + 2;
            setFontSize(mFontSize, true);
            return true;
        }
        else if (itemId == R.id.dec) {
            mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT) - 2;
            setFontSize(mFontSize, true);
            return true;

        }
        // === Set font color ===
        else if (itemId == R.id.red) {
            setFontColor(Color.RED, true);
            return true;
        }
        else if (itemId == R.id.blue) {
            setFontColor(Color.BLUE, true);
            return true;
        }
        else if (itemId == R.id.white) {
            setFontColor(Color.WHITE, true);
            return true;
        }
        else if (itemId == R.id.grey) {
            setFontColor(Color.GRAY, true);
            return true;
        }
        else if (itemId == R.id.cyan) {
            setFontColor(Color.CYAN, true);
            return true;
        }
        else if (itemId == R.id.yellow) {
            setFontColor(Color.YELLOW, true);
            return true;
        }
        else if (itemId == R.id.green) {
            setFontColor(Color.GREEN, true);
            return true;
        }
        else if (itemId == R.id.black) {
            setFontColor(ContextCompat.getColor(this, R.color.grey900), true);
            return true;

        }
        else if (itemId == R.id.sn_convert) {
            // HymnIdx2NoConvert.validateIdx2NoConversion(HYMN_ER, HYMN_ER_INDEX_MAX);
            // HymnNo2IdxConvert.validateNo2IdxConversion(HYMN_DB, HYMN_DB_NO_TMAX);
            // Hymn2SnConvert.startConvert(); use for old to new file name conversion for 1.1.0 only
            return true;
        }
        else if (itemId == R.id.media_config) {
            intent = new Intent(this, MediaConfig.class);
            startActivity(intent);
            return true;
        }
        else if (itemId == R.id.permission_request) {
            onInfoButtonClicked();
            return true;
        }
        else if (itemId == R.id.online_help) {
            About.hymnUrlAccess(this, HYMNCHTV_FAQ);
            return true;
        }
        else if (itemId == R.id.about) {
            intent = new Intent(this, About.class);
            startActivity(intent);
            return true;
        }
        else if (itemId == R.id.exit) {
            LogUploadServiceImpl.purgeDebugLog();
            finishAndRemoveTask();
            System.exit(0);
            return true;
        }
        return false;
    }

    /**
     * Bind all the button to its resource id
     */
    private void initButton() {
        mEntry = findViewById(R.id.tv_entry);

        mEntry.setOnClickListener(view -> {
            if (mHistoryListView.getVisibility() == View.GONE) {
                mEntry.setHint(R.string.hint_hymn_history);
                initHistoryList();
                mHistoryListView.setVisibility(View.VISIBLE);
            }
            else {
                mEntry.setHint(R.string.hint_hymn_number_enter);
                mHistoryListView.setVisibility(View.GONE);
            }
        });

        tv_Search = findViewById(R.id.tv_search);

        btn_n0 = findViewById(R.id.n0);
        btn_n1 = findViewById(R.id.n1);
        btn_n2 = findViewById(R.id.n2);
        btn_n3 = findViewById(R.id.n3);
        btn_n4 = findViewById(R.id.n4);
        btn_n5 = findViewById(R.id.n5);
        btn_n6 = findViewById(R.id.n6);
        btn_n7 = findViewById(R.id.n7);
        btn_n8 = findViewById(R.id.n8);
        btn_n9 = findViewById(R.id.n9);

        btn_fu = findViewById(R.id.n10);
        btn_del = findViewById(R.id.n11);

        btn_db = findViewById(R.id.bs_db);
        btn_bb = findViewById(R.id.bs_bb);
        btn_er = findViewById(R.id.bs_er);
        btn_xb = findViewById(R.id.bs_xb);
        btn_xg = findViewById(R.id.bs_xg);
        btn_yb = findViewById(R.id.bs_yb);

        btn_search = findViewById(R.id.btn_search);
        btn_update = findViewById(R.id.btn_update);
        btn_english = findViewById(R.id.btn_english);

        // Create an ArrayAdapter using the string array and hymnApp default spinner layout
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item, hymnTocPage);
        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_radio);

        mTocSpinner = findViewById(R.id.spinner_toc);
        mTocSpinner.setAdapter(mAdapter);

        // Must allow to trigger onItemSelected() to show correct color on orientation change
        mTocSpinner.setOnItemSelectedListener(this);
        mTocSpinner.setSelection(0, false);
    }

    /**
     * Retrieve all the user preference settings and initialize the UI accordingly
     */
    private void initUserSettings() {
        applyMainBackground();

        mFontSize = mSharedPref.getInt(PREF_TEXT_SIZE, FONT_SIZE_DEFAULT);
        mFontColor = mSharedPref.getInt(PREF_TEXT_COLOR, ContextCompat.getColor(this, R.color.grey900));
        initTocSpinnerItem();

        setFontSize(mFontSize, false);
        setFontColor(mFontColor, false);
    }

    // Must re-init mTocSpinnerItem reference here whenever a new item is selected
    private void initTocSpinnerItem() {
        mTocSpinnerItem = mTocSpinner.findViewById(R.id.textItem);
        mTocSpinnerItem.setGravity(Gravity.CENTER);
        mTocSpinnerItem.setTypeface(null, Typeface.BOLD);
        mTocSpinnerItem.setTextSize(mFsDelta);
        mTocSpinnerItem.setTextColor(effectiveFontColor());
    }

    /**
     * Check the menu item of the current UI language; called on every menu creation so it follows recreation.
     */
    private void initLanguage(Menu menu) {
        int checkedId;
        switch (LocaleStore.current(this)) {
            case ZH_HANS:
                checkedId = R.id.localeChinese;
                break;
            case ZH_HANT:
                checkedId = R.id.localeChineseHant;
                break;
            case EN:
                checkedId = R.id.localeEnglish;
                break;
            default:
                checkedId = R.id.localeSystem;
                break;
        }
        MenuItem item = menu.findItem(checkedId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    // Create the YB hymn cross-reference table for use in History record and PagerSlider
    private void createYbXTable() {
        ybXTable.clear();
        try {
            InputStream in2 = HymnsApp.getInstance().getResources().getAssets().open(mTocYB);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");
            for (String record : mList) {
                String[] token = record.split("\\s");
                String hymnTN = token[2].substring(1);
                if (!hymnTN.startsWith("yb")) {
                    int hymnNo = Integer.parseInt(token[0].substring(1));
                    ybXTable.put(hymnNo, hymnTN);
                }
            }
        }
        catch (IOException e) {
            Timber.w("Content toc not available: %s", e.getMessage());
        }
    }

    private void showHymn(HistoryRecord sRecord) {
        mEntry.setHint(R.string.hint_hymn_number_enter);
        mHistoryListView.setVisibility(View.GONE);

        sNumber = sRecord.getHymnNoFu();
        mEntry.setText(sNumber);
        isFu = sRecord.isFu();
        showContent(this, sRecord.getHymnType(), sRecord.getHymnNo(), false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initHistoryList() {
        List<HistoryRecord> historyRecords = mDB.getHistoryRecords();
        mHistoryAdapter = new MySwipeListAdapter<>(this, historyRecords) {
            @Override
            public void remove(HistoryRecord sRecord) {
                int count = mDB.deleteHymnHistory(sRecord);
                if (count == 1) {
                    super.remove(sRecord);
                }
            }

            @Override
            public void open(@NonNull HistoryRecord sRecord) {
                showHymn(sRecord);
            }
        };
        mHistoryListView.setAdapter(mHistoryAdapter);
        mHistoryListView.setOnTouchListener(touchListener);
    }

    /**
     * TouchListener to support singleTap, doubleTap and longPress for the view for site visit
     */
    TouchListener touchListener = new TouchListener(HymnsApp.getGlobalContext()) {
        @Override
        public boolean onSingleTap(View v, int pos) {
            HistoryRecord sRecord = (HistoryRecord) ((ListView) v).getItemAtPosition(pos);
            if (sRecord != null) {
                showHymn(sRecord);
            }
            return true;
        }

        @Override
        public void onLongPress(View v, int pos) {
            HistoryRecord sRecord = (HistoryRecord) ((ListView) v).getItemAtPosition(pos);
            if (sRecord == null)
                return;

            DialogActivity.showConfirmDialog(MainActivity.this,
                    R.string.delete,
                    R.string.delete_history,
                    R.string.delete, new DialogActivity.DialogListener() {
                        public boolean onConfirmClicked(DialogActivity dialog) {
                            mHistoryAdapter.setSelectState(pos, false);
                            mHistoryAdapter.remove(sRecord);
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog) {
                        }
                    }, sRecord.toString());

        }

        @Override
        public boolean onSwipeRight(View v, int idx) {
            mHistoryAdapter.setSelectState(idx, false);
            int pos = idx - ((ListView) v).getFirstVisiblePosition();
            return showActionButton(pos, false);
        }

        @Override
        public boolean onSwipeLeft(View v, int idx) {
            mHistoryAdapter.setSelectState(idx, true);
            int pos = idx - ((ListView) v).getFirstVisiblePosition();
            return showActionButton(pos, true);
        }

        /**
         * Toggle between primary and alt view layout pending on action and current state
         *
         * @param pos the actual ListView item view location on display; no the same as HistoryRecord index
         * @param show true is to reveal the alt layout
         * @return true always
         */
        private boolean showActionButton(int pos, boolean show) {
            ViewSwitcher child = (ViewSwitcher) mHistoryListView.getChildAt(pos);
            if (child != null) {
                if ((child.getDisplayedChild() == 0) == show) {
                    child.showNext();
                }
            }
            return true;
        }
    };

    /**
     * Set app Theme as per sTheme. Need to restart MainActivity to reflect newly selected theme.
     *
     * @param sTheme Request Theme
     * @param prefChange true if use change theme
     */
    private void setAppTheme(String sTheme, boolean prefChange) {
        Timber.d("Set App Theme: %s => %s", prefChange, sTheme);
        if (prefChange) {
            mEditor.putString(PREF_THEME, sTheme);
            mEditor.apply();

            finish();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else {
            Theme theme = Theme.valueOf(sTheme);
            ThemeHelper.setTheme(this, theme);
        }
    }

    /**
     * Set HymnApp locale per user selected language.
     * Must commit preference change immediately before perform system restart.
     * Note: Restart MainActivity does not apply to HymnApp Application class.
     * HymnApp mBase Context can only be changed with Application restart.
     *
     * @param language Locale language
     */
    private void setAppLocale(AppLanguage language) {
        if (language == LocaleStore.current(this)) {
            return;
        }
        // API 33+: framework applies it and recreates activities; API < 33: restart to re-wrap HymnsApp context
        if (LocaleStore.set(this, language)) {
            doRestart();
        }
        else {
            invalidateOptionsMenu();
        }
    }

    // Need to restart whole app to make HymnApp Locale change working
    private void doRestart() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        if (intent == null)
            return;
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * Show the main-screen background chosen in the reading settings (plan A2) and colour the hint to match.
     */
    private void applyMainBackground() {
        ReadingPalette palette = BackgroundPrefs.applyTo(findViewById(R.id.mainBackground), mSharedPref, BackgroundSlot.MAIN);
        mPalette = palette;
        TextView hint = findViewById(R.id.tv_hint);
        hint.setTextColor(palette.getAccentColor());
        // Photo backgrounds: hint, entry, search box and keys sit on the same contrast-tested panel as the lyrics
        // (the backdrop is null otherwise)
        hint.setBackground(BackgroundDrawables.backdrop(this, palette));
        findViewById(R.id.tv_entry).setBackground(BackgroundDrawables.backdrop(this, palette));
        findViewById(R.id.keypadArea).setBackground(BackgroundDrawables.backdrop(this, palette));
        findViewById(R.id.actionArea).setBackground(BackgroundDrawables.backdrop(this, palette));

        EditText search = findViewById(R.id.tv_search);
        if (mSearchDefaultBg == null) {
            mSearchDefaultBg = search.getBackground();
        }
        Drawable searchPanel = BackgroundDrawables.backdrop(this, palette);
        search.setBackground(searchPanel != null ? searchPanel : mSearchDefaultBg);
    }

    /**
     * The font colour to draw on the main screen: the user's choice if readable on the background, else the palette's text.
     */
    private int effectiveFontColor() {
        return MainScreenColors.textColor(mFontColor, mPalette);
    }

    /**
     * Set the font size of the buttons' labels
     *
     * @param size button label font size
     * @param update true to update the preference settings
     */
    private void setFontSize(int size, boolean update) {
        if (update) {
            mEditor.putInt(PREF_TEXT_SIZE, size);
            mEditor.apply();
        }
        mFsDelta = size - 10;

        btn_n0.setTextSize(size);
        btn_n1.setTextSize(size);
        btn_n2.setTextSize(size);
        btn_n3.setTextSize(size);
        btn_n4.setTextSize(size);
        btn_n5.setTextSize(size);
        btn_n6.setTextSize(size);
        btn_n7.setTextSize(size);
        btn_n8.setTextSize(size);
        btn_n9.setTextSize(size);
        // mEntry.setTextSize(size);

        btn_fu.setTextSize(mFsDelta);
        btn_del.setTextSize(mFsDelta);

        btn_db.setTextSize(mFsDelta);
        btn_bb.setTextSize(mFsDelta);
        btn_xg.setTextSize(mFsDelta);
        btn_xb.setTextSize(mFsDelta);
        btn_er.setTextSize(mFsDelta);
        btn_english.setTextSize(mFsDelta);

        btn_search.setTextSize(mFsDelta);
        btn_update.setTextSize(mFsDelta);
        mTocSpinnerItem.setTextSize(mFsDelta);
    }

    /**
     * Set the color of the buttons' labels
     *
     * @param color text color
     * @param update true to update the preference settings
     */
    private void setFontColor(int color, boolean update) {
        mFontColor = color;
        if (update) {
            mEditor.putInt(PREF_TEXT_COLOR, color);
            mEditor.apply();
        }

        // mFontColor is the user's choice; what is drawn must also be readable on the current background
        color = effectiveFontColor();

        mEntry.setHintTextColor(MainScreenColors.hintColor(color));
        mEntry.setTextColor(color);

        tv_Search.setHintTextColor(MainScreenColors.hintColor(color));
        tv_Search.setTextColor(color);
        // the underline follows the text colour; the photo-mode panel must keep its own colour
        ViewCompat.setBackgroundTintList(tv_Search,
                mPalette.getBackdropColor() == 0 ? ColorStateList.valueOf(color) : null);

        btn_n0.setTextColor(color);
        btn_n1.setTextColor(color);
        btn_n2.setTextColor(color);
        btn_n3.setTextColor(color);
        btn_n4.setTextColor(color);
        btn_n5.setTextColor(color);
        btn_n6.setTextColor(color);
        btn_n7.setTextColor(color);
        btn_n8.setTextColor(color);
        btn_n9.setTextColor(color);

        btn_fu.setTextColor(color);
        btn_del.setTextColor(color);

        btn_db.setTextColor(color);
        btn_bb.setTextColor(color);
        btn_xg.setTextColor(color);
        btn_xb.setTextColor(color);
        btn_er.setTextColor(color);

        btn_search.setTextColor(color);
        btn_update.setTextColor(color);
        btn_english.setTextColor(color);
        mTocSpinnerItem.setTextColor(color);
    }

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
     * standard ActivityResultContract#StartActivityForResult
     */
    ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new StartActivityForResult(), result -> {
        // Back from the reading settings: the main background (or its photo dim/blur) may have changed
        if (result.getResultCode() == Activity.RESULT_OK) {
            applyMainBackground();
            setFontColor(mFontColor, false);
        }
    });

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
