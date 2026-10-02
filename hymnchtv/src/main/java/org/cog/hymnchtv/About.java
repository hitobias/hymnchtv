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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import org.cog.hymnchtv.logutils.LogUploadServiceImpl;
import org.cog.hymnchtv.service.androidupdate.UpdateServiceImpl;
import org.cog.hymnchtv.utils.ThemeHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.cketti.library.changelog.ChangeLog;
import timber.log.Timber;

/**
 * About info on hymnchtv
 *
 * @author Eng Chong Meng
 */
public class About extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {
    public static String HYMNCHTV_LINK = "https://cmeng-git.github.io/hymnchtv/";

    private static final String[][] USED_LIBRARIES = new String[][] {
            new String[] {"Android Support Library", "https://developer.android.com/topic/libraries/support-library/index.html"},
            new String[] {"android-opencc", "https://github.com/frankslin/android-opencc"},
            new String[] {"android-youtube-player", "https://github.com/PierfrancescoSoffritti/android-youtube-player"},
            new String[] {"annotations-java5", "https://mvnrepository.com/artifact/org.jetbrains/annotations"},
            new String[] {"Apache HttpCore", "https://hc.apache.org/httpcomponents-core-4.4.x/httpcore/dependency-info.html"},
            new String[] {"ckChangeLog", "https://github.com/cketti/ckChangeLog"},
            new String[] {"commons-lang", "https://commons.apache.org/proper/commons-lang/"},
            new String[] {"jsoup", "https://github.com/jhy/jsoup"},
            new String[] {"Media-ExoPlayer", "https://github.com/androidx/media"},
            new String[] {"glide", "https://github.com/bumptech/glide"},
            new String[] {"js-evaluator-for-android", "https://github.com/evgenyneu/js-evaluator-for-android"},
            new String[] {"httpcore", "https://hc.apache.org/httpcomponents-core-ga/"},
            new String[] {"okhttp", "https://github.com/lysine-dev/okhttp"},
            new String[] {"OpenCC", "https://github.com/byvoid/opencc"},
            new String[] {"pinyin", "https://github.com/duguying/pinyin"},
            new String[] {"RichEditor for Android", "https://github.com/wasabeef/richeditor-android"},
            new String[] {"Timber", "https://github.com/JakeWharton/timber"},
            new String[] {"uCrop", "https://github.com/Yalantis/uCrop"}
    };

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
        // crash if enabled under FragmentActivity
        // requestWindowFeature(Window.FEATURE_LEFT_ICON);
        // setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, android.R.drawable.ic_dialog_info);
        setTitle(getString(R.string.about));

        View hymnchtvkUrl = findViewById(R.id.hymnchtv_link);
        hymnchtvkUrl.setOnClickListener(this);

        TextView hymnchtvHelp = findViewById(R.id.hymnchtv_help);
        hymnchtvHelp.setOnClickListener(this);
        findViewById(R.id.font_credit).setOnClickListener(this);

        TextView copyRight = findViewById(R.id.copyRight);
        copyRight.setMovementMethod(LinkMovementMethod.getInstance());
        copyRight.setText(Html.fromHtml(getString(R.string.copyright), Html.FROM_HTML_MODE_LEGACY));

        Button btn_HistoryLog = findViewById(R.id.history_log);
        btn_HistoryLog.setOnClickListener(this);
        btn_HistoryLog.setOnLongClickListener(this);

        findViewById(R.id.submit_logs).setOnClickListener(this);
        findViewById(R.id.ok_button).setOnClickListener(this);

        Button btn_chkNewVersion = findViewById(R.id.check_new_version);
        if (HymnsApp.updateServiceAllowed || BuildConfig.DEBUG) {
            btn_chkNewVersion.setVisibility(View.VISIBLE);
            btn_chkNewVersion.setOnClickListener(this);
        }
        else {
            btn_chkNewVersion.setVisibility(View.GONE);
        }

        // View btn_submitLogs = findViewById(R.id.submit_logs);
        // btn_submitLogs.setOnClickListener(this);

        String aboutInfo = getAboutInfo();
        WebView wv = findViewById(R.id.about_info);
        wv.setBackgroundColor(Color.TRANSPARENT);
        wv.loadDataWithBaseURL("file:///android_res/drawable/", aboutInfo, "text/html", "utf-8", null);

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);

            TextView textView = findViewById(R.id.about_appVersion);
            textView.setText(getString(R.string.version, pi.versionName, pi.versionCode));
        }
        catch (PackageManager.NameNotFoundException e) {
            Timber.e("Package Info; %s", e.getMessage());
        }
    }

    @Override
    public void onClick(View view) {
        String LOG_REPORT_EMAIL = "cmeng.gm@gmail.com";
        int id = view.getId();
        if (id == R.id.ok_button) {
            finish();
        }
        else if (id == R.id.check_new_version) {
            checkUpdate();
        }
        else if (id == R.id.submit_logs) {
            new LogUploadServiceImpl().sendLogs(new String[] {LOG_REPORT_EMAIL},
                    getString(R.string.send_logs_subject),
                    getString(R.string.send_logs_title));
        }
        else if (id == R.id.history_log) {
            ChangeLog cl = new ChangeLog(this, DEFAULT_CSS);
            cl.getFullLogDialog().show();
        }
        else if (id == R.id.hymnchtv_help || id == R.id.hymnchtv_link) {
            hymnUrlAccess(this, HYMNCHTV_LINK);
        }
        else if (id == R.id.font_credit) {
            showFontLicense();
        }
        else {
            finish();
        }
    }

    /**
     * Show the SIL OFL text that must ship with the bundled HymnalKai fonts (plan A2).
     */
    private void showFontLicense() {
        String licence;
        try (InputStream in = getAssets().open("licenses/OFL-HymnalKai.txt")) {
            // not IOUtils.toByteArray: commons-io 2.x touches java.nio.file, which API 24 does not have
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            licence = out.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e) {
            Timber.e(e, "Font licence asset missing");
            HymnsApp.showToastMessage(R.string.font_license_title);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.font_license_title)
                .setMessage(licence)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public boolean onLongClick(View view) {
        if (view.getId() == R.id.history_log) {
            checkUpdate();
            return true;
        }
        return false;
    }

    /**
     * Check app new update availability
     */
    private void checkUpdate() {
        new Thread() {
            @Override
            public void run() {
                UpdateServiceImpl.getInstance().checkForUpdates();
            }
        }.start();
    }

    public static void hymnUrlAccess(Context context, String url) {
        if (url == null)
            url = HYMNCHTV_LINK;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    private String getAboutInfo() {
        StringBuilder html = new StringBuilder()
                .append("<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\"/>")
                .append("<html><head><style type=\"text/css\">")
                .append(DEFAULT_CSS);

        // Change text and url according to app theme
        if (ThemeHelper.isAppTheme(ThemeHelper.Theme.DARK)) {
            html.append(bodyTextLight);
        }
        else {
            html.append(bodyTextDark);
        }

        html.append("</style></head><body>");

        StringBuilder libs = new StringBuilder().append("<ul>");
        for (String[] library : USED_LIBRARIES) {
            libs.append("<li><a href=\"")
                    .append(library[1])
                    .append("\">")
                    .append(library[0])
                    .append("</a></li>");
        }
        libs.append("</ul>");

        html.append(getString(R.string.content_about))
                .append("</p><hr/><p>");

        html.append(getString(R.string.content_help))
                .append("</p><hr/><p>");

        html.append(String.format(getString(R.string.app_libraries), libs))
                .append("</p><hr/><p>");
        html.append("</body></html>");

        return html.toString();
    }
}
