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

import static org.cog.hymnchtv.ContentView.LYRICS_BB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_DB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_ER_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XB_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_XG_DIR;
import static org.cog.hymnchtv.ContentView.LYRICS_YB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_BB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_DB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_ER_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_XB_DIR;
import static org.cog.hymnchtv.ContentView.SCORE_XG_DIR;
import static org.cog.hymnchtv.ui.toc.TocConstants.category_bb;
import static org.cog.hymnchtv.ui.toc.TocConstants.category_db;
import static org.cog.hymnchtv.ui.toc.TocConstants.category_er;
import static org.cog.hymnchtv.ui.toc.TocConstants.category_xb;
import static org.cog.hymnchtv.ui.toc.TocConstants.hymnCategoryBb;
import static org.cog.hymnchtv.ui.toc.TocConstants.hymnCategoryDb;
import static org.cog.hymnchtv.ui.toc.TocConstants.hymnCategoryEr;
import static org.cog.hymnchtv.ui.toc.TocConstants.hymnCategoryXb;
import static org.cog.hymnchtv.ui.toc.TocConstants.hymnCategoryYb;
import static org.cog.hymnchtv.MainActivity.ATTR_AUTO_PLAY;
import static org.cog.hymnchtv.MainActivity.ATTR_ENGLISH_NO;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_NUMBER;
import static org.cog.hymnchtv.MainActivity.ATTR_HYMN_TYPE;
import static org.cog.hymnchtv.MainActivity.ATTR_MEDIA_TYPE;
import static org.cog.hymnchtv.MainActivity.ATTR_MEDIA_URI;
import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.MainActivity.PREF_MENU_SHOW;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.MainActivity.ybXTable;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_BB_DUMMY;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_MAX;
import static org.cog.hymnchtv.utils.HymnNoValidate.HYMN_DB_NO_TMAX;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.http.util.EncodingUtils;
import org.apache.http.util.TextUtils;
import org.cog.hymnchtv.lyrics.HantVariant;
import org.cog.hymnchtv.lyrics.LyricsAssets;
import org.cog.hymnchtv.lyrics.LyricsLang;
import org.cog.hymnchtv.lyrics.LyricsScript;
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;
import org.cog.hymnchtv.concurrent.AppExecutors;
import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
import kotlin.jvm.functions.Function1;
import org.cog.hymnchtv.mediaconfig.HymnFileName;
import org.cog.hymnchtv.mediaconfig.MediaConfig;
import org.cog.hymnchtv.mediaconfig.MediaRecord;
import org.cog.hymnchtv.mediaconfig.NotionRecord;
import org.cog.hymnchtv.mediaconfig.QQRecord;
import org.cog.hymnchtv.mediaconfig.ShareWith;
import org.cog.hymnchtv.persistance.DatabaseBackend;
import org.cog.hymnchtv.persistance.FileBackend;
import org.cog.hymnchtv.reading.DisplayMode;
import org.cog.hymnchtv.reading.LyricsFont;
import org.cog.hymnchtv.reading.LyricsTypefaces;
import org.cog.hymnchtv.reading.ReadingPrefs;
import org.cog.hymnchtv.reading.ReadingSettingsActivity;
import org.cog.hymnchtv.reading.background.BackgroundPolicy;
import org.cog.hymnchtv.reading.background.BackgroundPrefs;
import org.cog.hymnchtv.reading.background.BackgroundSlot;
import org.cog.hymnchtv.reading.BackgroundPickerActivity;
import org.cog.hymnchtv.reading.background.BackgroundChoice;
import org.cog.hymnchtv.reading.background.ReadingPalette;
import org.cog.hymnchtv.reading.background.GlassMode;
import org.cog.hymnchtv.reading.background.TokenInput;
import org.cog.hymnchtv.reading.background.UiTokens;
import org.cog.hymnchtv.ui.lyrics.ReadingPanelSheet;
import org.cog.hymnchtv.ui.lyrics.ChromePage;
import org.cog.hymnchtv.ui.lyrics.FavoriteController;
import org.cog.hymnchtv.notebook.Notebook;
import org.cog.hymnchtv.ui.lyrics.LyricsChromeHost;
import org.cog.hymnchtv.ui.lyrics.LyricsWindowInsets;
import org.cog.hymnchtv.ui.lyrics.PillAnchor;
import org.cog.hymnchtv.ui.player.CapsuleForm;
import org.cog.hymnchtv.ui.player.SheetDisplay;
import org.cog.hymnchtv.ui.motion.Motion;
import org.cog.hymnchtv.ui.motion.SharedNumberStarter;
import org.cog.hymnchtv.ui.player.PlaybackUiListener;
import org.cog.hymnchtv.ui.player.GlassPolicy;
import org.cog.hymnchtv.ui.player.PlayerSheetCallbacks;
import org.cog.hymnchtv.ui.player.PlayerSheetController;
import org.cog.hymnchtv.ui.player.PlayerSheetState;
import org.cog.hymnchtv.ui.theme.SystemBars;
import org.cog.hymnchtv.utils.DepthPageTransformer;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
import org.cog.hymnchtv.utils.HymnNo2IdxConvert;
import org.cog.hymnchtv.utils.HymnNoCh2EngXRef;
import org.cog.hymnchtv.webview.WebViewFragment;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class handles the actual content source address decoding for the user selected hymn
 * The MainActivity (parent) must use SingleTask instead of SingleInstance.
 * SingleTask will call ContentHandler#onDestroy() when launch by user.
 * Otherwise ContentHandler#onDestroy() will not get call when user exits via HOME button.
 * New hymnchtv launch via MainActivity will skip this.onCreate(), and all user selections
 * are ignored; hence previous lyrics being displayed instead.
 *
 * @author Eng Chong Meng
 */
public class ContentHandler extends BaseActivity {
    public static final String btAddr = "https://bibletool.online";
    public static final String btMp3Link = "https://bibletool.online/hymnal/playnew.php?file=hymns/%s/%s#%s";

    // subdirectory for various media type
    public static String MEDIA_MEDIA = "/media_media/";
    public static String MEDIA_JIAOCHANG = "/media_jiaochang/";
    public static String MEDIA_CHANGSHI = "/media_changshi/";
    public static String MEDIA_BANZOU = "/media_banzou/";

    public static final String MIDI_BB = "bm";
    public static final String MIDI_BBC = "bmc";
    public static final String MIDI_DB = "dm";
    public static final String MIDI_DBC = "dmc";

    public static final Map<String, String> HymnTypeMap = Map.of(
            HYMN_DB, "大本诗歌",
            HYMN_BB, "补充本",
            HYMN_ER, "儿童诗歌",
            HYMN_XB, "新歌颂咏",
            HYMN_XG, "新诗歌本",
            HYMN_YB, "青年诗歌"
    );

    // DB MP3 links non-standard naming conventions
    private static final Map<Integer, String> DB_Links = new HashMap<>();

    static {
        DB_Links.put(65, "D65耶稣大名");
        DB_Links.put(199, "D199荣耀的主");
        DB_Links.put(205, "D205活水涌流");
        DB_Links.put(245, "D245路途遥远");
        DB_Links.put(247, "D247惊人恩典");
        DB_Links.put(277, "D277请进,哦请进");
        DB_Links.put(326, "D326求主光照");
        DB_Links.put(340, "D340完全地交出");
        DB_Links.put(348, "D348我今撇下一切事物背起十架跟耶稣");
        DB_Links.put(359, "D359与你合一");
        DB_Links.put(466, "D466若是死了");
        DB_Links.put(499, "D499非我所是");
        DB_Links.put(527, "D527迫得太紧");
        DB_Links.put(561, "D561凭信心求");
        DB_Links.put(599, "D599召会的种子乃基督自己作生命子粒");
        DB_Links.put(630, "D630为着你同在");
        DB_Links.put(669, "D669我有一救主在天为我祈");
        DB_Links.put(751, "D751今天神的国度对我是操练");
        DB_Links.put(763, "D763荣耀盼望是基督我的生命是祂");
        DB_Links.put(769, "D769神的永远心意是与人联合");
    }

    public final DatabaseBackend mDB = DatabaseBackend.getInstance(HymnsApp.getGlobalContext());
    private MediaContentHandler mMediaContentHandler;

    private boolean onUserLeaveHint = false;
    /** The only owner of what the player layer shows (card, capsule or nothing); see PlayerSheetController.render(). */
    private PlayerSheetController mPlayerSheet;

    // True if either YouTube or exoPlayer is playing
    private boolean isMediaPlayerUi = false;

    // Both flogs are defined here as MediaGuiController can be destroyed when play video
    private boolean mAutoPlay = false; // start playing on content shown
    private boolean mAutoStream = false; // Autoplay next video
    private boolean mMediaConfigPending = false; // media config lookup in flight: ignore further taps
    private boolean mSharePending = false; // lyrics share lookup in flight: ignore further taps

    // Hymn Type and number selected by user
    public boolean mAutoEnglish = false;
    // Allow showing of JiaoChang website if available, if lyrics text is empty and from main entry only.
    private boolean mAutoJC = false;
    public String mHymnType;
    private int mHymnNo;
    private int hymnIdx = -1;
    /** True while auto-next moves to the next hymn by itself: that page change must not stop the playback. */
    private boolean mAutoAdvancing = false;

