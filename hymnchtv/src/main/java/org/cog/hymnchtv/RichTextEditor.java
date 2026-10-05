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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.cog.hymnchtv.concurrent.AppExecutors;
import org.cog.hymnchtv.editor.EditorStore;
import org.cog.hymnchtv.utils.DialogActivity;

import timber.log.Timber;

/**
 * Plain-text editor of the media links import/export file (a CSV): view, change and save it.
 * The text never goes into the saved-state Bundle (a large export exceeds the binder limit and crashed API 24+ when the
 * screen was stopped); unsaved changes are kept in a draft file in the cache directory instead, see EditorStore.
 *
 * @author Eng Chong Meng
 */
public class RichTextEditor extends BaseActivity
        implements View.OnClickListener, DialogActivity.DialogListener {
    /** Intent extra and saved-state key: absolute path of the file under edit. */
    public static final String ATTR_FILE_URI = "attr_file_uUri";
    /** Saved-state key: the unsaved text was written to the draft file. */
    private static final String STATE_HAS_DRAFT = "has_draft";

    /* filename under edit */
    private String fileUri = null;

    /* Uncommitted changes to apply on save; per screen (it was static, so one editor's state leaked into the next) */
    private boolean hasChanges = false;

    /* The text is still being loaded from the draft: say so again if this screen is saved before it arrives */
    private boolean draftPending = false;

    /* The file (or draft) text is in the editor; edits before that are not the user's */
    private boolean loaded = false;

    /* A save is running on AppExecutors.io */
    private boolean saving = false;

    private EditText mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rich_text_editor);
        mEditor = findViewById(R.id.editor);
        mEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (loaded) {
                    hasChanges = true;
                }
            }
        });
        findViewById(R.id.saveButton).setOnClickListener(this);
        findViewById(R.id.endButton).setOnClickListener(this);

        if (savedInstanceState != null) {
            fileUri = savedInstanceState.getString(ATTR_FILE_URI);
            draftPending = savedInstanceState.getBoolean(STATE_HAS_DRAFT, false);
        }
        else {
            // A draft left by an editor that was closed without saving belongs to no one now
            Bundle extras = getIntent().getExtras();
            fileUri = (extras == null) ? null : extras.getString(ATTR_FILE_URI);
            if (!TextUtils.isEmpty(fileUri)) {
                EditorStore.clearDraft(getCacheDir(), fileUri);
            }
        }

        if (TextUtils.isEmpty(fileUri)) {
            HymnsApp.showToastMessage(R.string.file_does_not_exist);
            finish();
            return;
        }
        setTitle(new File(fileUri).getName());
        loadText();
    }

    /** Reads the file, or the draft of a recreated screen, on AppExecutors.io. */
    private void loadText() {
        final File source = new File(fileUri);
        final File cacheDir = getCacheDir();
        final boolean preferDraft = draftPending;
        AppExecutors.ioThenMain("editor-load", this, () -> {
            try {
                return EditorStore.load(source, cacheDir, fileUri, preferDraft);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, content -> {
            if (content.getTooLarge()) {
                HymnsApp.showToastMessage(R.string.editor_file_too_large, EditorStore.MAX_EDIT_BYTES / 1024);
                finish();
                return;
            }
            mEditor.setText(content.getText());
            loaded = true;
            draftPending = false;
            hasChanges = content.getFromDraft();
            if (content.getDraftMissing()) {
                HymnsApp.showToastMessage(R.string.editor_draft_lost);
            }
        }, () -> {
            HymnsApp.showToastMessage(R.string.file_does_not_exist);
            finish();
        });
    }

    /**
     * Only the file path and a draft flag go into the Bundle; unsaved text is written to the draft file.
     *
     * @param outState Bundle
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ATTR_FILE_URI, fileUri);
        boolean hasDraft = draftPending;
        if (loaded && hasChanges) {
            try {
                EditorStore.saveDraft(getCacheDir(), fileUri, mEditor.getText().toString());
                hasDraft = true;
            }
            catch (IOException e) {
                Timber.w(e, "Editor draft not saved");
            }
        }
        outState.putBoolean(STATE_HAS_DRAFT, hasDraft);
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && !TextUtils.isEmpty(fileUri)) {
            EditorStore.clearDraft(getCacheDir(), fileUri);
        }
        super.onDestroy();
    }

    @VisibleForTesting
    public boolean hasUnsavedChanges() {
        return hasChanges;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.saveButton) {
            saveOrFinish();
        }
        else if (id == R.id.endButton) {
            checkUnsavedChanges();
        }
    }

    /**
     * check for any unsaved changes and alert user
     */
    private void checkUnsavedChanges() {
        if (hasChanges) {
            DialogActivity.showConfirmDialog(this,
                    R.string.to_be_added,
                    R.string.unsaved_changes,
                    R.string.add, this);
        }
        else {
            finish();
        }
    }

    /**
     * Fired when user clicks the dialog's confirm button.
     *
     * @param dialog source <tt>DialogActivity</tt>.
     */
    public boolean onConfirmClicked(DialogActivity dialog) {
        saveOrFinish();
        return true;
    }

    /**
     * Fired when user dismisses the dialog: the changes are discarded.
     *
     * @param dialog source <tt>DialogActivity</tt>
     */
    public void onDialogCancelled(DialogActivity dialog) {
        EditorStore.clearDraft(getCacheDir(), fileUri);
        finish();
    }

    /** Writes the text back to its file on AppExecutors.io (byte for byte, line breaks kept), then closes. */
    private void saveOrFinish() {
        if (!hasChanges) {
            finish();
            return;
        }
        if (saving) {
            return;
        }
        saving = true;
        final File target = new File(fileUri);
        final String text = mEditor.getText().toString();
        AppExecutors.ioThenMain("editor-save", this, () -> {
            try {
                EditorStore.write(target, text);
                return Boolean.TRUE;
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, saved -> {
            saving = false;
            hasChanges = false;
            HymnsApp.showToastMessage(R.string.file_saved);
            finish();
        }, () -> {
            saving = false;
            HymnsApp.showToastMessage(R.string.editor_save_failed);
        });
    }
}
