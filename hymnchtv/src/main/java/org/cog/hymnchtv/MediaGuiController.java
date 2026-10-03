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

import static org.cog.hymnchtv.MainActivity.HYMN_DB;
import static org.cog.hymnchtv.MainActivity.PREF_MEDIA_HYMN;
import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;
import static org.cog.hymnchtv.mediaplayer.AudioBgService.PlaybackState;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.IntentCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.cog.hymnchtv.reading.background.GlassMode;
import org.cog.hymnchtv.reading.background.UiTokens;
import org.cog.hymnchtv.ui.lyrics.PlayerCardStyle;
import org.cog.hymnchtv.ui.player.PlaybackUiListener;
import org.cog.hymnchtv.ui.player.PlaybackUiState;
import org.cog.hymnchtv.ui.player.GlassFrameLayout;

import org.cog.hymnchtv.mediaplayer.AudioBgService;
import org.cog.hymnchtv.utils.DialogActivity;
import org.cog.hymnchtv.utils.ViewUtil;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * Class implements the media player UI. It provides the full media playback control
 * e.g. play, pause, stop and select play position etc.
 * The UI also includes user selectable media options for the playback i.e. Midi, 教唱, 伴奏 and MP3
 * <p>
 * The UI hymn info and playback are synchronous with the user selected Hymn number.
 * The hymn playing continues, even as user slide to select new hymn; but get updated when the hymn ends or
 * stop by the user.
 * <p>
 * 播放按钮点一下，开始播放媒体档。
 * 再次点播放按钮，播放就会暂停。
 * 再次点播放按钮，从暂停位置继续播放。
 * 长按播放按钮后，可再次从头开始播放。
 *
 * @author Eng Chong Meng
 */