    private String mDir = "";
    private String mFileName = "";

    // Null if there is no corresponding English lyrics
    private Integer mHymnNoEng = null;
    private String mWebUrl = null;
    private String mHymnInfo = null;
    private String mHymnSearch;

    /**
     * 大本诗歌 MP3 file naming is a mess, so attempt to use lyricsPhrase; may not match all the times
     */
    private String lyricsPhrase;

    public enum UrlType {
        englishLyrics,
        hymnGoogleSearch,
        hymnYoutubeSearch,
        hymnNotionSearch,
        hymnQqSearch,
        hymnBibleTool
    }

    private MyPagerAdapter mPagerAdapter;

    /** Favourite state of the hymn on screen; created in onCreate. */
    private FavoriteController mFavorites;

    private final FavoriteController.Listener mFavoriteListener = new FavoriteController.Listener() {
        @Override
        public void onState(boolean marked, boolean canToggle) {
            refreshFavoriteViews();
        }

        @Override
        public void onToggled(boolean marked) {
            HymnsApp.showToastMessage(marked ? R.string.fav_added : R.string.fav_removed);
        }

        @Override
        public void onError() {
            HymnsApp.showToastMessage(R.string.fav_error);
        }
    };

    /** The favourite star of every page that has a view follows the controller's state. */
    private void refreshFavoriteViews() {
        if (mPagerAdapter == null) {
            return;
        }
        for (int i = 0; i < mPagerAdapter.mFragments.size(); i++) {
            Fragment page = mPagerAdapter.mFragments.valueAt(i);
            if (page instanceof ContentView) {
                ((ContentView) page).onFavoriteStateChanged();
            }
        }
    }

    public boolean isFavoriteMarked(String type, int no) {
        return mFavorites != null && mFavorites.isMarked(type, no);
    }

    public boolean canToggleFavorite() {
        return mFavorites != null && mFavorites.canToggle();
    }
    private ViewPager2 mPager;

    public PopupWindow pop;
    public SharedPreferences sPreference;

    /**
     * The media controller used to handle the playback of the user selected hymn.
     */
    private MediaGuiController mMediaGuiController;
    private MediaDownloadHandler mMediaDownloadHandler;

    private LinearLayout mWebView;

    /** Per-session lyrics script chosen with button_ts; null = use the default from LyricsLanguagePolicy. */
    public Boolean lyricsViewOverride = null;

    private static final String STATE_PAGE = "state_page"; // fallback; ViewPager2 also restores its own item
    private static final String STATE_LYRICS_OVERRIDE = "state_lyrics_override"; // -1 none, 0 simplified, 1 traditional
    private static final String STATE_DISPLAY_OVERRIDE = "state_display_override"; // DisplayMode name; absent = none
    private static final String STATE_CHROME_VISIBLE = "state_chrome_visible"; // lyrics toolbars shown or faded away
    private static final String STATE_PLAYER_HIDDEN = "state_player_hidden"; // player bar hidden from the overflow menu
    private static final String STATE_PLAYER_COLLAPSED = "state_player_collapsed"; // portrait: capsule instead of the card

    /** Tests install a manual timer here so the idle-hide fade does not depend on emulator speed; null in production. */
    @VisibleForTesting
    public static org.cog.hymnchtv.ui.lyrics.ChromeTimer sChromeTimerForTest = null;

    /** Show/hide state of the lyrics toolbars shared by all pager pages (plan 6c). */
    private LyricsChromeHost mChromeHost;

    /** Per-session display mode chosen with button_mode; null = the default from the reading settings (plan A2). */
    public DisplayMode displayModeOverride = null;

    /** Colours matching the lyrics background actually shown; read by every ContentView page. */
    private ReadingPalette mLyricsPalette;

    /** Surface/text/accent tokens derived from the same background (visual redesign spec section 4). */
    private UiTokens mLyricsTokens;

    /** The same background's tokens for the frosted player (card and capsule): their surface is the glass tint. */
    private TokenInput mTokenInput;
    private UiTokens mPlayerTokens;
    private GlassMode mGlassMode;

    private final android.database.ContentObserver mHighContrastObserver =
            new android.database.ContentObserver(new android.os.Handler(android.os.Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    onHighContrastChanged();
                }
            };

