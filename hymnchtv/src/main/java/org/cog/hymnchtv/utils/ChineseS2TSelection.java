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
package org.cog.hymnchtv.utils;

import static org.cog.hymnchtv.MainActivity.PREF_SETTINGS;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.OnBackPressedCallback;

import java.util.Locale;

import org.cog.hymnchtv.BaseActivity;
import org.cog.hymnchtv.ContentView;
import org.cog.hymnchtv.R;
import org.cog.hymnchtv.lyrics.HantVariant;
import org.cog.hymnchtv.lyrics.LyricsLang;
import org.cog.hymnchtv.lyrics.LyricsLanguagePolicy;

/**
 * The class allows user to define the final S2T conversion type.
 *
 * @author Eng Chong Meng
 */
public class ChineseS2TSelection extends BaseActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private SharedPreferences mSharedPref;
    private HantVariant mVariant;
    private LyricsLang mLyricsLang;
    private boolean mHasChanges = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chinese_t2s_selection);
        setTitle(R.string.app_title_main);

        mSharedPref = getSharedPreferences(PREF_SETTINGS, 0);
        Locale uiLocale = getResources().getConfiguration().getLocales().get(0);
        String rawType = mSharedPref.getString(ContentView.PREF_CONVERSION_TYPE, null);
        mVariant = LyricsLanguagePolicy.parseVariant(rawType, uiLocale);
        if (rawType != null && !LyricsLanguagePolicy.isCanonical(rawType)) {
            // Self-heal a corrupted value so other readers never see it again
            mSharedPref.edit().putString(ContentView.PREF_CONVERSION_TYPE, mVariant.getPrefValue()).apply();
        }
        checkVariantButton(mVariant);

        mLyricsLang = LyricsLang.fromPref(mSharedPref.getString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, null));
        checkLyricsLangButton(mLyricsLang);

        // Only enable OnCheckedChangeListener after the initial check states are set
        ((RadioGroup) findViewById(R.id.radioGroupVar)).setOnCheckedChangeListener(this);
        ((RadioGroup) findViewById(R.id.radioGroupLyricsDefault)).setOnCheckedChangeListener(this);

        findViewById(R.id.btnCancel).setOnClickListener(this);
        findViewById(R.id.btnOk).setOnClickListener(this);
        getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnOk) {
            updateS2TSelection(mHasChanges);
        }
        else if (id == R.id.btnCancel) {
            checkUnsavedChanges();
        }
    }

    private void checkVariantButton(HantVariant variant) {
        int id = (variant == HantVariant.HK) ? R.id.radioButtonS2HK : R.id.radioButtonS2TW;
        ((RadioButton) findViewById(id)).setChecked(true);
    }

    private void checkLyricsLangButton(LyricsLang lang) {
        int id;
        switch (lang) {
            case SIMPLIFIED:
                id = R.id.radioLyricsSimplified;
                break;
            case TRADITIONAL:
                id = R.id.radioLyricsTraditional;
                break;
            default:
                id = R.id.radioLyricsFollowUi;
                break;
        }
        ((RadioButton) findViewById(id)).setChecked(true);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.findViewById(checkedId) == null) {
            return;
        }
        mHasChanges = true;
        if (group.getId() == R.id.radioGroupLyricsDefault) {
            if (checkedId == R.id.radioLyricsSimplified) {
                mLyricsLang = LyricsLang.SIMPLIFIED;
            }
            else if (checkedId == R.id.radioLyricsTraditional) {
                mLyricsLang = LyricsLang.TRADITIONAL;
            }
            else {
                mLyricsLang = LyricsLang.FOLLOW_UI;
            }
        }
        else {
            mVariant = (checkedId == R.id.radioButtonS2HK) ? HantVariant.HK : HantVariant.TW;
        }
    }

    /**
     * Save the user defined conversion type setting.
     */
    private void updateS2TSelection(boolean hasChanges) {
        if (hasChanges) {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString(ContentView.PREF_CONVERSION_TYPE, mVariant.getPrefValue());
            editor.putString(LyricsLanguagePolicy.PREF_LYRICS_DEFAULT, mLyricsLang.name());
            editor.apply();
        }

        Intent result = new Intent();
        result.putExtra(ContentView.EXTR_KEY_HAS_CHANGES, hasChanges);
        setResult(Activity.RESULT_OK, result);

        mHasChanges = false;
        finish();
    }

    /**
     * check for any unsaved changes and alert user before the exit.
     */
    private void checkUnsavedChanges() {
        if (mHasChanges) {
            DialogActivity.showConfirmDialog(this,
                    R.string.to_be_added,
                    R.string.unsaved_changes,
                    R.string.add_renew, new DialogActivity.DialogListener() {
                        public boolean onConfirmClicked(DialogActivity dialog) {
                            updateS2TSelection(true);
                            return true;
                        }

                        public void onDialogCancelled(DialogActivity dialog) {
                            updateS2TSelection(false);
                            finish();
                        }
                    });
        }
        else {
            finish();
        }
    }

    /**
     * Trap BackKey to check for unsaved changes.
     */
    OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            checkUnsavedChanges();
        }
    };
}
