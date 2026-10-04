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

import static org.cog.hymnchtv.MainActivity.HYMN_BB;
import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.HYMN_ER;
import static org.cog.hymnchtv.MainActivity.HYMN_XB;
import static org.cog.hymnchtv.MainActivity.HYMN_XG;
import static org.cog.hymnchtv.MainActivity.HYMN_YB;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.utils.ZoomTextView.STEP_SCALE_FACTOR;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;

import org.cog.hymnchtv.glide.MyGlideApp;
import org.cog.hymnchtv.lyrics.HantVariant;
import org.cog.hymnchtv.lyrics.LyricsAssets;
import org.cog.hymnchtv.lyrics.LyricsLang;
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;
import org.cog.hymnchtv.mediaconfig.LyricsEnglishRecord;
import org.cog.hymnchtv.reading.DisplayMode;
import org.cog.hymnchtv.reading.DisplayModePolicy;
import org.cog.hymnchtv.reading.LyricsFaceSpec;
import org.cog.hymnchtv.reading.LyricsFont;
import org.cog.hymnchtv.reading.LyricsWeight;
import org.cog.hymnchtv.reading.LyricsScale;
import org.cog.hymnchtv.reading.LyricsTypefaces;
import org.cog.hymnchtv.reading.ReadingPrefKeys;
import org.cog.hymnchtv.reading.ReadingPrefs;
import org.cog.hymnchtv.reading.ScorePages;
import org.cog.hymnchtv.reading.ScoreTintPolicy;
import org.cog.hymnchtv.reading.background.BackgroundDrawables;
import org.cog.hymnchtv.reading.background.ReadingPalette;
import org.cog.hymnchtv.reading.background.UiTokens;
import org.cog.hymnchtv.ui.lyrics.ChromeButtonStyle;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
import org.cog.hymnchtv.ui.lyrics.ChromePage;
import org.cog.hymnchtv.ui.lyrics.LyricsInsets;
import org.cog.hymnchtv.ui.lyrics.LyricsMeta;
import org.cog.hymnchtv.ui.lyrics.LyricsStyle;
import org.cog.hymnchtv.ui.lyrics.LyricsTypography;
import org.cog.hymnchtv.ui.lyrics.LyricsPadding;
import org.cog.hymnchtv.ui.motion.Motion;
import org.cog.hymnchtv.utils.NestedScrollableHost;
import org.cog.hymnchtv.utils.ZoomTextView;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class displays the hymn lyrics content selected by user;
 * It is a part of the whole Hymn lyrics content UI display
 * Note: The context menu needs to be created here, instead its parent, for it to be visible
 *
 * @author Eng Chong Meng
 */
