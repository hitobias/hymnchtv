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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
import org.cog.hymnchtv.reading.LyricsFont;
import org.cog.hymnchtv.reading.LyricsScale;
import org.cog.hymnchtv.reading.LyricsTypefaces;
import org.cog.hymnchtv.reading.ReadingPrefKeys;
import org.cog.hymnchtv.reading.ReadingPrefs;
import org.cog.hymnchtv.reading.ScorePages;
import org.cog.hymnchtv.reading.ScoreTintPolicy;
import org.cog.hymnchtv.reading.background.BackgroundDrawables;
import org.cog.hymnchtv.reading.background.ReadingPalette;
import org.cog.hymnchtv.utils.HymnIdx2NoConvert;
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
        View.OnLongClickListener, LyricsEnglishRecord.EnglishLyricsListener {
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
    private View lyricsView;
    private View scoreContainer;
    private ZoomTextView lyricsSimplify;
    private ZoomTextView lyricsTraditional;
    private WebView lyricsEnglish;

    private Integer mHymnNoEng = null;
    private boolean isErGe;
    private boolean mLyricsLoaded = false;
    private boolean hasEnglishLyrics = false;
    private boolean mScoreLoaded = false;
    private boolean mHasLyricsText = false;

    /** Score colour level from the context menu; 0 = automatic (follows the background, see ScoreTintPolicy). */
    private static int mScoreColor = 0;

    private static float lyricsScaleP;
    private static float lyricsScaleL;
    private static float lyricsScaleEP;
    private static float lyricsScaleEL;

    private ReadingPalette mPalette;
    private String mResPrefix;
    private int[] mHymnScoreInfo;

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
        btn_ts.setOnLongClickListener(this);

        btn_english = mConvertView.findViewById(R.id.button_english);
        btn_english.setOnClickListener(this);
        btn_english.setOnLongClickListener(this);

        btn_mode = mConvertView.findViewById(R.id.button_mode);
        btn_mode.setOnClickListener(this);
        btn_mode.setOnLongClickListener(this);

        lyricsView = mConvertView.findViewById(R.id.lyricsView);
        lyricsSimplify = mConvertView.findViewById(R.id.lyrics_simplified);
        lyricsSimplify.registerZoomTextListener(this);

        lyricsTraditional = mConvertView.findViewById(R.id.lyrics_traditional);
        lyricsTraditional.registerZoomTextListener(this);

        lyricsEnglish = mConvertView.findViewById(R.id.lyrics_english);

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
    public void onResume() {
        super.onResume();
        registerForContextMenu(lyricsView);
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

    @Override
    public void onPause() {
        unregisterForContextMenu(lyricsView);
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateContextMenu(@NotNull ContextMenu menu, @NotNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mContentHandler.getMenuInflater().inflate(R.menu.menu_content, menu);

        // Hide "英文歌词" if no associated English lyrics
        menu.findItem(R.id.lyrcsEnglish).setVisible(mHymnNoEng != null);
        menu.findItem(R.id.lyrcsEnglishDelete).setVisible(mHymnNoEng != null && hasEnglishLyrics);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ts) {
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

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.button_ts || id == R.id.button_mode) {
            mContentHandler.openReadingSettings();
            return true;
        }
        else if (id == R.id.button_english) {
            if (View.VISIBLE == lyricsEnglish.getVisibility()) {
                mContentHandler.initWebView(ContentHandler.UrlType.englishLyrics);
            }
            else {
                HymnsApp.showToastMessage("Reinit English lyrics");
                reinitEnglishLyrics();
            }
            return true;
        }
        return false;
    }

    private void reinitEnglishLyrics() {
        MainActivity.showContent(mContentHandler, mContentHandler.mHymnType, mContentHandler.getHymnNo(), false, mHymnNoEng);
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
            lyricsSimplify.setText(lyrics);
            lyricsTraditional.setText(loadTraditional(resFName, lyrics));
        }
        mHasLyricsText = DisplayModePolicy.hasLyricsText(lyrics);

        // Auto launch or hint user to view lyrics text via online JiaoChang if available; er,length > 47.
        // Not in "score only": the reader asked not to see lyrics.
        if (!mHasLyricsText && currentDisplayMode() != DisplayMode.SCORE_ONLY) {
            mContentHandler.selectJC();
        }
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
        styleLyrics(lyricsSimplify, kai ? LyricsTypefaces.get(mContentHandler, false) : null);
        styleLyrics(lyricsTraditional, kai ? LyricsTypefaces.get(mContentHandler, true) : null);
        // Photo backgrounds: English lyrics also sit on the contrast-tested panel (PhotoPaletteTest).
        // The WebView's own colour stays transparent; the panel is the View background (set last, so it wins).
        lyricsEnglish.setBackgroundColor(Color.TRANSPARENT);
        lyricsEnglish.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
    }

    private void styleLyrics(ZoomTextView view, @Nullable Typeface typeface) {
        int accent = mPalette.getAccentColor();
        view.setTextColor(mPalette.getTextColor());
        view.setLinkTextColor(accent);
        view.setHighlightColor((accent & 0x00FFFFFF) | 0x40000000);
        view.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        // null for drawn backgrounds; an 85 % panel for photos
        view.setBackground(BackgroundDrawables.backdrop(mContentHandler, mPalette));
    }

    /**
     * Update the lyrics text view default size and the stored scale factor
     * Also being used onConfiguration change
     */
    public void setLyricsTextScale() {
        if (lyricsEnglish == null || lyricsSimplify == null || lyricsTraditional == null) {
            Timber.e(new Exception("Lyrics content view is null"));
            return;
        }

        // English lyrics text size in webSettings
        final WebSettings webSettings = lyricsEnglish.getSettings();

        if (HymnsApp.isPortrait) {
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

        if (HymnsApp.isPortrait) {
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
        if (HymnsApp.isPortrait) {
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
        return DisplayModePolicy.resolve(mContentHandler.displayModeOverride, ReadingPrefs.displayMode(mSharedPref));
    }

    /**
     * Show/hide score, buttons and lyrics for the display mode (plan A2).
     *
     * @param refreshLyrics false keeps the current lyrics views as they are (avoids re-fetching English lyrics)
     */
    private void applyDisplayMode(boolean refreshLyrics) {
        DisplayMode chosen = currentDisplayMode();
        DisplayMode shown = DisplayModePolicy.effective(chosen, mHasLyricsText);
        btn_mode.setText(displayModeLabel(chosen));

        if (shown.getShowScore() && !mScoreLoaded && mResPrefix != null) {
            showLyricsScore(mResPrefix, mHymnScoreInfo);
        }
        else if (!shown.getShowScore() && mScoreLoaded) {
            releaseScores();
        }
        scoreContainer.setVisibility(shown.getShowScore() ? View.VISIBLE : View.GONE);

        btn_ts.setVisibility(shown.getShowLyrics() ? View.VISIBLE : View.GONE);
        btn_english.setVisibility(shown.getShowLyrics() && mHymnNoEng != null ? View.VISIBLE : View.GONE);
        if (!shown.getShowLyrics()) {
            lyricsSimplify.setVisibility(View.GONE);
            lyricsTraditional.setVisibility(View.GONE);
            lyricsEnglish.setVisibility(View.GONE);
        }
        else if (refreshLyrics) {
            toggleLyricsView();
        }
    }

    /** "Lyrics only": let Glide recycle the score bitmaps; switching back reloads them (Codex P2). */
    private void releaseScores() {
        for (int id : SCORE_VIEW_IDS) {
            ImageView view = mConvertView.findViewById(id);
            Glide.with(this).clear(view);
        }
        mScoreLoaded = false;
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
            if (!isShowTraditional()) {
                lyricsSimplify.setVisibility(View.VISIBLE);
            }
            else {
                lyricsTraditional.setVisibility(View.VISIBLE);
            }
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