    private final ActivityResultLauncher<Intent> mBackgroundPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> applyReadingTheme());

    private final ActivityResultLauncher<Intent> mReadingSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                onReadingSettingsReturned(result.getResultCode() == Activity.RESULT_OK && data != null
                        && data.getBooleanExtra(ContentView.EXTR_KEY_HAS_CHANGES, false));
            });

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int saved = savedInstanceState.getInt(STATE_LYRICS_OVERRIDE, -1);
            lyricsViewOverride = (saved == -1) ? null : (saved == 1);
            displayModeOverride = savedInstanceState.containsKey(STATE_DISPLAY_OVERRIDE)
                    ? DisplayMode.fromPref(savedInstanceState.getString(STATE_DISPLAY_OVERRIDE)) : null;
        }
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // getWindow().setFlags(FLAG_FULLSCREEN, FLAG_FULLSCREEN); // will hide android notification bar
        setContentView(R.layout.content_main);
        // The background runs behind the status and navigation bars; the layers keep clear of them (LyricsWindowInsets)
        SystemBars.enable(this);
        // Cold start: the "show the player by default" setting decides, and the card starts collapsed as the capsule; a recreation restores the saved choice
        SharedPreferences settings = getSharedPreferences(PREF_SETTINGS, 0);
        PlayerSheetState sheetState = savedInstanceState == null
                ? new PlayerSheetState(!settings.getBoolean(PREF_MENU_SHOW, true), true, false)
                : new PlayerSheetState(savedInstanceState.getBoolean(STATE_PLAYER_HIDDEN, false),
                        savedInstanceState.getBoolean(STATE_PLAYER_COLLAPSED, false), false);
        mPlayerSheet = new PlayerSheetController(findViewById(R.id.mediaPlayer), findViewById(R.id.playerCapsule),
                sheetState, mPlayerSheetCallbacks);
        LyricsWindowInsets.install(findViewById(R.id.linear), insets -> mPlayerSheet.onContentInsets(insets));

        // Reading settings (plan A2): background first, so pages created below read the matching palette
        sPreference = getSharedPreferences(PREF_SETTINGS, 0);
        mChromeHost = new LyricsChromeHost(this, sPreference,
                sChromeTimerForTest != null ? sChromeTimerForTest : new org.cog.hymnchtv.ui.lyrics.HandlerChromeTimer());
        mChromeHost.start(savedInstanceState != null && savedInstanceState.containsKey(STATE_CHROME_VISIBLE)
                ? savedInstanceState.getBoolean(STATE_CHROME_VISIBLE) : null);
        applyReadingTheme();
        if (ReadingPrefs.lyricsFont(sPreference) == LyricsFont.KAI) {
            // Only the script the first page will show; the other one loads when first needed
            LyricsTypefaces.preload(this, LyricsLanguagePolicy.resolveShowTraditional(
                    LyricsLang.fromPref(sPreference.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null)),
                    getResources().getConfiguration().getLocales().get(0)),
                    ReadingPrefs.lyricsWeight(sPreference) != org.cog.hymnchtv.reading.LyricsWeight.REGULAR);
        }

        // Attach the media controller player UI; Reuse the fragment if found;
        // do not create/add new, otherwise playerUi setVisibility is no working
        mMediaGuiController = (MediaGuiController) getSupportFragmentManager().findFragmentById(R.id.mediaPlayer);
        if (mMediaGuiController == null) {
            mMediaGuiController = new MediaGuiController();
            getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
        }
        mMediaContentHandler = MediaContentHandler.getInstance(this);
        isMediaPlayerUi = false;

        // Attach the File Transfer GUI; Use single instance created in HymnApp;
        // do not create/add new, otherwise GUI display is not working properly
        mMediaDownloadHandler = HymnsApp.mMediaDownloadHandler;
        getSupportFragmentManager().beginTransaction().replace(R.id.filexferGui, mMediaDownloadHandler).commit();

        mWebView = findViewById(R.id.webView);
        mWebView.setVisibility(View.INVISIBLE);

        mAutoJC = true;

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mHymnType = bundle.getString(ATTR_HYMN_TYPE);
            mHymnNo = bundle.getInt(ATTR_HYMN_NUMBER);
            mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);

            mAutoPlay = bundle.getBoolean(ATTR_AUTO_PLAY, false);
            int tmpNo = bundle.getInt(ATTR_ENGLISH_NO, -1);
            if (tmpNo != -1) {
                mAutoEnglish = true;
                mHymnNoEng = tmpNo;
            }
        }

        // One-shot launch actions must not repeat after recreation (e.g. after changing lyrics settings)
        if (savedInstanceState != null) {
            mAutoJC = false;
            mAutoPlay = false;
            mAutoEnglish = false;
        }

        switch (mHymnType) {
        // Convert the user input hymn number i.e: hymn #1 => #0 i.e.index number
        case HYMN_ER:
        case HYMN_XB:
        case HYMN_XG:
        case HYMN_YB:
        case HYMN_BB:
        case HYMN_DB:
            hymnIdx = HymnNo2IdxConvert.hymnNo2IdxConvert(mHymnType, mHymnNo);
            break;
        }

        // Created before the pager so that pages built by the adapter can ask for the current state
        mFavorites = new FavoriteController(Notebook.async(this), mFavoriteListener);
        mFavorites.onHymnChanged(mHymnType, mHymnNo);

        // The pager adapter, which provides the pages to the view pager widget.
        mPagerAdapter = new MyPagerAdapter(this, mHymnType);

        // Instantiate a ViewPager2 and a PagerAdapter.
        mPager = findViewById(R.id.viewPager);
        // FragmentStatePagerAdapter default seems to create only 2, so omit this statement, otherwise 9 items get created
        // FragmentStateAdapter default created 9, setOffscreenPageLimit has no effect
        // mPager.setOffscreenPageLimit(1);
        // mPager.setCurrentItem(hymnIdx, false) will force it to load only user selected page
        mPager.setAdapter(mPagerAdapter);
        // Plan A2: page-turn animation is a reading setting (B-11 will default it off on low-RAM phones)
        if (ReadingPrefs.pageAnimation(sPreference)) {
            mPager.setPageTransformer(new DepthPageTransformer());
        }

        // Set the viewPager to the user selected hymn number, no transform animation; this also fixed incorrect page being displayed
        // see https://issuetracker.google.com/issues/177051960
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_PAGE))
            mPager.setCurrentItem(savedInstanceState.getInt(STATE_PAGE), false);
        else if (hymnIdx != -1)
            mPager.setCurrentItem(hymnIdx, false);
        else
            mPager.setCurrentItem(mHymnNo, false);

        mPager.registerOnPageChangeCallback(initOnPageChangeCallback());
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
        setupEnterMotion(savedInstanceState != null);
    }

    /**
     * Fade in and out; when opened from the home preview, the pager page is not shown until its header number has a
     * place to land (or 300 ms passed), so the shared element starts from the right bounds.
     */
    private void setupEnterMotion(boolean restored) {
        Motion.applyContentWindow(getWindow(), this);
        if (restored || !Motion.enabled(this) || !getIntent().getBooleanExtra(Motion.EXTRA_SHARED_NUMBER, false)) {
            return;
        }
        postponeEnterTransition();
        new SharedNumberStarter(getWindow().getDecorView(), this::placeSharedNumber, () -> {
            startPostponedEnterTransition();
            return kotlin.Unit.INSTANCE;
        },
                Motion.POSTPONE_TIMEOUT_MS);
    }

    private ContentView currentContentView() {
        Fragment page = mPagerAdapter == null ? null : mPagerAdapter.mFragments.get(mPager.getCurrentItem());
        return page instanceof ContentView ? (ContentView) page : null;
    }

    private boolean placeSharedNumber() {
        ContentView page = currentContentView();
        return page != null && page.placeNumberAnchor();
    }

    /** The shared number only flies in; going back is a plain fade, the hymn may have changed meanwhile. */
    @Override
    public void finishAfterTransition() {
        ContentView page = currentContentView();
        if (page != null) {
            page.clearNumberAnchor();
        }
        super.finishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onUserLeaveHint = false;
        mPlayerSheet.render();
        getContentResolver().registerContentObserver(GlassPolicy.highTextContrastUri(), false, mHighContrastObserver);
        onHighContrastChanged();
        // Keep the screen on while lyrics/score are shown (plan A.1.7); window-level so pager changes never drop it
        if (ReadingPrefs.keepScreenOn(sPreference)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    protected void onPause() {
        getContentResolver().unregisterContentObserver(mHighContrastObserver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mChromeHost.stop();
        mPlayerSheet.release();
        mFavorites.destroy();
    }

    /** A lyrics page follows the toolbar show/hide state from now on (plan 6c). */
    public void registerChromePage(ChromePage page) {
        mChromeHost.register(page);
    }

    public void unregisterChromePage(ChromePage page) {
        mChromeHost.unregister(page);
    }

    /** Single tap in the middle of a lyrics page. */
    public void onLyricsCenterTap() {
        mChromeHost.toggle();
    }

    /** A toolbar button was used: the 4 s idle timer restarts. */
    public void onChromeInteraction() {
        mChromeHost.onInteraction();
    }

    /** TalkBack on/off for the toolbars; public so ContentHandler tests can drive it without a screen reader. */
    @VisibleForTesting
    public void setChromeAlwaysVisible(boolean always) {
        mChromeHost.setAlwaysVisible(always);
    }

    /** True while the Aa sheet or the overflow menu is open: the toolbars stay. */
    public void setChromeHeld(boolean held) {
        mChromeHost.setHeld(held);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, mPager.getCurrentItem());
        outState.putBoolean(STATE_CHROME_VISIBLE, mChromeHost.isVisible());
        outState.putBoolean(STATE_PLAYER_HIDDEN, mPlayerSheet.getState().getUserHidden());
        outState.putBoolean(STATE_PLAYER_COLLAPSED, mPlayerSheet.getState().getCollapsed());
        outState.putInt(STATE_LYRICS_OVERRIDE, lyricsViewOverride == null ? -1 : (lyricsViewOverride ? 1 : 0));
        if (displayModeOverride != null) {
            outState.putString(STATE_DISPLAY_OVERRIDE, displayModeOverride.name());
        }
    }

    /**
     * When user exit via Home button; stop media player if any.
     * It is also triggered when external browser is launched.
     */
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        onUserLeaveHint = true;
        // The user pressed the Home button or Overview button to leave the app
        backPressedCallback.handleOnBackPressed();
    }

    // Clear auto streaming on exit
    @Override
    protected void onStop() {
        super.onStop();
        setAutoStream(false);
    }

    /** A video (YouTube or exoPlayer) takes the player layer: the card and the capsule give way to it. */
    public void showMediaPlayerUi() {
        isMediaPlayerUi = true;
        mPlayerSheet.setVideoActive(true);
    }

    /** The video is gone and the audio card is back in the player layer; render() shows it in its recorded form. */
    private void onVideoClosed() {
        isMediaPlayerUi = false;
        mPlayerSheet.setVideoActive(false);
    }

    private final PlayerSheetCallbacks mPlayerSheetCallbacks = new PlayerSheetCallbacks() {
        @Override
        public boolean isPortrait() {
            return PlayerSheetState.isPortrait(getResources().getConfiguration().orientation);
        }

        @Override
        public void setCardVisible(boolean visible) {
            if (mMediaGuiController != null) {
                mMediaGuiController.initPlayerUi(visible);
            }
        }

        @Override
        public void togglePlayback() {
            mMediaGuiController.startPlay();
        }

        @Override
        public void onPlayerInsetsChanged() {
            for (ContentView page : livePages()) {
                page.onPlayerInsetsChanged();
            }
        }
    };

    /** The player layer's controller (card, capsule, insets). */
    public PlayerSheetController getPlayerSheet() {
        return mPlayerSheet;
    }

    /** Pixels the lyrics keep clear for the player layer: the card's height, the capsule's, or 0. */
    public int getPlayerReserve() {
        return mPlayerSheet == null ? 0 : mPlayerSheet.playerReserve();
    }

    /** What the bottom toolbar capsule lines up with (player display and capsule width). */
    public PillAnchor getPillAnchor() {
        return mPlayerSheet == null
                ? new PillAnchor(SheetDisplay.HIDDEN, 0, CapsuleForm.NOTE)
                : mPlayerSheet.pillAnchor();
    }

    /** System bottom (or keyboard) inset in pixels, counted once for the lyrics padding. */
    public int getSystemBottomInset() {
        return mPlayerSheet == null ? 0 : mPlayerSheet.getSystemBottom();
    }

    /** Receives the audio player's state for the capsule. */
    public PlaybackUiListener getPlaybackUiListener() {
        return mPlayerSheet;
    }

    /**
     * Colours for the lyrics background on screen (plan A2). A page restored by the FragmentManager can ask
     * before onCreate has applied the background, so fall back to the resolved (not yet shown) choice.
     */
    public ReadingPalette getLyricsPalette() {
        if (mLyricsPalette == null) {
            mLyricsPalette = BackgroundPolicy.palette(
                    BackgroundPrefs.resolve(getSharedPreferences(PREF_SETTINGS, 0), BackgroundSlot.LYRICS));
        }
        return mLyricsPalette;
    }

    /** UI tokens for the lyrics background on screen; same fallback rule as {@link #getLyricsPalette()}. */
    public UiTokens getLyricsTokens() {
        if (mLyricsTokens == null) {
            mLyricsTokens = UiTokens.Companion.from(BackgroundPolicy.tokenInput(
                    BackgroundPrefs.resolve(getSharedPreferences(PREF_SETTINGS, 0), BackgroundSlot.LYRICS)));
        }
        return mLyricsTokens;
    }

    /** Glass tokens of the player card and capsule for the lyrics background on screen. */
    public UiTokens getPlayerTokens() {
        if (mPlayerTokens == null) {
            derivePlayerTokens(BackgroundPolicy.tokenInput(
                    BackgroundPrefs.resolve(getSharedPreferences(PREF_SETTINGS, 0), BackgroundSlot.LYRICS)));
        }
        return mPlayerTokens;
    }

    public GlassMode getGlassMode() {
        getPlayerTokens();
        return mGlassMode;
    }

    private void derivePlayerTokens(TokenInput input) {
        mTokenInput = input;
        mGlassMode = GlassPolicy.mode(this);
        mPlayerTokens = UiTokens.Companion.glass(input, mGlassMode);
    }

    /** Paints the card and the capsule with the current glass tokens. */
    private void applyPlayerGlass() {
        if (mMediaGuiController != null) {
            mMediaGuiController.applyTokens(mPlayerTokens, mGlassMode);
        }
        mPlayerSheet.applyTokens(mPlayerTokens, mGlassMode);
    }

    /** The system's high-contrast text setting may have changed: the glass turns opaque (or back) at once. */
    private void onHighContrastChanged() {
        if (mTokenInput != null && GlassPolicy.mode(this) != mGlassMode) {
            derivePlayerTokens(mTokenInput);
            applyPlayerGlass();
        }
    }

    /**
     * The single propagation point of the lyrics theme (visual redesign 6): apply the LYRICS background, derive the
     * palette and tokens once, and hand them to every page that has a view and to the player card. Called from
     * onCreate (also after a recreation), when the Aa panel or the background picker changes the theme.
     */
    public void applyReadingTheme() {
        BackgroundChoice choice = BackgroundPrefs.applyChoiceTo(findViewById(R.id.lyricsBackground), sPreference, BackgroundSlot.LYRICS);
        mLyricsPalette = BackgroundPolicy.palette(choice);
        TokenInput tokenInput = BackgroundPolicy.tokenInput(choice);
        mLyricsTokens = UiTokens.Companion.from(tokenInput);
        derivePlayerTokens(tokenInput);
        LyricsEnglishRecord.setDarkBackground(mLyricsPalette.isDark());
        SystemBars.styleIcons(this, mLyricsPalette.isDark(), SystemBars.legacyNavColor(mLyricsPalette.isDark(),
                UiTokens.over(mLyricsPalette.getPaperColor(), mLyricsTokens.getSurface()), mLyricsTokens.getOnSurface()));
        for (ContentView page : livePages()) {
            page.applyTheme(mLyricsPalette, mLyricsTokens);
        }
        applyPlayerGlass();
    }

    /**
     * Size, typeface or display mode changed in the Aa panel: apply to the pages that have a view now; a page without
     * one (not created yet, or destroyed) reads the stored values in onCreateView.
     *
     * @param modeChanged true when the display mode changed: the session override is dropped so the new default shows
     */
    public void applyReadingPrefsToPages(boolean modeChanged) {
        if (modeChanged) {
            displayModeOverride = null;
        }
        for (ContentView page : livePages()) {
            page.applyReadingPrefs();
        }
    }

    /** The lyrics pages whose view exists (FragmentManager order); destroyed or not yet created ones are skipped. */
    private List<ContentView> livePages() {
        List<ContentView> pages = new ArrayList<>();
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof ContentView && fragment.isAdded() && fragment.getView() != null) {
                pages.add((ContentView) fragment);
            }
        }
        return pages;
    }

    /** The "Aa" button: quick reading-style panel. */
    public void showReadingPanel() {
        if (getSupportFragmentManager().findFragmentByTag(ReadingPanelSheet.TAG) == null && !isFinishing()
                && !getSupportFragmentManager().isStateSaved()) {
            new ReadingPanelSheet().show(getSupportFragmentManager(), ReadingPanelSheet.TAG);
        }
    }

    /** "More..." of the Aa panel: the full background picker for the lyrics slot. */
    public void openBackgroundPicker() {
        mBackgroundPickerLauncher.launch(BackgroundPickerActivity.intent(this, BackgroundSlot.LYRICS));
    }

    /** Opens the reading settings; on return with changes all pages are rebuilt (see onReadingSettingsReturned). */
    public void openReadingSettings() {
        mReadingSettingsLauncher.launch(new Intent(this, ReadingSettingsActivity.class));
    }

    /**
     * New defaults: drop both session toggles and rebuild every page with the new settings.
     * Public so ContentHandlerReadingTest can drive the settings-return path without the settings UI.
     */
    @VisibleForTesting
    public void onReadingSettingsReturned(boolean hasChanges) {
        if (hasChanges) {
            lyricsViewOverride = null;
            displayModeOverride = null;
            recreate();
        }
    }

    /**
     * Show JiaoChang web site if available and user enter from main UI;
     * Called when lyrics text is empty.
     */
    public void selectJC() {
        if (mMediaGuiController.selectJC()) {
            if (mAutoJC) {
                mAutoJC = false;
                startPlay();
            }
            else {
                HymnsApp.showToastMessage(R.string.hint_hymn_lyrics_online);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (pop != null) {
                pop.dismiss();
                pop = null;
            }
            mPlayerSheet.toggleUserHidden();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Hde the HistoryList View and return to main, or pop fragment if any; else close app
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // for web view links
            if (mWebView.isShown()) {
                mWebView.setVisibility(View.INVISIBLE);
            }
            // For video player
            else if (isMediaPlayerUi) {
                if (mMediaContentHandler.isPlayerVisible()) {
                    mMediaContentHandler.releasePlayer();
                    // Must do this only after mMediaContentHandler.releasePlayer()
                    mMediaGuiController.initPlaybackSpeed();

                    // Restore the default MediaGuiController UI; its card shows itself in the recorded form once created
                    getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
                    onVideoClosed();
                }
                else {
                    mMediaContentHandler.setPlayerVisible(true);
                }
            }
            // For audio player
            else if (mMediaGuiController.isPlaying()) {
                mMediaGuiController.stopPlay();
                setAutoStream(false);
                Timber.e("mMediaGuiController.stopPlay()");
            }
            // Leave Home press handerling to android system.
            else if (onUserLeaveHint) {
                onUserLeaveHint = false;
            }
            else {
                backToHome();
            }
        }
    };

    /**
     * The lyrics top-bar buttons and its overflow menu (plan C-4; the long-press context menu is gone).
     *
     * @param itemId a top-bar action id (ids_lyrics.xml) or an item of menu_lyrics_more
     * @return true if the action was handled
     */
    public boolean onLyricsAction(int itemId) {
        ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());

        if (itemId == R.id.readingSettings) {
            openReadingSettings();
            return true;
        }
        else if (itemId == R.id.favorite) {
            mFavorites.toggle();
            return true;
        }
        else if (itemId == R.id.menutoggle) {
            mPlayerSheet.toggleUserHidden();
            return true;
        }
        else if (itemId == R.id.scoreColorChange) {
            if (contentView != null)
                contentView.toggleScoreColor();
            return true;
        }
        else if (itemId == R.id.lyrcsTextSizeInc || itemId == R.id.lyrcsTextSizeDec) {
            if (contentView != null)
                contentView.setLyricsTextSize(itemId == R.id.lyrcsTextSizeInc);
            return true;
        }
        else if (itemId == R.id.media_config) {
            openMediaConfig();
            return true;
        }
        else if (itemId == R.id.lyrcsEnglish) {
            if (mHymnNoEng == null) {
                HymnsApp.showToastMessage(R.string.error_english_lyrics_null, mHymnNo);
                return true;
            }
            initWebView(UrlType.englishLyrics);
            return true;
        }
        else if (itemId == R.id.lyrcsEnglishDelete) {
            deleteEnglishLyrics();
            return true;
        }
        else if (itemId == R.id.lyrcsShare) {
            lyricsShare();
            return true;
        }
        else if (itemId == R.id.help) {
            startActivity(new Intent(this, org.cog.hymnchtv.about.HelpActivity.class));
            return true;
        }
        else if (itemId == R.id.home) {
            backToHome();
            return true;
        }
        return false;
    }

    private void backToHome() {
        mMediaGuiController.stopPlay();
        finish();
    }

    /**
     * Open the media config screen prefilled with the current hymn's media link. The stored media record is read
     * on AppExecutors.io; the screen is started on the main thread. A second request while one is pending is ignored.
     */
    private void openMediaConfig() {
        if (mMediaConfigPending) {
            return;
        }
        mMediaConfigPending = true;
        final MediaType mediaType = mMediaGuiController.getMediaType();
        final String hymnType = mHymnType;
        final int hymnNo = mHymnNo;

        AppExecutors.ioThenMain("media-config-lookup", this,
                () -> mMediaContentHandler.findMediaRecord(hymnType, hymnNo, mediaType),
                mediaRecord -> {
                    mMediaConfigPending = false;
                    String dir = hymnType + MediaConfig.mediaDir.get(mediaType);

                    // Default to not empty string so mediaConfig will fill all other provided bundle info.
                    String mediaUrl = " ";
                    List<Uri> uriList = new ArrayList<>();
                    if (mMediaContentHandler.getMediaUris(mediaRecord, uriList)
                            || isFileExist(dir, hymnNo, uriList)) {
                        if (!uriList.isEmpty()) {
                            mediaUrl = uriList.get(0).toString();
                        }
                    }

                    Intent intent = new Intent(this, MediaConfig.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(ATTR_MEDIA_URI, mediaUrl);
                    bundle.putInt(ATTR_MEDIA_TYPE, mediaType.getValue());
                    bundle.putString(ATTR_HYMN_TYPE, hymnType);
                    bundle.putInt(ATTR_HYMN_NUMBER, hymnNo);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }, () -> mMediaConfigPending = false);
    }

    /**
     * Delete the stored English lyrics of the current hymn on AppExecutors.io.
     */
    private void deleteEnglishLyrics() {
        final Integer hymnNoEng = mHymnNoEng;
        if (hymnNoEng == null) {
            HymnsApp.showToastMessage(R.string.error_english_lyrics_null, mHymnNo);
            return;
        }
        final int dbKey = LyricsEnglishRecord.dbHymnNo(hymnNoEng, HYMN_ER.equals(mHymnType));
        AppExecutors.io("lyrics-eng-delete", () -> mDB.deleteLyricsEng(dbKey));
    }

    /**
     * Sharing of both the score png and lyrics text files via e.g. whatsapp
     */
    private void lyricsShare() {
        if (mSharePending) {
            return;
        }
        mSharePending = true;
        final String hymnType = mHymnType;
        final int hymnNo = mHymnNo;
        AppExecutors.ioThenMain("lyrics-share-url", this, () -> getMediaUrl(hymnType, hymnNo), mediaUrl -> {
            mSharePending = false;
            lyricsShare(hymnType, hymnNo, mediaUrl);
        }, () -> mSharePending = false);
    }

    /**
     * @param hymnType the hymn type the share was requested for (the user may have moved on meanwhile)
     * @param hymnNo the hymn number the share was requested for
     * @param mediaUrl that hymn's media link read from the DB, or null
     */
    private void lyricsShare(String hymnType, int hymnNo, String mediaUrl) {
        String resPrefix = "";
        String resFName = "";

        switch (hymnType) {
        case HYMN_ER:
            resPrefix = SCORE_ER_DIR + hymnNo;
            resFName = LYRICS_ER_DIR + "er" + hymnNo;
            break;

        case HYMN_XB:
            resPrefix = SCORE_XB_DIR + "xb" + hymnNo;
            resFName = LYRICS_XB_DIR + "xb" + hymnNo;
            break;

        case HYMN_XG:
            resPrefix = SCORE_XG_DIR + "xg" + hymnNo;
            resFName = LYRICS_XG_DIR + "xg" + hymnNo;
            break;

        case HYMN_YB:
            resPrefix = SCORE_XB_DIR + "yb" + hymnNo;
            resFName = LYRICS_XB_DIR + "yb" + hymnNo;
            break;

        case HYMN_BB:
            resPrefix = SCORE_BB_DIR + "bb" + hymnNo;
            resFName = LYRICS_BB_DIR + "bb" + hymnNo;
            break;

        case HYMN_DB:
            resPrefix = SCORE_DB_DIR + "db" + hymnNo;
            resFName = LYRICS_DB_DIR + "db" + hymnNo;
            break;
        }

        String fnScore = resPrefix + ".png";
        File fileScore = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fnScore.split("/")[1]);

        String fnLyrics = resFName + ".txt";
        File fileLyrics = new File(FileBackend.getHymnchtvStore(FileBackend.TMP, true), fnLyrics.split("/")[1]);

        try {
            InputStream inputStream = getResources().getAssets().open(fnScore);
            FileOutputStream outputStream = new FileOutputStream(fileScore);
            FileBackend.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();

            inputStream = getResources().getAssets().open(fnLyrics);
            outputStream = new FileOutputStream(fileLyrics);
            FileBackend.copy(inputStream, outputStream);
            inputStream.close();
            outputStream.close();

            ArrayList<Uri> imageUris = new ArrayList<>();
            imageUris.add(FileBackend.getUriForFile(this, fileScore));
            imageUris.add(FileBackend.getUriForFile(this, fileLyrics));
            ShareWith.share(this, mediaUrl, imageUris);
        }
        catch (IOException e) {
            Timber.e("lyrics shared: %s", e.getMessage());
        }
    }

    /**
     * Get the media record URL link of the given hymn. Reads the DB: AppExecutors.io only.
     *
     * @return urlLink if available else null
     */
    private String getMediaUrl(String hymnType, int hymnNo) {
        String urlLink = null;
        boolean isFu = hymnType.equals(HYMN_DB) && (hymnNo > HYMN_DB_NO_MAX);
        MediaRecord mediaRecord = new MediaRecord(hymnType, hymnNo, isFu, MediaType.HYMN_MEDIA);

        try {
            if (mDB.getMediaRecord(mediaRecord, true) && (mediaRecord.getMediaUri() != null)) {
                urlLink = mediaRecord.toString();
            }
        }
        catch (RuntimeException e) {
            Timber.e(e, "Media link lookup failed: %s", mediaRecord);
        }
        return urlLink;
    }

    /**
     * Callback interface for responding to changing state of the selected page.
     */
    private OnPageChangeCallback initOnPageChangeCallback() {
        return new OnPageChangeCallback() {
            /**
             * This method will be invoked when a new page becomes selected. Animation is not necessarily complete.
             *
             * @param position Position index of the new selected page.
             */
            @Override
            public void onPageSelected(int position) {
                int tmp = HymnIdx2NoConvert.hymnIdx2NoConvert(mHymnType, position)[0];
                if (tmp != mHymnNo) {
                    mHymnNo = tmp;
                    hymnIdx = position;
                    mFavorites.onHymnChanged(mHymnType, mHymnNo);
                    stopPlaybackForHymnChange();
                    updateMediaPlayerInfo();

                    // Will be handled in ContentView.onCreateView()
                    // ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());
                    // if (contentView != null)
                    //     contentView.setLyricsTextScale();
                }
            }
        };
    }

    /**
     * Update all the required media info base on the current selected hymnType and hymnNo i.e.
     * a. English hymn number or null if none
     * b. The media player hymn title info
     * c. The text color of the Button Media
     */
    public void updateMediaPlayerInfo() {
        // Update both mHymnType and mHymnNo for share autofill.
        MainActivity.setHymnTypeNo(mHymnType, mHymnNo);
        if (mHymnNo != HYMN_BB_DUMMY) {
            mHymnNoEng = HymnNoCh2EngXRef.hymnNoCh2EngConvert(mHymnType, mHymnNo);
        }

        mHymnInfo = getHymnInfo();

        // Check to see if all the mediaTypes are defined/available for the current user selected HymnType/HymnNo;
        // the DB and file lookups run on AppExecutors.io, a result for a hymn the user has left is dropped.
        final String hymnType = mHymnType;
        final int hymnNo = mHymnNo;
        // Shown in the reader's lyrics script; mHymnInfo stays Simplified because file names and searches derive from it
        final HantVariant variant = LyricsScript.hantVariant(this);
        final String hymnInfo = (variant == null) ? mHymnInfo : getHymnInfo(variant);
        AppExecutors.ioThenMain("hymn-media-state", this, () -> getHymnMediaState(hymnType, hymnNo), isAvailable -> {
            if (hymnNo == mHymnNo && hymnType.equals(mHymnType)) {
                mMediaGuiController.initHymnInfo(hymnInfo, isAvailable);
            }
        }, () -> { });
    }

    /**
     * Start playing the user selected hymn upon MediaGuiController init. Call from MediaGuiController.
     * Must reset to prevent multiple autoplay after exited from an external player.
     */
    public boolean isAutoPlay(boolean reset) {
        if (mAutoPlay && reset) {
            mAutoPlay = false;
            return true;
        }
        else
            return mAutoPlay;
    }

    /**
     * Start to play after the file is downloaded. Call from mediaHandler.
     */
    public void startPlay() {
        mMediaGuiController.startPlay();
    }

    public void setAutoStream(boolean autoStream) {
        mAutoStream = autoStream;
        // keep the visible auto-play check box in step with this state
        if (mMediaGuiController != null) {
            mMediaGuiController.setAutoStreamChecked(autoStream);
        }
    }

    public boolean isAutoStream() {
        return mAutoStream;
    }

    private boolean advanceByAutoStream() {
        mAutoAdvancing = true;
        try {
            return scrollNextHymn();
        }
        finally {
            mAutoAdvancing = false;
        }
    }

    // Media file playback ended or file download error
    public void onEndOrError(String statusText) {
        // Download callbacks outlive this page (the download handler is app-wide); ignore them once it is going away
        if (isFinishing() || isDestroyed())
            return;
        Timber.w("AutoStream: %s; %s", mAutoStream, statusText);
        if (mAutoStream && advanceByAutoStream()) {
            if (isMediaPlayerUi) {
                mMediaContentHandler.releasePlayer();
                // Restore the default MediaGuiController UI
                getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
                onVideoClosed();
            }

            // Allow some delay for the player and scrolled UI to settle before proceed
            new Handler(Looper.getMainLooper()).postDelayed(this::startPlay, 100);
        }
        else {
            HymnsApp.showToastMessage(statusText);
            mMediaGuiController.showPlayIcon(true);
            setAutoStream(false);
        }
    }