public class ContentView extends Fragment implements ZoomTextView.ZoomTextListener, View.OnClickListener,
        LyricsEnglishRecord.EnglishLyricsListener, ChromePage {
    public static String SCORE_DB_DIR = "lyrics_db_score/";
    public static String SCORE_BB_DIR = "lyrics_bb_score/";
    public static String SCORE_ER_DIR = "lyrics_er_score/";
    public static String SCORE_XB_DIR = "lyrics_xb_score/";
    public static String SCORE_XG_DIR = "lyrics_xg_score/";
    public static String SCORE_YB_DIR = "lyrics_yb_score/";

    public static String LYRICS_DB_DIR = "lyrics_db_text/";
    public static String LYRICS_BB_DIR = "lyrics_bb_text/";
    public static String LYRICS_ER_DIR = "lyrics_er_text/";
    public static String LYRICS_XB_DIR = "lyrics_xb_text/";
    public static String LYRICS_XG_DIR = "lyrics_xg_text/";
    public static String LYRICS_YB_DIR = "lyrics_yb_text/";

    public static String LYRICS_TOC = "lyrics_toc/";

    private static final long CHROME_FADE_MS = 150;
    private static final int LYRICS_BOTTOM_EXTRA_DP = 8;

    public final static String LYRICS_TYPE = "lyricsType";
    public final static String LYRICS_INDEX = "lyricsIndex";

    public static final String EXTR_KEY_HAS_CHANGES = "hasChanges";
    public final static String PREF_SCORE_COLOR = "ScoreColor";
    public static final String PREF_CONVERSION_TYPE = "ConversionType";
    public static final String PREF_LYRICS_SCALE_P = ReadingPrefKeys.LYRICS_SCALE_P;
    public static final String PREF_LYRICS_SCALE_L = ReadingPrefKeys.LYRICS_SCALE_L;
    public static final String PREF_LYRICS_ENGLISH_SCALE_P = "LyricsScaleEP";
    public static final String PREF_LYRICS_ENGLISH_SCALE_L = "LyricsScaleEL";

    /** One view per score page: the hymn itself, then suffixes a-d (plan A2: page 5 no longer reuses page 4's view). */
    private static final int[] SCORE_VIEW_IDS = {R.id.contentView, R.id.contentView_a, R.id.contentView_b,
            R.id.contentView_c, R.id.contentView_d};

    public ContentHandler mContentHandler;
    private LyricsEnglishRecord mLyricsEnglishRecord;

    private Button btn_ts;
    private Button btn_english;
    private Button btn_mode;
    private View mConvertView;
    private View topBar;
    private View buttonBar;
    private ScrollView lyricsScroll;
    private boolean mChromeVisible = false; // lyrics only on open; the bars start hidden
    private View lyricsView;
    private View scoreContainer;
    private ZoomTextView lyricsSimplify;
    private ZoomTextView lyricsTraditional;
    private TextView meterKeyView;
    private View infoRowView;
    private ImageView favoriteStarView;
    /** Bumped by every applyFont: a background face load only applies if no newer choice was made meanwhile. */
    private int mFontRequest;
    private WebView lyricsEnglish;

    /** Key and time signature per script ("大调" / "大調"); null when the lyrics header has none. */
    private String mLyricsSimplifiedPlain;
    private String mLyricsTraditionalPlain;
    private String mMeterKeySimplified;
    private String mMeterKeyTraditional;

    private Integer mHymnNoEng = null;
    private boolean isErGe;
    private boolean mLyricsLoaded = false;
    private boolean hasEnglishLyrics = false;
    private boolean mScoreLoaded = false;
    private boolean mHasLyricsText = false;

    /** Stored default display mode, read once per onCreateView/onResume. */
    /** Line spacing multiplier of the lyrics text views (also in content_lyrics.xml). */
    private static final float LYRICS_LINE_SPACING = 1.7f;

    private DisplayMode mStoredDisplayMode = DisplayMode.LYRICS_ONLY;

    /** Score colour level from the context menu; 0 = automatic (follows the background, see ScoreTintPolicy). */
    private static int mScoreColor = 0;

    private static float lyricsScaleP;
    private static float lyricsScaleL;
    private static float lyricsScaleEP;
    private static float lyricsScaleEL;

    private ReadingPalette mPalette;
    private String mResPrefix;
    private int[] mHymnScoreInfo;
    /** The hymn this page shows; the favourite star follows the key of this page, not of the current one. */
    private String mPageHymnType;
    private int mPageHymnNo;

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mEditor;

// Need this to prevent crash on rotation if there are other constructors implementation
// public ContentView() { }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContentHandler = (ContentHandler) context;

        mLyricsEnglishRecord = LyricsEnglishRecord.getInstanceFor(mContentHandler);
        mLyricsEnglishRecord.registerLyricsListener(this);

        mSharedPref = mContentHandler.getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mConvertView = inflater.inflate(R.layout.content_lyrics, container, false);
        scoreContainer = mConvertView.findViewById(R.id.scoreContainer);

        btn_ts = mConvertView.findViewById(R.id.button_ts);
        btn_ts.setOnClickListener(this);

        btn_english = mConvertView.findViewById(R.id.button_english);
        btn_english.setOnClickListener(this);

        for (int id : new int[]{R.id.btn_home, R.id.btn_share, R.id.btn_lyrics_media, R.id.btn_aa, R.id.btn_next, R.id.btn_more}) {
            mConvertView.findViewById(id).setOnClickListener(this);
        }

        topBar = mConvertView.findViewById(R.id.lyrics_top_bar);
        buttonBar = mConvertView.findViewById(R.id.lyricsButtonBar);
        lyricsScroll = mConvertView.findViewById(R.id.lyrics_scroll);
        // Toolbar heights change with font scale and orientation: keep the lyrics padding in step
        View.OnLayoutChangeListener insetsFollowBars = (v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t != ob - ot) {
                applyLyricsInsets();
            }
        };
        topBar.addOnLayoutChangeListener(insetsFollowBars);
        buttonBar.addOnLayoutChangeListener(insetsFollowBars);
        ((NestedScrollableHost) mConvertView.findViewById(R.id.lyrics_scroll_host))
                .setOnCenterTapListener(mContentHandler::onLyricsCenterTap);
        ChromeButtonStyle.styleBar((ViewGroup) topBar, mContentHandler.getLyricsTokens());
        ChromeButtonStyle.styleBar((ViewGroup) buttonBar, mContentHandler.getLyricsTokens());
        mContentHandler.registerChromePage(this);

        btn_mode = mConvertView.findViewById(R.id.button_mode);
        btn_mode.setOnClickListener(this);

        lyricsView = mConvertView.findViewById(R.id.lyricsView);
        lyricsSimplify = mConvertView.findViewById(R.id.lyrics_simplified);
        lyricsSimplify.registerZoomTextListener(this);

        lyricsTraditional = mConvertView.findViewById(R.id.lyrics_traditional);
        lyricsTraditional.registerZoomTextListener(this);

        meterKeyView = mConvertView.findViewById(R.id.meter_key);
        infoRowView = mConvertView.findViewById(R.id.lyrics_info_row);
        favoriteStarView = mConvertView.findViewById(R.id.favorite_star);
        lyricsEnglish = mConvertView.findViewById(R.id.lyrics_english);

        mStoredDisplayMode = ReadingPrefs.displayMode(mSharedPref);
        mPalette = mContentHandler.getLyricsPalette();
        applyPaletteAndFont();

        lyricsScaleP = ReadingPrefs.lyricsScale(mSharedPref, true);
        lyricsScaleL = ReadingPrefs.lyricsScale(mSharedPref, false);
        lyricsScaleEP = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_P, 1.0f);
        lyricsScaleEL = mSharedPref.getFloat(PREF_LYRICS_ENGLISH_SCALE_L, 1.0f);

        mScoreColor = mSharedPref.getInt(PREF_SCORE_COLOR, 0);

        mLyricsLoaded = false;
        hasEnglishLyrics = false;
        mScoreLoaded = false;
        mHasLyricsText = false;

        Bundle bundle = getArguments();
        if (bundle != null) {
            String lyricsType = getArguments().getString(LYRICS_TYPE);
            int lyricsIndex = getArguments().getInt(LYRICS_INDEX);

            if (!TextUtils.isEmpty(lyricsType)) {
                updateHymnContent(lyricsType, lyricsIndex);
            }
        }
        applyDisplayMode(true);
        return mConvertView;
    }

    @Override
    public void onDestroyView() {
        mContentHandler.unregisterChromePage(this);
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        mStoredDisplayMode = ReadingPrefs.displayMode(mSharedPref);
        Timber.w("Content View on Resume");

        // get the corresponding English lyrics# or null if none
        mHymnNoEng = mContentHandler.getHymnNoEng();
        boolean autoEnglish = mContentHandler.mAutoEnglish;
        if (autoEnglish) {
            // autoload English lyrics for first entry only.
            mContentHandler.mAutoEnglish = false;
            hasEnglishLyrics = true;
        }
        // ViewPager2 only resumes the visible page: re-apply toggles (script, display mode) made on another page.
        // Do not reload English lyrics that are already showing.
        applyDisplayMode(autoEnglish || !hasEnglishLyrics);
    }

    /** The "more" button of the top bar: the entries that have no button of their own. */
    /**
     * Puts the transparent shared-element target over the header number (line 0 of the lyrics text) and names it for the
     * home to lyrics transition. Returns true once the target sits where the number is drawn.
     */
    public boolean placeNumberAnchor() {
        View anchor = mConvertView == null ? null : mConvertView.findViewById(R.id.lyrics_number_anchor);
        TextView text = isShowTraditional() ? lyricsTraditional : lyricsSimplify;
        if (anchor == null || text == null || text.getLayout() == null || text.getLayout().getLineCount() == 0
                || text.getVisibility() != View.VISIBLE) {
            return false;
        }
        Layout layout = text.getLayout();
        Rect line = new Rect();
        layout.getLineBounds(0, line);
        int[] textAt = new int[2];
        int[] frameAt = new int[2];
        text.getLocationInWindow(textAt);
        mConvertView.getLocationInWindow(frameAt);
        int width = Math.max(1, (int) Math.ceil(layout.getLineWidth(0)));
        int height = Math.max(1, line.height());
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) anchor.getLayoutParams();
        int left = textAt[0] - frameAt[0] + text.getTotalPaddingLeft() + (int) layout.getLineLeft(0);
        int top = textAt[1] - frameAt[1] + text.getTotalPaddingTop() + line.top;
        boolean placed = lp.width == width && lp.height == height && lp.leftMargin == left && lp.topMargin == top;
        if (!placed) {
            lp.width = width;
            lp.height = height;
            lp.leftMargin = left;
            lp.topMargin = top;
            anchor.setLayoutParams(lp);
            return false;
        }
        ViewCompat.setTransitionName(anchor, Motion.SHARED_NUMBER);
        return true;
    }

    public void clearNumberAnchor() {
        View anchor = mConvertView == null ? null : mConvertView.findViewById(R.id.lyrics_number_anchor);
        if (anchor != null) {
            ViewCompat.setTransitionName(anchor, null);
        }
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(new ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_Hymnal_LyricsMenu), anchor);
        popup.inflate(R.menu.menu_lyrics_more);
        // Hide the English entries if there are no associated English lyrics
        popup.getMenu().findItem(R.id.lyrcsEnglish).setVisible(mHymnNoEng != null);
        popup.getMenu().findItem(R.id.lyrcsEnglishDelete).setVisible(mHymnNoEng != null && hasEnglishLyrics);
        MenuItem favorite = popup.getMenu().findItem(R.id.favorite);
        favorite.setTitle(isFavoriteMarked() ? R.string.fav_remove : R.string.fav_add);
        favorite.setEnabled(mContentHandler.canToggleFavorite());
        popup.setOnMenuItemClickListener(item -> mContentHandler.onLyricsAction(item.getItemId()));
        // The toolbars must not fade away under an open menu
        mContentHandler.setChromeHeld(true);
        popup.setOnDismissListener(menu -> mContentHandler.setChromeHeld(false));
        popup.show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        mContentHandler.onChromeInteraction();
        if (id == R.id.btn_aa) {
            mContentHandler.showReadingPanel();
        }
        else if (id == R.id.btn_home) {
            mContentHandler.onLyricsAction(R.id.home);
        }
        else if (id == R.id.btn_share) {
            mContentHandler.onLyricsAction(R.id.lyrcsShare);
        }
        else if (id == R.id.btn_lyrics_media) {
            mContentHandler.onLyricsAction(R.id.media_config);
        }
        else if (id == R.id.btn_next) {
            mContentHandler.scrollNextHymn();
        }
        else if (id == R.id.btn_more) {
            showMoreMenu(v);
        }
        else if (id == R.id.button_ts) {
            if (!hasEnglishLyrics) {
                // Session-only toggle; the persisted default is set in the reading settings
                mContentHandler.lyricsViewOverride = !isShowTraditional();
            }
            else {
                hasEnglishLyrics = false;
            }
            toggleLyricsView();
        }
        else if (id == R.id.button_english) {
            hasEnglishLyrics = !hasEnglishLyrics;
            toggleLyricsView();
        }
        else if (id == R.id.button_mode) {
            // Session-only toggle (plan A2); the persisted default is set in the reading settings
            mContentHandler.displayModeOverride = currentDisplayMode().next();
            applyDisplayMode(true);
        }
    }

    /**
     * The lyrics png/jpg file has the following formats: HYMN_ER, HYMN_XB, HYMN_XG, HYMN_YB, HYMN_BB, HYMN_DB
     * i.e. er, xb, xg, yb, bb, db followed by the hymn number, a, b, c etc for more than one page;
     * The files are stored in asset respective sub-dir e.g. LYRICS_XB_SCORE
     * The content view can support up to 5 pages for user vertical scrolls
     *
     * @param hymnType see below cases
     * @param hymnIndex hymn index provided by the page adapter when user scroll
     */
    private void updateHymnContent(String hymnType, int hymnIndex) {
        String resFName;
        mHymnScoreInfo = HymnIdx2NoConvert.hymnIdx2NoConvert(hymnType, hymnIndex);

        // Chinese lyrics#
        int lyricsNo = mHymnScoreInfo[0];
        mPageHymnType = hymnType;
        mPageHymnNo = lyricsNo;
        isErGe = HYMN_ER.equals(hymnType);

        switch (hymnType) {
        case HYMN_ER:
            mResPrefix = SCORE_ER_DIR + lyricsNo;
            resFName = LYRICS_ER_DIR + "er" + lyricsNo + ".txt";
            break;

        case HYMN_XB:
            mResPrefix = SCORE_XB_DIR + "xb" + lyricsNo;
            resFName = LYRICS_XB_DIR + "xb" + lyricsNo + ".txt";
            break;

        case HYMN_XG:
            mResPrefix = SCORE_XG_DIR + "xg" + lyricsNo;
            resFName = LYRICS_XG_DIR + "xg" + lyricsNo + ".txt";
            break;

        case HYMN_YB:
            mResPrefix = SCORE_YB_DIR + "yb" + lyricsNo;
            resFName = LYRICS_YB_DIR + "yb" + lyricsNo + ".txt";
            break;

        case HYMN_BB:
            mResPrefix = SCORE_BB_DIR + "bb" + lyricsNo;
            resFName = LYRICS_BB_DIR + "bb" + lyricsNo + ".txt";
            break;

        case HYMN_DB:
            mResPrefix = SCORE_DB_DIR + "db" + lyricsNo;
            resFName = LYRICS_DB_DIR + "db" + lyricsNo + ".txt";
            break;

        default:
            Timber.e("Unsupported content type: %s", hymnType);
            return;
        }

        // Text first (plan A2): it is a few KB and decides whether "lyrics only" can be honoured
        if (!TextUtils.isEmpty(resFName)) {
            setLyricsTextScale();
            showLyricsChText(resFName);
        }

        // The score images are the expensive part; "lyrics only" skips them until the mode changes
        if (DisplayModePolicy.effective(currentDisplayMode(), mHasLyricsText).getShowScore()) {
            showLyricsScore(mResPrefix, mHymnScoreInfo);
        }
    }

    /** Score colour for the current manual level and lyrics background (plan A2). */
    @Nullable
    private ColorFilter scoreColorFilter() {
        float[] matrix = ScoreTintPolicy.matrix(ScoreTintPolicy.resolve(mScoreColor, mPalette.isDark(),
                mPalette.getPaperColor(), mPalette.getTextColor()));
        return (matrix == null) ? null : new ColorMatrixColorFilter(matrix);
    }

    public void toggleScoreColor() {
        mScoreColor = (mScoreColor + 1) % ScoreTintPolicy.LEVEL_COUNT;
        mEditor.putInt(PREF_SCORE_COLOR, mScoreColor);
        mEditor.apply();
        applyScoreFilter();
    }

    private void applyScoreFilter() {
        ColorFilter filter = scoreColorFilter();
        for (int id : SCORE_VIEW_IDS) {
            ImageView view = mConvertView.findViewById(id);
            view.setColorFilter(filter);
        }
    }

    /**
     * Display the selected Hymn Lyric Scores. Scores with multi-pages have suffixed with a, b, c and d.
     * i.e. support a total of 5 pages maximum.
     *
     * @param resPrefix The selected Hymn Lyric scores fileName prefix
     * @param hymnScoreInfo Contain info for the hymnNo and number of pages of the selected Lyric Scores
     */
    private void showLyricsScore(String resPrefix, int[] hymnScoreInfo) {
        Context ctx = getContext();
        ColorFilter filter = scoreColorFilter();
        List<String> names = ScorePages.fileNames(resPrefix, hymnScoreInfo[1]);
        for (int i = 0; i < SCORE_VIEW_IDS.length; i++) {
            ImageView view = mConvertView.findViewById(SCORE_VIEW_IDS[i]);
            if (i < names.size()) {
                view.setVisibility(View.VISIBLE);
                view.setColorFilter(filter);
                MyGlideApp.loadImage(ctx, view, names.get(i));
            }
            else {
                view.setVisibility(View.GONE);
            }
        }
        mScoreLoaded = true;
    }

    /**
     * Display the selected hymn lyrics text
     *
     * @param resFName Lyrics text resource fileName
     */
    private void showLyricsChText(String resFName) {
        String lyrics = readAsset(resFName);
        if (lyrics != null) {
            // The key/meter line is shown by meter_key instead of inside the text
            String traditional = loadTraditional(resFName, lyrics);
            mMeterKeySimplified = LyricsMeta.parseMeterKey(Arrays.asList(lyrics.split("\n", -1)));
            mMeterKeyTraditional = LyricsMeta.parseMeterKey(Arrays.asList(traditional.split("\n", -1)));
            mLyricsSimplifiedPlain = LyricsMeta.removeMeterLine(lyrics);
            mLyricsTraditionalPlain = LyricsMeta.removeMeterLine(traditional);
            applyVerseColors();
        }
        mHasLyricsText = DisplayModePolicy.hasLyricsText(lyrics);

        // Auto launch or hint user to view lyrics text via online JiaoChang if available; er,length > 47.
        // Not in "score only": the reader asked not to see lyrics.
        if (!mHasLyricsText && currentDisplayMode() != DisplayMode.SCORE_ONLY) {
            mContentHandler.selectJC();
        }
    }

    /** Verse numbers: red on light backgrounds; on dark ones (and photos) red cannot reach AA contrast, so the accent colour. */
    private void applyVerseColors() {
        if (mLyricsSimplifiedPlain == null || mLyricsTraditionalPlain == null) {
            return;
        }
        int verseColor = mPalette.isDark() ? mPalette.getAccentColor()
                : ContextCompat.getColor(mContentHandler, R.color.c_verse_red);
        String book = mContentHandler.hymnType2Text(mContentHandler);
        lyricsSimplify.setText(LyricsTypography.apply(mLyricsSimplifiedPlain, book, lyricsStyle(lyricsSimplify, verseColor)));
        lyricsTraditional.setText(LyricsTypography.apply(mLyricsTraditionalPlain, book, lyricsStyle(lyricsTraditional, verseColor)));
    }

    private LyricsStyle lyricsStyle(ZoomTextView view, int verseColor) {
        android.content.res.Resources res = getResources();
        return new LyricsStyle(mPalette.getAccentColor(), verseColor, mContentHandler.getLyricsTokens().getOnSurfaceMuted(),
                res.getDimensionPixelSize(R.dimen.lyrics_header_number), res.getDimensionPixelSize(R.dimen.lyrics_header_title),
                res.getDimensionPixelSize(R.dimen.lyrics_header_book), res.getDimensionPixelSize(R.dimen.lyrics_header_gap),
                res.getDimensionPixelSize(R.dimen.lyrics_chorus_bar), LYRICS_LINE_SPACING, view);
    }

    /**
     * The background changed (Aa panel, settings): re-colour this page in place. Single page-level entry of
     * ContentHandler.applyReadingTheme(); a page created later reads the same palette and tokens in onCreateView.
     */
    public void applyTheme(@NonNull ReadingPalette palette, @NonNull UiTokens tokens) {
        if (mConvertView == null) {
            return;
        }
        mPalette = palette;
        applyPaletteAndFont();
        applyVerseColors();
        applyScoreFilter();
        ChromeButtonStyle.styleBar((ViewGroup) topBar, tokens);
        ChromeButtonStyle.styleBar((ViewGroup) buttonBar, tokens);
        if (DisplayModePolicy.refreshEnglishOnTheme(hasEnglishLyrics, currentDisplayMode(), mHasLyricsText)) {
            toggleLyricsView(); // the English HTML is generated for the background brightness
        }
    }

    /**
     * Re-read the reading preferences that the Aa panel changes (size, typeface, display mode) and apply them to
     * the page on screen, without recreating it. Pages whose view does not exist yet read the same values in
     * onCreateView, so the caller only needs to reach pages that have a view.
     */
    public void applyReadingPrefs() {
        if (mConvertView == null || !isAdded()) {
            return;
        }
        mStoredDisplayMode = ReadingPrefs.displayMode(mSharedPref);
        lyricsScaleP = ReadingPrefs.lyricsScale(mSharedPref, true);
        lyricsScaleL = ReadingPrefs.lyricsScale(mSharedPref, false);
        setLyricsTextScale();
        applyPaletteAndFont();
        applyDisplayMode(true);
    }

    /** Pre-generated Traditional lyrics (plan A.1.9); the sync test guarantees they exist, Simplified is a last resort. */
    private String loadTraditional(String resFName, String simplified) {
        HantVariant variant = LyricsLanguagePolicy.parseVariant(mSharedPref.getString(PREF_CONVERSION_TYPE, null), uiLocale());
        String hantPath = LyricsAssets.hantPath(resFName, variant);
        String text = (hantPath == null) ? null : readAsset(hantPath);
        if (text != null) {
            return text;
        }
        Timber.w("Missing pre-generated lyrics for %s (%s); showing Simplified", resFName, hantPath);
        return simplified;
    }

    /** @return the asset text with '\n' line ends, or null if it cannot be read. */
    private String readAsset(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getResources().getAssets().open(path), StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
            return text.toString();
        }
        catch (IOException e) {
            Timber.w("Error reading file: %s", path);
            return null;
        }
    }

    /** Text colour, link/selection colours, text backdrop and typeface from the reading settings (plan A2). */
    private void applyPaletteAndFont() {
        boolean kai = ReadingPrefs.lyricsFont(mSharedPref) == LyricsFont.KAI;
        styleLyrics(lyricsSimplify);
        styleLyrics(lyricsTraditional);
        applyFont(isShowTraditional());
        // Photo backgrounds: English lyrics also sit on the contrast-tested panel (PhotoPaletteTest).
        // The WebView's own colour stays transparent; the panel is the View background (set last, so it wins).
        lyricsEnglish.setBackgroundColor(Color.TRANSPARENT);
        lyricsEnglish.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
    }

    /**
     * Font for the script on screen only; never blocks. The other script loads when first shown.
     * Until the face is ready the system font is used, then it is swapped in on the main thread.
     */
    private void applyFont(boolean traditional) {
        int request = ++mFontRequest;
        ZoomTextView view = traditional ? lyricsTraditional : lyricsSimplify;
        LyricsWeight weight = ReadingPrefs.lyricsWeight(mSharedPref);
        LyricsFaceSpec spec = LyricsFaceSpec.choose(ReadingPrefs.lyricsFont(mSharedPref), weight, traditional);
        if (spec instanceof LyricsFaceSpec.System) {
            applyFace(view, LyricsTypefaces.systemFace(((LyricsFaceSpec.System) spec).getWeight(), Build.VERSION.SDK_INT), false);
            meterKeyView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            return;
        }
        boolean medium = weight != LyricsWeight.REGULAR;
        boolean fakeBold = ((LyricsFaceSpec.Kai) spec).getFakeBold();
        Typeface ready = LyricsTypefaces.peek(traditional, medium);
        if (ready != null) {
            applyFace(view, ready, fakeBold);
            meterKeyView.setTypeface(ready, Typeface.BOLD);
            return;
        }
        applyFace(view, Typeface.DEFAULT, false);
        LyricsTypefaces.request(mContentHandler, traditional, medium, face -> {
            // A later choice (another weight, font or script) owns the view now; this late face must not undo it
            if (isAdded() && request == mFontRequest) {
                applyFace(view, face, fakeBold);
                meterKeyView.setTypeface(face, Typeface.BOLD);
            }
        });
    }

    /**
     * Lyrics text only (never the score images): the typeface plus the Bold step's fake-bold stroke. The stroke goes
     * through TextView.setPaintFlags, which rebuilds the text layout: the lyrics are selectable, so hardware rendering
     * draws them from cached text display lists that a paint change plus invalidate() does not refresh (Medium and Bold
     * share one typeface, so nothing else would rebuild them).
     */
    private static void applyFace(ZoomTextView view, Typeface face, boolean fakeBold) {
        view.setTypeface(face);
        int flags = view.getPaintFlags();
        view.setPaintFlags(fakeBold ? flags | Paint.FAKE_BOLD_TEXT_FLAG : flags & ~Paint.FAKE_BOLD_TEXT_FLAG);
    }

    private void styleLyrics(ZoomTextView view) {
        int accent = mPalette.getAccentColor();
        view.setTextColor(mPalette.getTextColor());
        meterKeyView.setTextColor(accent);
        ImageViewCompat.setImageTintList(favoriteStarView, ColorStateList.valueOf(accent));
        view.setLinkTextColor(accent);
        view.setHighlightColor((accent & 0x00FFFFFF) | 0x40000000);
        // null for drawn backgrounds; an 85 % panel for photos
        view.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
    }

    /**
     * Update the lyrics text view default size and the stored scale factor
     * Also being used onConfiguration change
     */
    /** Read the live configuration: HymnsApp.isPortrait may not be updated yet while the activity rotates. */
    private boolean isPortraitNow() {
        return getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT;
    }

    public void setLyricsTextScale() {
        if (lyricsEnglish == null || lyricsSimplify == null || lyricsTraditional == null) {
            Timber.e(new Exception("Lyrics content view is null"));
            return;
        }

        // English lyrics text size in webSettings
        final WebSettings webSettings = lyricsEnglish.getSettings();

        if (isPortraitNow()) {
            lyricsSimplify.scaleTextSize(LyricsScale.BASE_SP_PORTRAIT, lyricsScaleP);
            lyricsTraditional.scaleTextSize(LyricsScale.BASE_SP_PORTRAIT, lyricsScaleP);
            webSettings.setDefaultFontSize((int) (18 * lyricsScaleEP));
        }
        else {
            lyricsSimplify.scaleTextSize(LyricsScale.BASE_SP_LANDSCAPE, lyricsScaleL);
            lyricsTraditional.scaleTextSize(LyricsScale.BASE_SP_LANDSCAPE, lyricsScaleL);
            webSettings.setDefaultFontSize((int) (26 * lyricsScaleEL));
        }
    }

    /**
     * Increase or decrease the lyrics text scale factor
     *
     * @param stepInc true if size increment else decrement
     */
    public void setLyricsTextSize(boolean stepInc) {
        if (lyricsEnglish.getVisibility() == View.VISIBLE) {
            setLyricsEnglishTextScale(stepInc);
        }
        else {
            float scaleFactor = lyricsSimplify.onTextSizeChange(stepInc);
            lyricsTraditional.onTextSizeChange(stepInc);
            updateTextScale(scaleFactor);
        }
    }

    // Handler for english lyrics textSize changes
    private void setLyricsEnglishTextScale(boolean stepInc) {
        float tmpScale = stepInc ? STEP_SCALE_FACTOR : -STEP_SCALE_FACTOR;

        if (isPortraitNow()) {
            lyricsScaleEP += tmpScale;
            mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_P, lyricsScaleEP);
        }
        else {
            lyricsScaleEL += tmpScale;
            mEditor.putFloat(PREF_LYRICS_ENGLISH_SCALE_L, lyricsScaleEL);
        }
        mEditor.apply();
        setLyricsTextScale();
    }

    /**
     * Save the user selected scale factory to preference settings
     *
     * @param scaleFactor scale factor
     */
    @Override
    public void updateTextScale(Float scaleFactor) {
        if (isPortraitNow()) {
            lyricsScaleP = scaleFactor;
            mEditor.putFloat(PREF_LYRICS_SCALE_P, scaleFactor);
        }
        else {
            lyricsScaleL = scaleFactor;
            mEditor.putFloat(PREF_LYRICS_SCALE_L, scaleFactor);
        }
        mEditor.apply();
    }

    /** Stored default, or this session's button_mode choice. */
    private DisplayMode currentDisplayMode() {
        return DisplayModePolicy.resolve(mContentHandler.displayModeOverride, mStoredDisplayMode);
    }

    /**
     * Show/hide score, buttons and lyrics for the display mode (plan A2).
     *
     * @param refreshLyrics false keeps the current lyrics views as they are (avoids re-fetching English lyrics)
     */
    private void applyDisplayMode(boolean refreshLyrics) {
        DisplayMode chosen = currentDisplayMode();
        DisplayMode shown = DisplayModePolicy.effective(chosen, mHasLyricsText);
        btn_mode.setText(displayModeShortLabel(chosen));
        btn_mode.setContentDescription(getString(R.string.c_cd_mode, getString(displayModeLabel(chosen))));

        if (shown.getShowScore() && !mScoreLoaded && mResPrefix != null) {
            showLyricsScore(mResPrefix, mHymnScoreInfo);
        }
        else if (!shown.getShowScore() && mScoreLoaded) {
            releaseScores();
        }
        scoreContainer.setVisibility(shown.getShowScore() ? View.VISIBLE : View.GONE);

        // Score only: script and language buttons keep their place (three equal columns) but are disabled
        setScriptButtonsEnabled(shown.getShowLyrics());
        btn_english.setVisibility(mHymnNoEng != null ? View.VISIBLE : View.GONE);
        if (!shown.getShowLyrics()) {
            lyricsSimplify.setVisibility(View.GONE);
            lyricsTraditional.setVisibility(View.GONE);
            lyricsEnglish.setVisibility(View.GONE);
        }
        else if (refreshLyrics || noLyricsViewShown()) {
            // Also when every lyrics view was hidden by an earlier mode (e.g. English page -> score only -> back)
            toggleLyricsView();
        }
    }

    private boolean noLyricsViewShown() {
        return lyricsSimplify.getVisibility() != View.VISIBLE && lyricsTraditional.getVisibility() != View.VISIBLE
                && lyricsEnglish.getVisibility() != View.VISIBLE;
    }

    /** "Lyrics only": let Glide recycle the score bitmaps; switching back reloads them (Codex P2). */
    private void releaseScores() {
        for (int id : SCORE_VIEW_IDS) {
            ImageView view = mConvertView.findViewById(id);
            Glide.with(this).clear(view);
        }
        mScoreLoaded = false;
    }

    private void setScriptButtonsEnabled(boolean enabled) {
        btn_ts.setEnabled(enabled);
        btn_english.setEnabled(enabled);
        btn_ts.setContentDescription(getString(enabled ? R.string.c_cd_script : R.string.c_cd_score_only_na));
        btn_english.setContentDescription(getString(enabled ? R.string.c_cd_cn_en : R.string.c_cd_score_only_na));
    }

    private static int displayModeShortLabel(DisplayMode mode) {
        switch (mode) {
        case SCORE_ONLY:
            return R.string.c_btn_mode_score;
        case LYRICS_ONLY:
            return R.string.c_btn_mode_lyrics;
        default:
            return R.string.c_btn_mode_both;
        }
    }

    private static int displayModeLabel(DisplayMode mode) {
        switch (mode) {
        case SCORE_ONLY:
            return R.string.display_mode_score;
        case LYRICS_ONLY:
            return R.string.display_mode_lyrics;
        default:
            return R.string.display_mode_both;
        }
    }

    private void toggleLyricsView() {
        lyricsTraditional.setVisibility(View.GONE);
        lyricsSimplify.setVisibility(View.GONE);
        lyricsEnglish.setVisibility(View.GONE);

        if (hasEnglishLyrics && mHymnNoEng != null) {
            lyricsEnglish.setVisibility(View.VISIBLE);
            Timber.d("Lyrics English #%s loaded: %s", mHymnNoEng, mLyricsLoaded);
            if (!mLyricsLoaded) {
                showLyricsEnglish(LyricsEnglishRecord.str2Html("<h3>" + getResources().getString(R.string.download_wait) + "</h3>"), false);
            }
            mLyricsEnglishRecord.fetchLyrics(mHymnNoEng, isErGe);
        }
        else {
            boolean traditional = isShowTraditional();
            applyFont(traditional);
            (traditional ? lyricsTraditional : lyricsSimplify).setVisibility(View.VISIBLE);
            showMeterKey(traditional ? mMeterKeyTraditional : mMeterKeySimplified);
            return;
        }
        showMeterKey(null);
    }

    private void showMeterKey(@Nullable String meterKey) {
        meterKeyView.setText(meterKey);
        meterKeyView.setVisibility(TextUtils.isEmpty(meterKey) ? View.GONE : View.VISIBLE);
        updateInfoRow();
    }

    private boolean isFavoriteMarked() {
        return mContentHandler.isFavoriteMarked(mPageHymnType, mPageHymnNo);
    }

    /** The row shows the key and time signature and/or the favourite star; it is hidden when it has neither. */
    private void updateInfoRow() {
        boolean marked = isFavoriteMarked();
        favoriteStarView.setVisibility(marked ? View.VISIBLE : View.GONE);
        boolean hasKey = meterKeyView.getVisibility() == View.VISIBLE;
        infoRowView.setVisibility(hasKey || marked ? View.VISIBLE : View.GONE);
    }

    /** Called by ContentHandler when the favourite state of the hymn on screen changed. */
    public void onFavoriteStateChanged() {
        if (infoRowView != null) {
            updateInfoRow();
        }
    }

    private boolean isShowTraditional() {
        Boolean override = mContentHandler.lyricsViewOverride;
        if (override != null) {
            return override;
        }
        LyricsLang pref = LyricsLang.fromPref(mSharedPref.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null));
        return LyricsLanguagePolicy.resolveShowTraditional(pref, uiLocale());
    }

    private Locale uiLocale() {
        return mContentHandler.getResources().getConfiguration().getLocales().get(0);
    }

    /** Fades the top bar and the three buttons in or out; the lyrics padding follows at once (plan 6c). */
    @Override
    public void setChromeVisible(boolean visible, boolean animate) {
        mChromeVisible = visible;
        if (topBar == null || buttonBar == null) {
            return;
        }
        fade(topBar, visible, animate);
        fade(buttonBar, visible, animate);
        applyLyricsInsets();
    }

    private void fade(View bar, boolean visible, boolean animate) {
        bar.animate().cancel();
        long duration = animate && animationsEnabled() ? CHROME_FADE_MS : 0;
        if (visible) {
            bar.setVisibility(View.VISIBLE);
            if (duration == 0) {
                bar.setAlpha(1f);
            }
            else {
                bar.animate().alpha(1f).setDuration(duration);
            }
        }
        else if (duration == 0) {
            bar.setAlpha(0f);
            bar.setVisibility(View.GONE);
        }
        else {
            bar.animate().alpha(0f).setDuration(duration).withEndAction(() -> {
                if (!mChromeVisible) {
                    bar.setVisibility(View.GONE);
                }
            });
        }
    }

    /** System "remove animations" (animator duration scale 0) shows and hides without a fade. */
    private boolean animationsEnabled() {
        return Settings.Global.getFloat(requireContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    /** The player layer changed size or the system bottom inset changed: pad the lyrics again. */
    public void onPlayerInsetsChanged() {
        applyLyricsInsets();
    }

    /**
     * Lyrics padding = the overlays shown right now (top bar; three buttons + player layer + system bottom inset + 8dp).
     * The pager fills the screen and the player layer floats over it, so the player's reserve and the system bottom
     * inset are counted here once; the three buttons sit above both.
     */
    private void applyLyricsInsets() {
        if (lyricsScroll == null) {
            return;
        }
        int extra = (int) (LYRICS_BOTTOM_EXTRA_DP * getResources().getDisplayMetrics().density + 0.5f);
        int reserve = mContentHandler == null ? 0 : mContentHandler.getPlayerReserve();
        int systemBottom = mContentHandler == null ? 0 : mContentHandler.getSystemBottomInset();
        ViewGroup.MarginLayoutParams barParams = (ViewGroup.MarginLayoutParams) buttonBar.getLayoutParams();
        if (barParams.bottomMargin != reserve + systemBottom) {
            barParams.bottomMargin = reserve + systemBottom;
            buttonBar.setLayoutParams(barParams);
        }
        LyricsPadding padding = LyricsInsets.padding(topBar.getHeight(), mChromeVisible,
                buttonBar.getHeight(), mChromeVisible, reserve, systemBottom, extra);
        int oldTop = lyricsScroll.getPaddingTop();
        if (oldTop == padding.getTop() && lyricsScroll.getPaddingBottom() == padding.getBottom()) {
            return;
        }
        lyricsScroll.setPadding(lyricsScroll.getPaddingLeft(), padding.getTop(), lyricsScroll.getPaddingRight(), padding.getBottom());
        if (oldTop != padding.getTop()) {
            int target = LyricsInsets.scrollAfterTopPaddingChange(lyricsScroll.getScrollY(), oldTop, padding.getTop());
            lyricsScroll.post(() -> lyricsScroll.scrollTo(0, target));
        }
    }

    @Override
    public void showLyricsEnglish(final String lyrics, boolean preload) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (lyrics != null) {
                mLyricsLoaded = true;
                if (preload) {
                    lyricsEnglish.loadUrl("about:blank");
                }

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // stop canGoBack to display about:blank. not working 100%.
                    lyricsEnglish.clearHistory();
                    // Timber.d("Show Lyrics English: %s", lyrics.length());
                    lyricsEnglish.loadDataWithBaseURL(null, lyrics, "text/html", "utf8", null);
                }, 100);
            }
            else {
                lyricsEnglish.loadUrl(LyricsEnglishRecord.HYMNAL_LINK_MAIN + mHymnNoEng);
            }
        });
    }
}
