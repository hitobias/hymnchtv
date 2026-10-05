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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cog.hymnchtv.about.HelpActivity;
import org.cog.hymnchtv.about.LicensesActivity;
import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

/**
 * About page (sub-project Z): app name, version, short description, licenses and help. Native views only; no
 * links and no personal data. The original project's Apache-2.0 notice is in the generated licenses list.
 *
 * @author Eng Chong Meng
 */
public class About extends BaseActivity implements View.OnClickListener {
    /**
     * Default CSS styles used to format the change log.
     */
    public static final String DEFAULT_CSS =
            "h1 { margin-left: 0px; font-size: 1.2em; }\n" +
                    "li { margin-left: 0px; font-size: 0.9em;}\n" +
                    "ul { padding-left: 2em; }";
    public static final String bodyTextLight =
            " body { color: white }\n" +
                    " a { color: #80CBC4; text-decoration:none }";

    public static final String bodyTextDark =
            " body { color: black }\n" +
                    " a { color: #00897B; text-decoration:none }";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        setTitle(getString(R.string.about));

        // getString drops the <u>/<b> styling some shared strings carry; the chips and buttons are plain text here
        setPlainText(R.id.about_help, R.string.help);
        setPlainText(R.id.about_licenses, R.string.about_licenses_title);
        setPlainText(R.id.history_log, R.string.show_history_log);
        setPlainText(R.id.submit_logs, R.string.send_logs);
        setPlainText(R.id.check_new_version, R.string.app_update_check);
        setPlainText(R.id.ok_button, R.string.ok);

        findViewById(R.id.about_licenses).setOnClickListener(this);
        findViewById(R.id.about_help).setOnClickListener(this);

        View btn_HistoryLog = findViewById(R.id.history_log);
        btn_HistoryLog.setOnClickListener(this);

        findViewById(R.id.submit_logs).setOnClickListener(this);
        findViewById(R.id.ok_button).setOnClickListener(this);

        View btn_chkNewVersion = findViewById(R.id.check_new_version);
        if (HymnsApp.updateServiceAllowed || BuildConfig.DEBUG) {
            btn_chkNewVersion.setVisibility(View.VISIBLE);
            btn_chkNewVersion.setOnClickListener(this);
        }
        else {
            btn_chkNewVersion.setVisibility(View.GONE);
            // Without the update button, OK keeps the same gap above it as the button pair
            View okButton = findViewById(R.id.ok_button);
            ViewGroup.MarginLayoutParams okParams = (ViewGroup.MarginLayoutParams) okButton.getLayoutParams();
            okParams.topMargin = getResources().getDimensionPixelSize(R.dimen.dialog_button_gap_first);
            okButton.setLayoutParams(okParams);
        }

        TextView version = findViewById(R.id.about_appVersion);
        version.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
    }

    private void setPlainText(int viewId, int stringId) {
        ((TextView) findViewById(viewId)).setText(getString(stringId));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ok_button) {
            finish();
        }
        else if (id == R.id.check_new_version) {
            checkUpdate();
        }
        else if (id == R.id.submit_logs) {
            // No fixed recipient: the user picks where the report goes in the share sheet.
            new LogUploadServiceImpl().sendLogs(new String[0],
                    getString(R.string.send_logs_subject), getString(R.string.send_logs_title));
        }
        else if (id == R.id.history_log) {
            new ChangeLog(this, DEFAULT_CSS).getFullLogDialog().show();
        }
        else if (id == R.id.about_licenses) {
            startActivity(new Intent(this, LicensesActivity.class));
        }
        else if (id == R.id.about_help) {
            startActivity(new Intent(this, HelpActivity.class));
        }
        else {
            finish();
        }
    }

    private void checkUpdate() {
        new Thread(() -> UpdateServiceImpl.getInstance().checkForUpdates(), "hymnal-update").start();
    }

    /**
     * Opens a content url (e.g. English lyrics) in a browser. There is no default help url any more.
     */
    public static void hymnUrlAccess(Context context, String url) {
        if (url == null) {
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        catch (ActivityNotFoundException e) {
            Timber.w("No activity for %s", url);
        }
    }
}