// 第112首 神生命的种子 https://g.cgbr.org/music/x/media/112x.mp3
// https://g.cgbr.org/music/x/media/139.mp3

    /**
     * Fetch the playback list of the current hymn without blocking the main thread: the stored media record is read
     * on AppExecutors.io, then {@link #getPlayHymn(MediaType, boolean, MediaRecord)} runs on the main thread.
     *
     * @param onResult receives the playback list, or null if the user changed the hymn meanwhile (nothing played)
     */
    public void fetchPlayHymn(MediaType mediaType, boolean proceedDownLoad, Consumer<List<Uri>> onResult) {
        final String hymnType = mHymnType;
        final int hymnNo = mHymnNo;
        AppExecutors.ioThenMain("play-hymn-lookup", this,
                () -> mMediaContentHandler.findMediaRecord(hymnType, hymnNo, mediaType),
                mediaRecord -> onResult.accept((hymnNo == mHymnNo && hymnType.equals(mHymnType))
                        ? getPlayHymn(mediaType, proceedDownLoad, mediaRecord) : null),
                () -> onResult.accept(null));
    }

    /**
     * First priority: fetch the user defined DB media links/contents for the selected hymnType/hymnNo.
     * To save local storage space; the media url link is played via streaming using YoutubePlayer,
     * or ExoPlayer without downloading the file.
     * <p>
     * If none found, then fetch the required playback media resources from local directory if available.
     * Otherwise, fetch from online sites with the predefined link if available and if proceedDownLoad is true;
     * else drop to next mediaType search for playback. Main thread; reads no DB.
     *
     * @param mediaType media Type for the playback i.e. hymnType ER, JIAOCHANG, CHANGSHI or BANZOU
     * @param proceedDownLoad download from the specified dnLink if true;
     * @param mediaRecord the stored media record of the current hymn and mediaType, or null if none
     *
     * @return list of media resource to playback. Usually only one item, two for midi resources
     */
    private List<Uri> getPlayHymn(MediaType mediaType, boolean proceedDownLoad, MediaRecord mediaRecord) {
        List<Uri> uriList = new ArrayList<>();
        /*
         * Fetch the user defined DB media links/contents for the selected hymnType/hymnNo;
         * an empty uriList is returned when it has been handled/played in the above process.
         *
         * Proceed to other media handlers if is not handled in getMediaUris i.e. not defined in DB
         * A media audio link or download link is returned. The media audio content can be
         * in mp3, mid, midi format.
         */
        if (mMediaContentHandler.getMediaUris(mediaRecord, uriList)) {
            return uriList;
        }

        String dir = null;
        String fbLink = null;
        String fileName = mHymnNo + getHymnTitle();

        switch (mHymnType) {
        // HYMN_ER, "儿童诗歌"
        case HYMN_ER:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "C" + fileName + ".mp3";
                    // fbLink = String.format(Locale.US, "https://www.lightinnj.org/mp3/k-mp3/C%04d.mp3", mHymnNo);
                    fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/er/mp3/er%d.mp3", mHymnNo);
                    break;
                }

            case HYMN_BANZOU:
                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;
            }
            break;

        // HYMN_XB, "新歌颂咏"
        case HYMN_XB:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "X" + fileName + ".mp3";
                    // fbLink = String.format(Locale.US, "https://g.cgbr.org/music/x/media/%03d.mp3", mHymnNo);
                    // fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/xin/mp3/X%d.mp3", mHymnNo);
                    fbLink = String.format(Locale.US, "https://four.soqimp.com/sg/xin/mp3/X%d.mp3", mHymnNo);
                    break;
                }

            case HYMN_BANZOU:
                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;
            }
            break;

        // HYMN_XG, "新诗歌本"
        case HYMN_XG:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "xg" + fileName + ".mp3";
                    // https://mana.stmn1.com/sg/csr/mp3/csr20.mp3
                    // fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/csr/mp3/csr%d.mp3", mHymnNo);
                    fbLink = String.format(Locale.US, "https://four.soqimp.com/sg/csr/mp3/csr%d.mp3", mHymnNo);
                    break;
                }

            case HYMN_BANZOU:
                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;
            }
            break;

        // HYMN_YB, "青年诗歌"
        case HYMN_YB:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "Q" + fileName + ".mp3";
                    fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/yb/mp3/Q%d.mp3", mHymnNo);

                    // Translate YB to other if specified.
                    String hymnTN = ybXTable.get(mHymnNo);
                    if (hymnTN != null) {
                        mHymnType = MainActivity.getHymnType(hymnTN);
                        mHymnNo = Integer.parseInt(hymnTN.substring(2));
                    }
                    uriList.add(Uri.parse(getHymnUri()));
                    return uriList;
                }

            case HYMN_BANZOU:
                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;
            }
            break;

        // HYMN_BB, "补充本"
        case HYMN_BB:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "B" + fileName + ".mp3";
                    // https://www.hymnal.net/Hymns/Chinese/mp3/ch_0048_vocal.mp3
                    // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=sing", mHymnNo);
                    // fbLink = String.format(Locale.US, "https://four.soqimp.com/sg/bu/mp3/B%d.mp3", mHymnNo);
                    fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/bu/mp3/B%d.mp3", mHymnNo);
                    break;
                }

            case HYMN_BANZOU:
                // proceed to use HYMN_BANZOU if no midi files available
                if (HymnsApp.getFileResId(MIDI_BB + mHymnNo, "raw") != 0) {
                    uriList.add(HymnsApp.getRawUri(MIDI_BB + mHymnNo));
                    uriList.add(HymnsApp.getRawUri(MIDI_BBC + mHymnNo));
                    return uriList;
                }

                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "B" + fileName + ".mid";
                    // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ts/%d/f=mid", mHymnNo);
                    // https://www.hymnal.net/Hymns/ChineseTS/midi/tunes/ts0014_tune.midi
                    fbLink = String.format(Locale.US, "https://www.hymnal.net/Hymns/ChineseTS/midi/tunes/ts%04d_tune.midi", mHymnNo);
                    break;
                }
            }
            break;

        // HYMN_DB, "大本诗歌"
        case HYMN_DB:
            switch (mediaType) {
            case HYMN_MEDIA:
                dir = mHymnType + MEDIA_MEDIA;
                if (isFileExist(dir, mHymnNo, uriList) || mAutoStream) break;

            case HYMN_JIAOCHANG:
                dir = mHymnType + MEDIA_JIAOCHANG;
                if (isFileExist(dir, mHymnNo, uriList)) break;

            case HYMN_CHANGSHI:
                dir = mHymnType + MEDIA_CHANGSHI;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    // Use lyricsPhrase for DB filename for reference.
                    fileName = "D" + mHymnNo + lyricsPhrase + ".mp3";
                    // https://g.cgbr.org/music/d/media/48m.mp3
                    // fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=sing", mHymnNo);
                    fbLink = String.format(Locale.US, "https://mana.stmn1.com/sg/da/Dmp3/D%d.mp3", mHymnNo);
                    break;
                }

            case HYMN_BANZOU:
                // proceed to use HYMN_BANZOU if no midi files available
                if (HymnsApp.getFileResId(MIDI_DB + mHymnNo, "raw") != 0) {
                    uriList.add(HymnsApp.getRawUri(MIDI_DB + mHymnNo));
                    uriList.add(HymnsApp.getRawUri(MIDI_DBC + mHymnNo));
                    return uriList;
                }

                dir = mHymnType + MEDIA_BANZOU;
                if (isFileExist(dir, mHymnNo, uriList)) break;

                if (proceedDownLoad) {
                    fileName = "D" + fileName + ".mid";
                    fbLink = String.format(Locale.US, "https://www.hymnal.net/cn/hymn/ch/%d/f=mid", mHymnNo);
                    break;
                }
            }
            break;
        }

        if (!TextUtils.isEmpty(fbLink) && !TextUtils.isEmpty(fileName)) {
            // Timber.d("Download Info: FileName = %s%s; fbLink = %s", dir, fileName, fbLink);
            mMediaDownloadHandler.initHttpFileDownload(fbLink, dir, fileName);
            return uriList;
        }

        if (mAutoStream && uriList.isEmpty()) {
            onEndOrError(getString(R.string.error_playback, ""));
            return uriList;
        }
        return mMediaContentHandler.playIfVideo(uriList);
    }

    /**
     * Show BibleTool linked page if download failed.
     */
    public void showBibleToolHymnal() {
        if (isFinishing() || isDestroyed())
            return;
        String url = getHymnUri();
        initWebView(ContentHandler.UrlType.hymnBibleTool, url);
    }

    /**
     * Start to download the bibletool.online media when user click 下载： Mp3
     *
     * @param fbLink the download link provided.
     * @param mimeType the mimeType of the media to download.
     */
    public void startFileDownload(String fbLink, String mimeType) {
        if (mimeType.startsWith("video")) {
            mFileName = mFileName.replaceAll("[^.]+$", mimeType.split("/")[1]);
        }

        if (mimeType.startsWith("audio") || mimeType.startsWith("video")) {
            HymnsApp.showToastMessage(R.string.nq_download_starting, mFileName);
            mMediaDownloadHandler.initHttpFileDownload(fbLink, mDir, mFileName);
        }
        else {
            HymnsApp.showToastMessage(R.string.error_invalid_mimetype_download, mimeType);
        }
    }

    /**
     * Function use as fallback when download of MEDIA_CHANGSHI failed.
     * It proceeds to show the media content from bibletool.online site.
     *
     * @return the bibletool.online media url link.
     */
    public String getHymnUri() {
        String uri = null;
        String hymnType = HymnTypeMap.get(mHymnType);
        String subLink = "";
        String resName = "";

        mDir = mHymnType + MEDIA_CHANGSHI;
        String hymnTitle = getHymnTitle();
        String fileName = mHymnNo + hymnTitle;

        switch (mHymnType) {
        case HYMN_ER:
            for (int idx = 0; idx < category_er.length; idx++) {
                if (mHymnNo < category_er[idx]) {
                    subLink = String.format(Locale.CHINA, "%02d%s", (idx - 1), hymnCategoryEr[idx - 1]);
                    break;
                }
            }
            // Generate the resName for link creation
            resName = "C" + fileName;
            uri = String.format(Locale.CHINA, btMp3Link, hymnType, subLink, resName);
            break;

        case HYMN_XB:
            // dnlink for xB does not use the last hymn category for fetching
            for (int idx = 0; idx < category_xb.length; idx++) {
                if (mHymnNo < category_xb[idx]) {
                    subLink = String.format(Locale.CHINA, "%02d%s", idx, hymnCategoryXb[idx - 1]);
                    break;
                }
            }
            // Generate the resName for link creation
            resName = "X" + fileName;
            uri = String.format(Locale.CHINA, btMp3Link, hymnType, subLink, resName);
            break;

        case HYMN_XG:
            break;

        case HYMN_YB:
            hymnType = "其他诗歌";
            subLink = hymnCategoryYb[0];
            resName = "Q" + fileName;
            uri = String.format(Locale.CHINA, btMp3Link, hymnType, subLink, resName);
            break;

        case HYMN_BB:
            for (int idx = 0; idx < category_bb.length; idx++) {
                if (mHymnNo < category_bb[idx]) {
                    subLink = String.format(Locale.CHINA, "%02d%s", (idx - 1), hymnCategoryBb[idx - 1]);
                    break;
                }
            }
            resName = "B" + fileName;
            uri = String.format(Locale.CHINA, btMp3Link, hymnType, subLink, resName);
            break;

        case HYMN_DB:
            for (int idx = 0; idx < category_db.length; idx++) {
                if (mHymnNo < category_db[idx]) {
                    subLink = String.format(Locale.CHINA, "%02d%s", idx, hymnCategoryDb[idx - 1]);
                    break;
                }
            }
            // Generate the resName for link creation; DB uses lyricsPhrase
            resName = DB_Links.get(mHymnNo);
            if (resName == null) {
                if (mHymnNo > HYMN_DB_NO_MAX) {
                    resName = "DF" + (mHymnNo - HYMN_DB_NO_MAX) + lyricsPhrase;
                }
                else {
                    resName = "D" + mHymnNo + lyricsPhrase;
                }
            }
            uri = String.format(Locale.CHINA, btMp3Link, hymnType, subLink, resName);
            break;
        }

        // Use supported filename for DB Fu hymn when saving media file.
        resName = resName.replaceFirst("DF\\d+", "D" + mHymnNo);
        mFileName = resName + ".mp3";
        Timber.d("bibleTool: %s", uri);
        return uri;
    }

    /**
     * Search local Hymn media directory for wildCard media file for exact match of hymnNo optionally prefix with 0.
     * init the local media file URI path for play back if any else return false
     *
     * @param dir the media local dir
     * @param hymnNo the hymn No
     * @param uriList the media URI list
     *
     * @return true if local media file is found else false
     */
    public static boolean isFileExist(String dir, int hymnNo, List<Uri> uriList) {
        File hymnDir = FileBackend.getHymnchtvStore(dir, true);
        if (hymnDir != null) {
            final Function1<String, Boolean> matchesHymnNo = HymnFileName.matcher(hymnNo);
            File[] fileList = hymnDir.listFiles((d, name) -> matchesHymnNo.invoke(name));

            if (fileList != null && fileList.length != 0) {
                if (uriList != null) {
                    Timber.d("Hymn #%s; Media file found (%s): %s", hymnNo, fileList.length, fileList[0].getPath());
                    uriList.add(Uri.fromFile(fileList[0]));
                }
                return true;
            }
        }
        return false;
    }

    public static boolean isFileExist(MediaRecord mediaRecord) {
        if (mediaRecord != null) {
            String dir = mediaRecord.getHymnType() + MediaConfig.mediaDir.get(mediaRecord.getMediaType());
            return isFileExist(dir, mediaRecord.getHymnNo(), null);
        }
        return false;
    }

    /** A failing read counts as "no stored record" (logged), so the other media types are still evaluated. */
    private boolean hasStoredMediaRecord(MediaRecord mediaRecord) {
        try {
            return mDB.getMediaRecord(mediaRecord, false);
        }
        catch (RuntimeException e) {
            Timber.e(e, "Media state lookup failed: %s", mediaRecord);
            return false;
        }
    }

    /**
     * Get the local availability of the hymn media content for all mediaType. Reads the DB: AppExecutors.io only.
     *
     * @return array of media content availability for all mediaType
     */
    private boolean[] getHymnMediaState(String hymnType, int hymnNo) {
        boolean[] isAvailable = {false, false, false, false};

        // Check to see if HYMN_MEDIA is available for the given HymnType/HymnNo
        boolean isFu = hymnType.equals(HYMN_DB) && (hymnNo > HYMN_DB_NO_MAX);

        switch (hymnType) {
        case HYMN_ER:
        case HYMN_XB:
        case HYMN_XG:
        case HYMN_YB:
            break;

        case HYMN_BB:
            isAvailable[3] = HymnsApp.getFileResId(MIDI_BB + hymnNo, "raw") != 0;
            break;

        case HYMN_DB:
            isAvailable[3] = HymnsApp.getFileResId(MIDI_DB + hymnNo, "raw") != 0;
            break;
        }

        String dir;
        MediaType[] mediaTypes = MediaType.values();
        for (int i = 0; i < mediaTypes.length; i++) {
            MediaType mediaType = mediaTypes[i];
            MediaRecord mediaRecord = new MediaRecord(hymnType, hymnNo, isFu, mediaType);

            // Skip to next if state is already evaluated to true i.e. defined in DB media link
            if ((isAvailable[i] |= hasStoredMediaRecord(mediaRecord)))
                continue;

            switch (mediaType) {
            case HYMN_MEDIA:
                dir = hymnType + MEDIA_MEDIA;
                isAvailable[0] = isFileExist(dir, hymnNo, null);
                break;

            case HYMN_JIAOCHANG:
                dir = hymnType + MEDIA_JIAOCHANG;
                isAvailable[1] = isFileExist(dir, hymnNo, null);
                break;

            case HYMN_CHANGSHI:
                dir = hymnType + MEDIA_CHANGSHI;
                isAvailable[2] = isFileExist(dir, hymnNo, null);
                break;

            case HYMN_BANZOU:
                dir = hymnType + MEDIA_BANZOU;
                isAvailable[3] |= isFileExist(dir, hymnNo, null);
                break;
            }
        }
        return isAvailable;
    }

    /**
     * Generate the hymn fileName (remove all punctuation marks), and the lyricsPhrase
     * Currently use in  MP3 media fileName is: ? + hymnNo + hymnTitle + ".mp3"
     */
    private String getHymnTitle() {
        String pattern = "[，、‘’！：；。？]";
        String hymnTitle = getHymnInfo().split(":\\s|？|（")[1].replaceAll(pattern, "");
        // Strip off the hymn category prefix
        int idx = hymnTitle.lastIndexOf("－");
        if (idx != -1) {
            hymnTitle = hymnTitle.substring(idx + 1);
        }
        return hymnTitle;
    }

    /**
     * Get the hymn information for the media controller.
     * It always try to generate the possible lyricsPhrase for media download link
     *
     * @return the hymn info for display
     */
    public String getHymnInfo() {
        return getHymnInfo(null);
    }

    /**
     * The hymn info for the player title bar; the title is read from the Traditional Chinese lyrics of [variant]
     * (null: Simplified). Only the Simplified info may be used to build media file names and search phrases.
     */
    private String getHymnInfo(HantVariant variant) {
        String fileName = "";
        String hymnTitle = "";
        String hymnInfo = "";
        Resources res = getResources();

        if (mHymnNo == HYMN_BB_DUMMY) {
            return getString(R.string.hymn_no_chinese_lyrics, mHymnNoEng);
        }

        switch (mHymnType) {
        case HYMN_ER:
            fileName = LYRICS_ER_DIR + "er" + mHymnNo + ".txt";
            break;

        case HYMN_XG:
            fileName = LYRICS_XG_DIR + "xg" + mHymnNo + ".txt";
            break;

        case HYMN_XB:
            fileName = LYRICS_XB_DIR + "xb" + mHymnNo + ".txt";
            break;

        case HYMN_YB:
            String hymnTN = ybXTable.get(mHymnNo);
            if (hymnTN != null) {
                fileName = getHymnDir(hymnTN) + hymnTN + ".txt";
            }
            else {
                fileName = LYRICS_YB_DIR + "yb" + mHymnNo + ".txt";
            }
            break;

        case HYMN_BB:
            fileName = LYRICS_BB_DIR + "bb" + mHymnNo + ".txt";
            break;

        case HYMN_DB:
            fileName = LYRICS_DB_DIR + "db" + mHymnNo + ".txt";
            break;
        }

        if (variant != null) {
            String hantPath = LyricsAssets.hantPath(fileName, variant);
            if (hantPath != null) {
                fileName = hantPath;
            }
        }

        try {
            InputStream in2 = getResources().getAssets().open(fileName);
            byte[] buffer2 = new byte[in2.available()];
            if (in2.read(buffer2) == -1)
                return hymnInfo;

            String mResult = EncodingUtils.getString(buffer2, "utf-8");
            String[] mList = mResult.split("\r\n|\n");

            // fetch the hymn title with the category intact
            hymnTitle = mList[1];

            // Check the third line for additional info e.g.（诗篇二篇）（英1094）
            int idx = mList[2].indexOf("（");
            if (idx != -1) {
                hymnTitle = hymnTitle + mList[2].substring(idx);
            }

            // Do the best guess to find the first phrase from lyrics
            idx = 4;
            String tmp = "";
            while (tmp.length() < 6 && idx < mList.length) {
                tmp = mList[idx++];
            }

            lyricsPhrase = "";
            mList = tmp.split("[，、‘’！：；。？]");

            // Do not change value 5: is the magic length to extract correct hymn phrase for bibletool access.
            for (String s : mList) {
                if (lyricsPhrase.length() < 5) {
                    lyricsPhrase += s;
                }
            }
        }
        catch (IOException e) {
            Timber.w("Error getting info for hymn %s: %s", fileName, e.getMessage());
            hymnTitle += getString(R.string.error_file_not_found, fileName);
        }

        int resId = -1;
        switch (mHymnType) {
        case HYMN_ER:
            resId = R.string.hymn_title_mc_er;
            break;
        case HYMN_XB:
            resId = R.string.hymn_title_mc_xb;
            break;
        case HYMN_XG:
            resId = R.string.hymn_title_mc_xg;
            break;
        case HYMN_YB:
            resId = R.string.hymn_title_mc_yb;
            break;
        case HYMN_BB:
            resId = R.string.hymn_title_mc_bb;
            break;
        case HYMN_DB:
            resId = (mHymnNo > HYMN_DB_NO_MAX) ? R.string.hymn_title_mc_dbs : R.string.hymn_title_mc_db;
            break;
        }

        if (variant == null) {
            mHymnSearch = res.getString(resId, mHymnNo, lyricsPhrase);
        }
        hymnInfo = res.getString(resId, mHymnNo, hymnTitle);
        return hymnInfo;
    }

    public Integer getHymnNoEng() {
        return mHymnNoEng;
    }

    public int getHymnNo() {
        return mHymnNo;
    }

    public static String getHymnDir(String hymnTN) {
        if (hymnTN.startsWith("er")) {
            return LYRICS_ER_DIR;
        }
        if (hymnTN.startsWith("xb")) {
            return LYRICS_XB_DIR;
        }
        else if (hymnTN.startsWith("xg")) {
            return LYRICS_XG_DIR;
        }
        else if (hymnTN.startsWith("yb")) {
            return LYRICS_YB_DIR;
        }
        else {
            return hymnTN.startsWith("db") ? LYRICS_DB_DIR : LYRICS_BB_DIR;
        }
    }

    public void showNotionSite() {
        initWebView(UrlType.hymnNotionSearch, NotionRecord.getNotionSite(mHymnType, mHymnNo));
    }

    /**
     * Use android default browser for all web url access except for 'englishLyrics';
     * avoid reload webPage for englishLyrics if user accesses to the same english hymn no.
     * <p>
     * WebView UI is not user-friendly, and offers limited share links for youtube.com/google.com string search.
     * i.e. The webView does not offer all the share app, and excluded hymnchtv for user selection.
     *
     * @param type UrlType enum type
     */
    public void initWebView(UrlType type, String... url) {
        switch (type) {
        case englishLyrics:
            String HymnalLink = "https://www.hymnal.net/en/hymn/h/";
            mWebUrl = (mHymnNoEng == null) ? null : HymnalLink + mHymnNoEng;
            break;
        case hymnGoogleSearch:
            mWebUrl = (mHymnInfo == null) ? null : "https://www.google.com/search?q=" + mHymnSearch;
            break;
        case hymnYoutubeSearch:
            mWebUrl = (mHymnInfo == null) ? null : "https://m.youtube.com/results?search_query=" + mHymnSearch;
            break;
        case hymnNotionSearch:
            mWebUrl = ((url.length < 1) || (url[0] == null)) ? NotionRecord.HYMNCHTV_NOTION : url[0];
            break;
        case hymnQqSearch:
            mWebUrl = ((url.length < 1) || (url[0] == null)) ? QQRecord.HYMNCHTV_QQ_MAIN : url[0];
            break;
        case hymnBibleTool:
            mWebUrl = url[0];
            break;
        default:
            mWebUrl = null;
        }

        // Timber.d("Web URL link: %s", mWebUrl);
        if (mWebUrl == null) {
            HymnsApp.showToastMessage(R.string.error_media_url_invalid, type.toString());
            return;
        }

        // Proceed to use android default browser if it is not englishLyrics access

        if (UrlType.hymnNotionSearch == type && Build.MANUFACTURER.contains("HUAWEI")) {
            About.hymnUrlAccess(this, mWebUrl);
            return;
        }

        // 20260829: Change to use this implementation; Backkey will return to parent,
        FragmentManager fragmentManager = getSupportFragmentManager();
        WebViewFragment mWebFragment = (WebViewFragment) fragmentManager.findFragmentById(R.id.webView);
        if (mWebFragment == null) {
            mWebFragment = new WebViewFragment();
        }
        else {
            mWebFragment.initWebView(mWebUrl);
        }

        fragmentManager.beginTransaction()
                .replace(R.id.webView, mWebFragment)
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit();
        mWebView.setVisibility(View.VISIBLE);
    }

    public MediaRecord getMediaRecord() {
        boolean isFu = mHymnType.equals(HYMN_DB) && (mHymnNo > HYMN_DB_NO_MAX);
        return new MediaRecord(mHymnType, mHymnNo, isFu, MediaType.HYMN_MEDIA);
    }

    public String hymnType2Text(Context context) {
        int idx = MediaConfig.hymnTypeValue.indexOf(mHymnType);
        return idx < 0 ? mHymnType : MediaConfig.hymnTypeEntries(context).get(idx);
    }

    /** The "next" button and auto-next: one page forward from the page on screen (a swipe moves it too). */
    public boolean scrollNextHymn() {
        int nextIdx = mPager.getCurrentItem() + 1;
        int tmp = HymnIdx2NoConvert.hymnIdx2NoConvert(mHymnType, nextIdx)[0];
        if (tmp != -1) {
            Timber.e("Scroll next hymn: %s: (AutoStream: %s)", tmp, mAutoStream);
            hymnIdx = nextIdx;
            mPager.setCurrentItem(nextIdx);
            return true;
        }
        return false;
    }

    /**
     * Anything playing stops when the reader moves to another hymn, and so does auto-next (the reader took over).
     * A video or YouTube player is closed the way the back key does. Auto-next's own advance is exempt.
     */
    private void stopPlaybackForHymnChange() {
        if (mAutoAdvancing) {
            return;
        }
        if (isMediaPlayerUi) {
            closeMediaPlayerUi();
        }
        mMediaGuiController.stopForHymnChange();
        setAutoStream(false);
    }

    /** Leaves the video / YouTube player and restores the audio player card. */
    private void closeMediaPlayerUi() {
        mMediaContentHandler.releasePlayer();
        mMediaGuiController.initPlaybackSpeed();
        getSupportFragmentManager().beginTransaction().replace(R.id.mediaPlayer, mMediaGuiController).commit();
        onVideoClosed();
    }

    /**
     * fetch the link for the current selected hymn corresponding English lyrics
     *
     * @return the webLink for the English lyrics
     */
    public String getWebUrl() {
        return mWebUrl;
    }

    /*
     * This method handles the display of PlayerGui when screen orientation is rotated
     * Override onConfigurationChanged() so that media playback is smooth when device is rotated
     */
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ContentView contentView = (ContentView) mPagerAdapter.mFragments.get(mPager.getCurrentItem());
        if (contentView != null)
            contentView.setLyricsTextScale();

        // Portrait: the card or the capsule as recorded; landscape: the capsule, the card only while expanded by hand
        mPlayerSheet.onOrientationChanged(PlayerSheetState.isPortrait(newConfig.orientation));
    }
}