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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

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

    private String webUrl = null;
    private ValueCallback<Uri[]> mUploadMessageArray;
    private ContentHandler mContentHandler;
    private boolean onErrorUrl = false;

    @SuppressLint("JavascriptInterface")
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentHandler = (ContentHandler) mContext;
        final View contentView = inflater.inflate(R.layout.webview_main, container, false);

        webView = contentView.findViewById(R.id.webview);
        // webView.setBackgroundColor(Color.TRANSPARENT);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // String agent = "Mozilla/5.0 (Linux; Android 7.0; Nexus 4 Build/KRT16H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/58.0.3029.125 Mobile Safari/537.36";
        // String defaultUserAgent = webSettings.getUserAgentString();
        // webSettings.setUserAgentString(defaultUserAgent + agent);

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
                // super.onPageFinished(view, url);
                // Script to help bibletool "下载： Mp3， 歌词， pdf，歌谱" text more visible to user.
                String targetColor = "transparent"; // "#FF5733";
                String js = "javascript:(function() { document.body.style.backgroundColor = '" + targetColor + "'; })()";
                view.evaluateJavascript(js, null);
            }

            // Many sites have Uncaught TypeError: Cannot read properties of null (reading 'classList')", source: https://bibletool.online/js/headroom.min.js
            // Main use is to resolve Huawei webView problem. But found to also affect others. So not use.
            // Instead, force load via external browser for Huawei devices.
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Timber.d("Received Error: %s: %s \n%s' \n%s", onErrorUrl, errorResponse.getStatusCode(), request.getUrl(), webUrl);
                if (!onErrorUrl) {
                    // Do not load, just log the event for future debug.
                    // About.hymnUrlAccess(mContext, webUrl);

                    // prevent current webView re-triggers on next error.
                    onErrorUrl = true;
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType,
                    long contentLength) {
                // Handle the download request here.
                mContentHandler.startFileDownload(url, mimeType);
            }
        });
        return contentView;
    }

    /**
     * Hymnchtv reuses the webView fragment. Init webView to download the new web page if it is not the same as webView.getUrl;
     * Note: must clear the View State entirely with a about:blank loading, and delayed loading the new url;
     * else last accessed old page is shown instead. webView.clearHistory() is not working 100%, canGoBack needs to handle.
     */
    public void initWebView(String url) {
        try {
            if (!URLDecoder.decode(webView.getUrl(), "UTF-8").equals(url)) {
                webUrl = url;
                webView.loadUrl("about:blank");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    // stop canGoBack to display about:blank. but not working 100%.
                    webView.clearHistory();
                    webView.loadUrl(webUrl);
                }, 100);
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        webUrl = mContentHandler.getWebUrl();
        // onResume is called when return from an external browser; Do not reload if onErrorUrl
        // if (!onErrorUrl && !TextUtils.isEmpty(webUrl)) {
        if (!TextUtils.isEmpty(webUrl)) {
            webView.loadUrl(webUrl);
        }

        // setup keyPress listener - must re-enable every time on resume
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();
        webView.setOnKeyListener(this);
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
     * @return false for parent to handle KEYCODE_BACK action.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    // Do not show about:blank page; see initWebView(): due to webView.clearHistory() not working 100%.
                    WebBackForwardList historyList = webView.copyBackForwardList();
                    int currentIndex = historyList.getCurrentIndex();
                    if ("about:blank".equals(historyList.getItemAtIndex(currentIndex - 1).getUrl())) {
                        return false;
                    }
                    webView.goBack();
                    return true;
                }
            }
        }
        return false;
    }
}