public class MediaGuiController extends Fragment implements AdapterView.OnItemSelectedListener,
        SeekBar.OnSeekBarChangeListener, RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    /**
     * The state of a player where playback is stopped
     */
    private static final int STATE_STOP = 0;
    /**
     * The state of a player when it's created
     */
    private static final int STATE_IDLE = 1;
    /**
     * The state of a player where playback is paused
     */
    private static final int STATE_PAUSE = 2;
    /**
     * The state of a player that is actively playing
     */
    private static final int STATE_PLAY = 3;

    public static final String PREF_PLAYBACK_LOOP = "PlayBack_Loop";
    public static final String PREF_PLAYBACK_LOOP_COUNT = "PlayBack_LoopCount";
    public static final String PREF_PLAYBACK_SPEED = "PlayBack_Speed";

    private static final String PLAYER_STATE = "playerState";
    private static final String PLAYER_INFO = "playerInfo";
    private static final String PLAYER_URIS = "playerUris";

    private int playerState;
    ArrayList<String> playerUris = new ArrayList<>();
    /** The reader moved to another hymn: the next stop broadcast is ours, not a finished track (no auto-next, no toast). */
    private boolean mStoppedByHymnChange = false;

    private static final Map<Uri, BroadcastReceiver> bcRegisters = new HashMap<>();

    public ImageView playbackPlay;

    private View playerUi;
    private TextView hymnInfo = null;
    private TextView playbackPosition;
    private TextView playbackDuration;
    private SeekBar playbackSeekBar;
    private Spinner playbackSpeed;
    private CheckBox cbPlaybackLoop;
    private CheckBox cbAutoStream;
    private EditText edLoopCount;

    private RadioGroup mHymnTypesGroup;
    private UiTokens mTokens;
    private boolean[] mSourceAvailable = {true, true, true, true};
    private RadioButton mBtnMedia;
    private RadioButton mBtnJiaoChang;
    private RadioButton mBtnChangShi;
    private RadioButton mBtnBanZhou;

    private final boolean isMediaAudio = true;
    private boolean isJiaoChangAvailable = false;
    private boolean isSeeking = false;
    private int positionSeek;
    /** Last position and duration reported by the audio service, for the capsule's progress ring. */
    private int mLastPositionMs;
    private int mLastDurationMs;

    private MediaType mMediaType;
    private SharedPreferences mSharedPref;
    private static SharedPreferences.Editor mEditor;

    private MpBroadcastReceiver mReceiver = null;

    private static final String[] mpSpeedValues = HymnsApp.getAppResources().getStringArray(R.array.mp_speed_value);

    protected Uri mUri;
    List<Uri> mediaHymns = new ArrayList<>();
    /** True while the playback list is being looked up off the main thread: further play taps are ignored. */
    private boolean mFetchingHymns = false;
    /** The media type of every playback lookup started so far, for tests. */
    final List<MediaType> mFetchedTypes = new java.util.concurrent.CopyOnWriteArrayList<>();

    private ContentHandler mContentHandler;

    // Need this to prevent crash on rotation if there are other constructors implementation
    // public MediaGuiController() { }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContentHandler = (ContentHandler) context;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View convertView = inflater.inflate(R.layout.media_player_audio_ui, container, false);
        playerUi = convertView.findViewById(R.id.playerUi);

        hymnInfo = convertView.findViewById(R.id.hymn_info);
        hymnInfo.setMovementMethod(new ScrollingMovementMethod());
        hymnInfo.setHorizontallyScrolling(true);

        playbackPosition = convertView.findViewById(R.id.playback_position);
        playbackDuration = convertView.findViewById(R.id.playback_duration);
        playbackSeekBar = convertView.findViewById(R.id.playback_seekbar);

        cbPlaybackLoop = convertView.findViewById(R.id.playback_repeat);
        cbPlaybackLoop.setOnClickListener(v -> onLoopClick());

        cbAutoStream = convertView.findViewById(R.id.playback_auto_stream);
        cbAutoStream.setChecked(mContentHandler.isAutoStream());
        cbAutoStream.setOnClickListener(v -> onAutoStreamClick());

        edLoopCount = convertView.findViewById(R.id.repeatCount);
        edLoopCount.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                onLoopValueChange();
                return true;
            }
            return false;
        });

        playbackSpeed = convertView.findViewById(R.id.playback_speed);
        playbackSpeed.setOnItemSelectedListener(this);

        if (savedInstanceState != null) {
            playerState = savedInstanceState.getInt(PLAYER_STATE);
            String playerInfo = savedInstanceState.getString(PLAYER_INFO);
            hymnInfo.setText(playerInfo);

            playerUris = savedInstanceState.getStringArrayList(PLAYER_URIS);
            if (playerUris != null) {
                for (String uriString : playerUris) {
                    mediaHymns.add(Uri.parse(uriString));
                }
            }
        }
        else {
            playerState = STATE_STOP;
        }

        // Note-5: seek progressBar is not visible and thumb partially clipped with xml default settings.
        // So increase the seekBar height to 16dp i.e. progressBar = 6dp
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && (getContext() != null)) {
            final float scale = getContext().getResources().getDisplayMetrics().density;
            int dp_padding = (int) (16 * scale + 0.5f);

            playbackSeekBar.requestLayout();
            playbackSeekBar.getLayoutParams().height = dp_padding;
        }
        // set to viewHolder default state
        playbackSeekBar.setOnSeekBarChangeListener(this);

        playbackPlay = convertView.findViewById(R.id.playback_play);
        playbackPlay.setOnClickListener(this);

        View mBtnHymnSearch = convertView.findViewById(R.id.btn_hymnSearch);
        // mBtnHymnSearch.setOnTouchListener(touchListener);
        mBtnHymnSearch.setOnClickListener(this);

        mHymnTypesGroup = convertView.findViewById(R.id.hymnsGroup);
        mBtnMedia = convertView.findViewById(R.id.btn_media);

        mBtnJiaoChang = convertView.findViewById(R.id.btn_jiaochang);
        mBtnJiaoChang.setOnClickListener(this);

        mBtnChangShi = convertView.findViewById(R.id.btn_changshi);

        mBtnBanZhou = convertView.findViewById(R.id.btn_banzhou);
        applyTokens(mContentHandler.getPlayerTokens(), mContentHandler.getGlassMode());
        mContentHandler.getPlayerSheet().attachCard((GlassFrameLayout) playerUi);
        publishPlaybackUi();
        return convertView;
    }

    @Override
    public void onDestroyView() {
        if (mContentHandler != null && mContentHandler.getPlayerSheet() != null && playerUi != null) {
            mContentHandler.getPlayerSheet().detachCard(playerUi);
        }
        super.onDestroyView();
    }

    /** The capsule's view of the player: sent whenever the state, the position or the title changes. */
    private void publishPlaybackUi() {
        PlaybackUiListener listener = mContentHandler == null ? null : mContentHandler.getPlaybackUiListener();
        if (listener == null || hymnInfo == null) {
            return;
        }
        boolean active = playerState == STATE_PLAY || playerState == STATE_PAUSE;
        listener.onPlaybackUiState(new PlaybackUiState(playerState == STATE_PLAY, mLastPositionMs, mLastDurationMs,
                hymnInfo.getText().toString(), active));
    }

    @Override
    @SuppressLint("CommitPrefEdits")
    public void onResume() {
        super.onResume();

        // init and prepare the mediaPlayer state receiver
        if (mReceiver == null) {
            mReceiver = new MpBroadcastReceiver();
            registerMpBroadCastReceiver();
        }

        // Request ContentHandler to update the media gui controller hymn display info and media button text color
        mContentHandler.updateMediaPlayerInfo();

        // Get the user selected mediaType for playback
        mSharedPref = mContentHandler.getSharedPreferences(PREF_SETTINGS, 0);
        mEditor = mSharedPref.edit();
        if (null == mEditor) {
            Timber.d("SharePref (Editor): %s (null)", mSharedPref);
        }

        int mediaType = mSharedPref.getInt(PREF_MEDIA_HYMN, MediaType.HYMN_BANZOU.getValue());
        mMediaType = MediaType.valueOf(mediaType);

        // enable OnCheckedChangeListener only after checkRadioButton()
        checkRadioButton(mMediaType);
        mHymnTypesGroup.setOnCheckedChangeListener(this);

        // Init user selected playback speed
        initPlaybackSpeed();

        // Init user selected playback loop parameters (order important)
        boolean isLoop = mSharedPref.getBoolean(PREF_PLAYBACK_LOOP, false);
        cbPlaybackLoop.setChecked(isLoop);
        edLoopCount.setVisibility(isLoop ? View.VISIBLE : View.GONE);

        String loopValue = mSharedPref.getString(PREF_PLAYBACK_LOOP_COUNT, "1");
        edLoopCount.setText(loopValue);
        setPlaybackLoopCount(loopValue);

        // Need this to resume last play state when user changes hymnNo while playing
        for (Uri uri : mediaHymns) {
            mUri = uri;
        }

        // start auto player once only
        if (mContentHandler.isAutoPlay(true)) {
            startPlay();
        }
    }

    /**
     * Need to save all the player state to restore when user return after screen rotation
     * i.e. playerState, playerInfo and playUris
     *
     * @param savedInstanceState for player states
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(PLAYER_STATE, playerState);
        savedInstanceState.putString(PLAYER_INFO, hymnInfo.getText().toString());

        // Need this to resume last play state when user changes hymnNo while playing
        for (Uri uri : mediaHymns) {
            playerUris.add(uri.toString());
        }
        savedInstanceState.putStringArrayList(PLAYER_URIS, playerUris);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Initialize the Media Player UI
     *
     * @param isShow show player UI if true
     */
    public void initPlayerUi(boolean isShow) {
        if (playerUi == null) {
            return; // no view yet: attachCard() applies the visibility when it is created
        }
        playerUi.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public boolean isShown() {
        return playerUi != null && (playerUi.getVisibility() == View.VISIBLE);
    }

    public boolean isPlaying() {
        return (playerState == STATE_PLAY);
    }

    /**
     * Initialize the Media Player playback speed to the user defined setting
     */
    public void initPlaybackSpeed() {
        String speed = mSharedPref.getString(PREF_PLAYBACK_SPEED, "1.0");
        for (int i = 0; i < mpSpeedValues.length; i++) {
            if (mpSpeedValues[i].equals(speed)) {
                playbackSpeed.setSelection(i);
                break;
            }
        }
        setPlaybackSpeed(speed);
    }

    /**
     * Update the player title info with the given text string and
     * Set the Radio Button_Media Text color to Black if user defined media is available, else gray
     *
     * @param info player info
     * @param isAvailable true if use defined media is available
     */
    public void initHymnInfo(String info, boolean[] isAvailable) {
        isJiaoChangAvailable = isAvailable[1];
        if (STATE_STOP == playerState) {
            hymnInfo.setText(info);
            publishPlaybackUi();
            mediaHymns.clear();
            // Available: the theme's text colour (readable in day and night mode); else gray
            mSourceAvailable = isAvailable.clone();
            if (mTokens != null) {
                PlayerCardStyle.styleSources(playerUi, mTokens, mSourceAvailable);
            }
        }
    }

    /** Colours of the player card from the glass tokens of the lyrics background (ContentHandler.applyPlayerGlass). */
    public void applyTokens(UiTokens tokens, GlassMode mode) {
        mTokens = tokens;
        if (playerUi != null) {
            PlayerCardStyle.apply((GlassFrameLayout) playerUi, tokens, mSourceAvailable, mode);
        }
    }

    public boolean selectJC() {
        if (isJiaoChangAvailable) {
            mBtnJiaoChang.performClick();
            return true;
        }
        return false;
    }

    /**
     * This is activated by user; or automatic from mediaController when the downloaded uri is completed
     */
    public void startPlay() {
        startPlay(true);
    }

    /** @param retryOnTypeChange allow one new lookup when the media type changes while this one is in flight */
    private void startPlay(boolean retryOnTypeChange) {
        // proceed to fetch the playback uri list if it is empty; the DB lookup runs off the main thread
        if (mediaHymns.isEmpty()) {
            if (mFetchingHymns) {
                return;
            }
            mFetchingHymns = true;
            final MediaType mediaType = mMediaType;
            mFetchedTypes.add(mediaType);
            mContentHandler.fetchPlayHymn(mediaType, true, uriList -> {
                mFetchingHymns = false;
                // dropped when the user changed the hymn meanwhile (uriList null)
                if (uriList == null) {
                    return;
                }
                if (mediaType == mMediaType) {
                    mediaHymns = uriList;
                    playFetchedHymns();
                }
                else if (retryOnTypeChange) {
                    // the media type changed while the lookup ran: play again once for the type now selected
                    startPlay(false);
                }
            });
            return;
        }
        playFetchedHymns();
    }

    private void playFetchedHymns() {
        for (Uri uri : mediaHymns) {
            String url = uri.toString();
            if (url.contains(".notion.site") || (url.contains("mp.weixin.qq.com") && !HYMN_DB.equals(mContentHandler.mHymnType))) {
                mContentHandler.initWebView(ContentHandler.UrlType.hymnNotionSearch, url);
            }
            else if (url.contains("mp.weixin.qq.com")) {
                mContentHandler.initWebView(ContentHandler.UrlType.hymnNotionSearch);
            }
            else if (url.contains(ContentHandler.btAddr)) {
                mContentHandler.initWebView(ContentHandler.UrlType.hymnBibleTool, url);
            }
            else {
                mUri = uri;
                playStart();
            }
        }
        edLoopCount.clearFocus();
    }

    public void stopPlay() {
        for (Uri uri : mediaHymns) {
            mUri = uri;
            playerStop();
        }

        /*
         * Do not clear the hymns list on stopPlay, allowing playback to fetch new if user changes the hymn
         * Must let MpBroadcastReceiver perform the required task, else has problem in Stop state update
         */
        // mediaHymns.clear();
        // mContentHandler.initMediaPlayerInfo();
    }

    /**
     * The reader moved to another hymn (swipe, "next", any route): whatever is playing stops and the bar resets.
     * Auto-next advancing by itself does not come through here.
     *
     * @return true if something was playing or paused and has been stopped
     */
    public boolean stopForHymnChange() {
        if (playerState != STATE_PLAY && playerState != STATE_PAUSE) {
            return false;
        }
        mStoppedByHymnChange = true;
        stopPlay();
        showPlayIcon(true);
        resetPlaybackBar();
        return true;
    }

    private void resetPlaybackBar() {
        playbackPosition.setText(formatTime(0));
        playbackDuration.setText(formatTime(0));
        playbackSeekBar.setProgress(0);
        mLastPositionMs = 0;
        mLastDurationMs = 0;
        publishPlaybackUi();
    }

    /** Play arrow when a tap starts or resumes playback, pause bars while playing. */
    public void showPlayIcon(boolean playArrow) {
        playbackPlay.setImageResource(playArrow ? R.drawable.ic_player_play_arrow : R.drawable.ic_player_pause);
        playbackPlay.setContentDescription(getString(playArrow ? R.string.c_play : R.string.c_pause));
        publishPlaybackUi();
    }

    /**
     * Starts playing a local file as if the reader had pressed play on it; for instrumentation tests only.
     */
    @androidx.annotation.VisibleForTesting
    public void playUriForTest(Uri uri) {
        mediaHymns = new ArrayList<>(List.of(uri));
        mUri = uri;
        playStart();
    }

    @androidx.annotation.VisibleForTesting
    public int playerStateForTest() {
        return playerState;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton rb = group.findViewById(checkedId);
        if (null != rb) {
            // Must clear mediaHymns on Radio button change (only if no playing); else last fetched media will be used for playback
            if (!isPlaying())
                mediaHymns.clear();

            if (checkedId == R.id.btn_media) {
                mMediaType = MediaType.HYMN_MEDIA;
            }
            else if (checkedId == R.id.btn_jiaochang) {
                mMediaType = MediaType.HYMN_JIAOCHANG;
            }
            else if (checkedId == R.id.btn_changshi) {
                mMediaType = MediaType.HYMN_CHANGSHI;
            }
            else if (checkedId == R.id.btn_banzhou) {
                mMediaType = MediaType.HYMN_BANZOU;
            }

            if (mEditor != null) {
                mEditor.putInt(PREF_MEDIA_HYMN, mMediaType.getValue());
                mEditor.apply();
            }
        }
    }

    private void checkRadioButton(MediaType mediaType) {
        switch (mediaType) {
        case HYMN_MEDIA:
            mBtnMedia.setChecked(true);
            break;

        case HYMN_JIAOCHANG:
            mBtnJiaoChang.setChecked(true);
            break;

        case HYMN_CHANGSHI:
            mBtnChangShi.setChecked(true);
            break;

        case HYMN_BANZOU:
            mBtnBanZhou.setChecked(true);
            break;
        }
    }

    public MediaType getMediaType() {
        return mMediaType;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.playback_play) {
            startPlay();
        }
        else if (id == R.id.btn_hymnSearch) {
            mContentHandler.initWebView(ContentHandler.UrlType.hymnYoutubeSearch);
        }
        else if (id == R.id.btn_jiaochang) {
            if (!isJiaoChangAvailable) {
                // mContentHandler.initWebView(ContentHandler.UrlType.hymnNotionSearch);
                mContentHandler.showNotionSite();
            }
        }
    }

    /** The visible auto-play check box (confirm, then start); replaces the old long press on the play button. */
    private void onAutoStreamClick() {
        if (!cbAutoStream.isChecked()) {
            mContentHandler.setAutoStream(false);
        }
        else if (mMediaType == MediaType.HYMN_JIAOCHANG) {
            cbAutoStream.setChecked(false);
            HymnsApp.showToastMessage(R.string.auto_stream_unsupported,
                    mContentHandler.hymnType2Text(requireContext()), MediaType.mediaType2Text(requireContext(), mMediaType));
        }
        else if (playerState == STATE_STOP) {
            // checked again by ContentHandler.setAutoStream once the user confirms
            cbAutoStream.setChecked(false);
            confirmAutoStream();
        }
        else {
            mContentHandler.setAutoStream(true);
        }
    }

    /** Reflects ContentHandler's auto-play state in the check box (no click event). */
    public void setAutoStreamChecked(boolean on) {
        if (cbAutoStream != null) {
            cbAutoStream.setChecked(on);
        }
    }

    /**
     * Request confirmation from user to start auto streaming.
     */
    private void confirmAutoStream() {
        DialogActivity.showConfirmDialog(mContentHandler,
                R.string.auto_stream,
                R.string.auto_stream_start,
                R.string.auto_stream, new DialogActivity.DialogListener() {
                    /**
                     * Fired when user clicks the dialog's confirm button.
                     *
                     * @param dialog source <tt>DialogActivity</tt>.
                     */
                    public boolean onConfirmClicked(DialogActivity dialog) {
                        mContentHandler.setAutoStream(true);
                        startPlay();
                        return true;
                    }

                    /**
                     * Fired when user dismisses the dialog.
                     *
                     * @param dialog source <tt>DialogActivity</tt>
                     */
                    public void onDialogCancelled(DialogActivity dialog) {
                        mContentHandler.setAutoStream(false);
                    }
                }, mContentHandler.hymnType2Text(requireContext()), MediaType.mediaType2Text(requireContext(), mMediaType)
        );
    }

    /**
     * Initialize the broadcast receiver for the media player (uri).
     * Keep the active bc receiver instance in bcRegisters list to ensure only one bc is registered
     */
    private void bcReceiverInit() {
        if (playerState == STATE_STOP) {
            BroadcastReceiver bcReceiver;
            if ((bcReceiver = bcRegisters.get(mUri)) != null) {
                LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(bcReceiver);
            }

            registerMpBroadCastReceiver();
            bcRegisters.put(mUri, mReceiver);
        }
    }

    private void registerMpBroadCastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioBgService.PLAYBACK_STATE);
        filter.addAction(AudioBgService.PLAYBACK_STATUS);
        LocalBroadcastManager.getInstance(mContentHandler).registerReceiver(mReceiver, filter);
    }

    /**
     * Get the active media player status or just media info for the view display;
     * update the view holder content via Broadcast receiver
     */
    private boolean playerInit() {
        if (isMediaAudio) {
            if (playerState == STATE_STOP) {
                Intent intent = new Intent(mContentHandler, AudioBgService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(mUri);
                intent.setAction(AudioBgService.ACTION_PLAYER_INIT);
                mContentHandler.startService(intent);
            }
            return true;
        }
        return false;
    }

    /**
     * Stop the current active media player playback
     */
    private void playerStop() {
        if (isMediaAudio) {
            if ((playerState == STATE_PAUSE) || (playerState == STATE_PLAY)) {
                Intent intent = new Intent(mContentHandler, AudioBgService.class);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setData(mUri);
                intent.setAction(AudioBgService.ACTION_PLAYER_STOP);
                mContentHandler.startService(intent);
            }
        }
    }

    /**
     * Toggle audio file playback states:
     * STOP -> PLAY -> PAUSE -> PLAY;
     * long press play button to STOP
     * Proceed to open the file for VIEW if this is not an audio file
     */
    private void playStart() {
        mStoppedByHymnChange = false;
        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        if (isMediaAudio) {
            if (playerState == STATE_PLAY) {
                intent.setData(mUri);
                intent.setAction(AudioBgService.ACTION_PLAYER_PAUSE);
                mContentHandler.startService(intent);
                return;
            }
            bcReceiverInit();

            intent.setAction(AudioBgService.ACTION_PLAYER_START);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(mUri);
            // intent.setTypeAndNormalize("1.0");
            mContentHandler.startService(intent);
            return;
        }

        intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setData(mUri);

        PackageManager manager = mContentHandler.getPackageManager();
        List<ResolveInfo> info = manager.queryIntentActivities(intent, 0);
        if (info.isEmpty()) {
            intent.setData(mUri);
        }
        try {
            mContentHandler.startActivity(intent);
        }
        catch (ActivityNotFoundException e) {
            HymnsApp.showToastMessage(R.string.file_open_no_application);
        }
    }

    /**
     * SeekTo player new start play position
     *
     * @param position seek time position
     */
    private void playerSeek(int position) {
        if (isMediaAudio) {
            bcReceiverInit();

            Intent intent = new Intent(mContentHandler, AudioBgService.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(mUri);
            intent.putExtra(AudioBgService.PLAYBACK_POSITION, position);
            intent.setAction(AudioBgService.ACTION_PLAYER_SEEK);
            mContentHandler.startService(intent);
        }
    }

    private void setPlaybackSpeed(String speed) {
        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        intent.setType(speed);
        intent.setAction(AudioBgService.ACTION_PLAYBACK_SPEED);
        mContentHandler.startService(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String speed = mpSpeedValues[position];
        setPlaybackSpeed(speed);
        if (mTokens != null) {
            PlayerCardStyle.styleSpeedItem(view, mTokens);
        }

        if (mEditor != null) {
            mEditor.putString(PREF_PLAYBACK_SPEED, speed);
            mEditor.apply();
            // Timber.d("Set mediaPlayer playback speed to: %sx", speed);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void onLoopClick() {
        boolean isLoop = cbPlaybackLoop.isChecked();
        edLoopCount.setVisibility(isLoop ? View.VISIBLE : View.GONE);
        if (isLoop)
            edLoopCount.requestFocus();

        String loopValue = ViewUtil.toString(edLoopCount);
        setPlaybackLoopCount(loopValue);

        if (mEditor != null) {
            mEditor.putBoolean(PREF_PLAYBACK_LOOP, isLoop);
            mEditor.apply();
        }
    }

    private void onLoopValueChange() {
        String loopValue = ViewUtil.toString(edLoopCount);
        loopValue = (loopValue == null) ? "1" : loopValue;

        edLoopCount.setText(loopValue);
        setPlaybackLoopCount(loopValue);

        if (mEditor != null) {
            mEditor.putString(PREF_PLAYBACK_LOOP_COUNT, loopValue);
            mEditor.apply();
        }
    }

    private void setPlaybackLoopCount(String loopValue) {
        if (!cbPlaybackLoop.isChecked() || (loopValue == null))
            loopValue = "1";

        Intent intent = new Intent(mContentHandler, AudioBgService.class);
        intent.setType(loopValue);
        intent.setAction(AudioBgService.ACTION_PLAYBACK_LOOP);
        mContentHandler.startService(intent);
    }

    /**
     * The Media player BroadcastReceiver to animate and update player view holder info
     */
    private class MpBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handle(intent);
            publishPlaybackUi();
        }

        private void handle(Intent intent) {
            // proceed only if it is the playback of the current Uri
            Uri uri = IntentCompat.getParcelableExtra(intent, AudioBgService.PLAYBACK_URI, Uri.class);
            // Timber.d("Audio playback state: %s: %s", intent.getAction(), uri.getPath());
            if (uri == null || !mediaHymns.contains(uri))
                return;

            int position = intent.getIntExtra(AudioBgService.PLAYBACK_POSITION, 0);
            int audioDuration = intent.getIntExtra(AudioBgService.PLAYBACK_DURATION, 0);
            mLastPositionMs = position;
            mLastDurationMs = audioDuration;

            if ((playerState == STATE_PLAY) && AudioBgService.PLAYBACK_STATUS.equals(intent.getAction())) {
                if (!isSeeking)
                    playbackPosition.setText(formatTime(position));
                playbackDuration.setText(formatTime(audioDuration - position));
                playbackSeekBar.setMax(audioDuration);
                playbackSeekBar.setProgress(position);

            }
            else if (AudioBgService.PLAYBACK_STATE.equals(intent.getAction())) {
                final PlaybackState playbackState = IntentCompat.getSerializableExtra(intent, AudioBgService.PLAYBACK_STATE, PlaybackState.class);
                Timber.d("Audio playback state: %s (%s/%s): %s", playbackState, position, audioDuration, uri.getPath());

                boolean byHymnChange = false;
                switch (playbackState) {
                case init:
                    playerState = STATE_IDLE;
                    playbackDuration.setText(formatTime(audioDuration));
                    playbackPosition.setText(formatTime(0));
                    playbackSeekBar.setMax(audioDuration);
                    playbackSeekBar.setProgress(0);

                    showPlayIcon(true);
                    break;

                case play:
                    playerState = STATE_PLAY;
                    playbackSeekBar.setMax(audioDuration);
                    playerUi.clearAnimation();

                    showPlayIcon(false);
                    break;

                case stop:
                    playerState = STATE_STOP;
                    /*
                     * actually bcRegisters contains the same receivers i.e. MediaGuiController.this
                     * So can just handle once by first incoming midi uri instance
                     */
                    // bcRegisters.remove(uri);
                    // mediaHymns.remove(uri);
                    // if (mediaHymns.isEmpty())
                    //    LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(mReceiver);

                    bcRegisters.clear();

                    // Clear the hymns list on stopPlay, allowing playback to fetch new if user changes the hymn
                    Timber.d("Clear the mediaHymn Uri List");
                    mediaHymns.clear();

                    LocalBroadcastManager.getInstance(mContentHandler).unregisterReceiver(mReceiver);
                    mContentHandler.updateMediaPlayerInfo();

                    // Auto next media if enabled via longPress playButton; not when the reader left the hymn
                    byHymnChange = mStoppedByHymnChange;
                    mStoppedByHymnChange = false;
                    if (!byHymnChange) {
                        mContentHandler.onEndOrError(getString(R.string.playback_completed));
                    }
                    // flow through to reset player state

                case pause:
                    if (playerState != STATE_STOP) {
                        playerState = STATE_PAUSE;
                    }

                    playbackPosition.setText(formatTime(position));
                    playbackDuration.setText(formatTime(audioDuration - position));
                    playbackSeekBar.setMax(audioDuration);
                    playbackSeekBar.setProgress(position);

                    showPlayIcon(true);
                    if (byHymnChange) {
                        resetPlaybackBar();
                    }
                    break;
                }
            }
        }
    }

    /**
     * OnSeekBarChangeListener callback interface
     * A SeekBar callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && (playbackSeekBar == seekBar)) {
            positionSeek = progress;
            playbackPosition.setText(formatTime(progress));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (playbackSeekBar == seekBar) {
            isSeeking = true;
        }

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (playbackSeekBar == seekBar) {
            for (Uri uri : mediaHymns) {
                mUri = uri;
                playerSeek(positionSeek);
            }
            isSeeking = false;
        }
    }

    /**
     * Format the given time to mm:ss
     *
     * @param time time is ms
     *
     * @return the formatted time string in mm:ss
     */
    private String formatTime(int time) {
        // int ms = (time % 1000) / 10;
        int seconds = time / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }
}
