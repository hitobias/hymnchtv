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
package org.cog.hymnchtv.webview;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import org.cog.hymnchtv.BaseFragment;
import org.cog.hymnchtv.BuildConfig;
import org.cog.hymnchtv.ContentHandler;
import org.cog.hymnchtv.HymnsApp;
import org.cog.hymnchtv.R;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * The class displays the content accessed via given web link
 * <a href="https://developer.android.com/guide/webapps/webview">...</a>
 *
 * @author Eng Chong Meng
 */
@SuppressLint("SetJavaScriptEnabled")
public class WebViewFragment extends BaseFragment implements OnKeyListener {
    private WebView webView;
    // private ProgressBar progressbar;
    private static final Stack<String> urlStack = new Stack<>();

    private String webUrl = null;
    private ValueCallback<Uri[]> mUploadMessageArray;
    private ContentHandler mContentHandler;

    @SuppressLint("JavascriptInterface")
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentHandler = (ContentHandler) mContext;
        View contentView = inflater.inflate(R.layout.webview_main, container, false);
        // progressbar = contentView.findViewById(R.id.progress);
        // progressbar.setIndeterminate(true);

        webView = contentView.findViewById(R.id.webview);
        // webView.setBackgroundColor(Color.TRANSPARENT);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // https://developer.android.com/guide/webapps/webview#BindingJavaScript
        webView.addJavascriptInterface(HymnsApp.getInstance(), "Android");
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // https://developer.android.com/guide/webapps/webview#HandlingNavigation
        webView.setWebViewClient(new MyWebViewClient(this) {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String targetColor = "transparent"; // "#FF5733";
                String js = "javascript:(function() { document.body.style.backgroundColor = '" + targetColor + "'; })()";
                view.evaluateJavascript(js, null);
            }
        });
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                // Handle the download request here.
                mContentHandler.startFileDownload(url, mimeType);
            }
        });

        // init webUrl with urlStack.pop() if non-empty, else load from default in DB
        if (urlStack.isEmpty()) {
            webUrl = mContentHandler.getWebUrl();
            urlStack.push(webUrl);
        }
        else {
            webUrl = urlStack.pop();
        }
        if (!TextUtils.isEmpty(webUrl))
            webView.loadUrl(webUrl);
        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // setup keyPress listener - must re-enable every time on resume
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();
        webView.setOnKeyListener(this);
    }

    /**
     * Hymnchtv reuses the webView fragment. Keep if they are the same.
     * Init webView to download a new web page if it is not the same as last accessed page
     */
    public void initWebView() {
        String tmp = mContentHandler.getWebUrl();
        if (webUrl == null || !webUrl.equals(tmp)) {
            urlStack.clear();
            webUrl = tmp;
            urlStack.push(webUrl);
            webView.loadUrl(webUrl);
        }
    }

    /**
     * Opens a FileChooserDialog to let the user pick files for upload
     */
    private ActivityResultLauncher<String> getFileUris() {
        return registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) {
                if (mUploadMessageArray == null)
                    return;

                Uri[] uriArray = new Uri[uris.size()];
                uriArray = uris.toArray(uriArray);

                mUploadMessageArray.onReceiveValue(uriArray);
                mUploadMessageArray = null;
            }
            else {
                HymnsApp.showToastMessage(R.string.file_does_not_exist);
            }
        });
    }

    // Prevent the webView from reloading on device rotation
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        }
        catch (IOException e) {
            Timber.w("Exception %s", e.getMessage());
            return null;
        }
    }

    /**
     * Handler for user enter Back Key
     * User Back Key entry will return to previous web access pages until root; before return to caller
     *
     * @param v view
     * @param keyCode the entered key keycode
     * @param event the key Event
     *
     * @return true if process
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
        }
        return false;
    }
}