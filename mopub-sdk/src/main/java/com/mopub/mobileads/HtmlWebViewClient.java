/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

class HtmlWebViewClient extends WebViewClient {
    static final String MOPUB_FINISH_LOAD = "mopub://finishLoad";
    static final String MOPUB_FAIL_LOAD = "mopub://failLoad";

    private final Context mContext;
    private HtmlWebViewListener mHtmlWebViewListener;
    private BaseHtmlWebView mHtmlWebView;
    private final String mClickthroughUrl;
    private final String mRedirectUrl;

    HtmlWebViewClient(HtmlWebViewListener htmlWebViewListener, BaseHtmlWebView htmlWebView, String clickthrough, String redirect) {
        mHtmlWebViewListener = htmlWebViewListener;
        mHtmlWebView = htmlWebView;
        mClickthroughUrl = clickthrough;
        mRedirectUrl = redirect;
        mContext = htmlWebView.getContext();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (handleSpecialMoPubScheme(url) || handlePhoneScheme(url) || handleNativeBrowserScheme(url)) {
            return true;
        }

        url = urlWithClickTrackingRedirect(url);
        Log.d("MoPub", "Ad clicked. Click URL: " + url);

        // this is added because http/s can also be intercepted
        if (!isWebSiteUrl(url) && Utils.canHandleApplicationUrl(mContext, url)) {
            if (launchApplicationUrl(url)) {
                return true;
            }
        }

        showMraidBrowserForUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // If the URL being loaded shares the redirectUrl prefix, open it in the browser.
        if (mRedirectUrl != null && url.startsWith(mRedirectUrl)) {
            url = urlWithClickTrackingRedirect(url);
            view.stopLoading();
            showMraidBrowserForUrl(url);
        }
    }

    private boolean isSpecialMoPubScheme(String url) {
        return url.startsWith("mopub://");
    }

    private boolean handleSpecialMoPubScheme(String url) {
        if (!isSpecialMoPubScheme(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        String host = uri.getHost();

        if ("finishLoad".equals(host)) {
            mHtmlWebViewListener.onLoaded(mHtmlWebView);
        } else if ("close".equals(host)) {
            mHtmlWebViewListener.onCollapsed();
        } else if ("failLoad".equals(host)) {
            mHtmlWebViewListener.onFailed(UNSPECIFIED);
        } else if ("custom".equals(host)) {
            handleCustomIntentFromUri(uri);
        }

        return true;
    }

    private boolean isPhoneScheme(String url) {
        return url.startsWith("tel:") || url.startsWith("voicemail:") ||
                url.startsWith("sms:") || url.startsWith("mailto:") ||
                url.startsWith("geo:") || url.startsWith("google.streetview:");
    }

    private boolean handlePhoneScheme(String url) {
        if (!isPhoneScheme(url)) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Could not handle intent with URI: " + url
                + ". Is this intent supported on your phone?";

        launchIntentForUserClick(mContext, intent, errorMessage);

        return true;
    }

    private boolean isNativeBrowserScheme(String url) {
        return url.startsWith("mopubnativebrowser://");
    }

    private boolean handleNativeBrowserScheme(String url) {
        if (!isNativeBrowserScheme(url)) {
            return false;
        }

        Uri uri = Uri.parse(url);


        String urlToOpenInNativeBrowser;
        try {
            urlToOpenInNativeBrowser = uri.getQueryParameter("url");
        } catch (UnsupportedOperationException e) {
            Log.w("MoPub", "Could not handle url: " + url);
            return false;
        }

        if (!"navigate".equals(uri.getHost()) || urlToOpenInNativeBrowser == null) {
            return false;
        }

        Uri intentUri = Uri.parse(urlToOpenInNativeBrowser);

        Intent intent = new Intent(Intent.ACTION_VIEW, intentUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Could not handle intent with URI: " + url
                + ". Is this intent supported on your phone?";

        launchIntentForUserClick(mContext, intent, errorMessage);

        return true;
    }

    private boolean isWebSiteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private String urlWithClickTrackingRedirect(String url) {
        if (mClickthroughUrl == null) {
            return url;
        } else {
            String encodedUrl = Uri.encode(url);
            return mClickthroughUrl + "&r=" + encodedUrl;
        }
    }

    private boolean launchApplicationUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Unable to open intent.";

        return launchIntentForUserClick(mContext, intent, errorMessage);
    }

    private void showMraidBrowserForUrl(String url) {
        if (url == null || url.equals("")) url = "about:blank";
        Log.d("MoPub", "Final URI to show in browser: " + url);
        Intent intent = new Intent(mContext, MraidBrowser.class);
        intent.putExtra(MraidBrowser.URL_EXTRA, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Could not handle intent action. "
                + ". Perhaps you forgot to declare com.mopub.mobileads.MraidBrowser"
                + " in your Android manifest file.";

        boolean handledByMraidBrowser = launchIntentForUserClick(mContext, intent, errorMessage);

        if (!handledByMraidBrowser) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("about:blank"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntentForUserClick(mContext, intent, null);
        }
    }

    private void handleCustomIntentFromUri(Uri uri) {
        String action;
        String adData;
        try {
            action = uri.getQueryParameter("fnc");
            adData = uri.getQueryParameter("data");
        } catch (UnsupportedOperationException e) {
            Log.w("MoPub", "Could not handle custom intent with uri: " + uri);
            return;
        }

        Intent customIntent = new Intent(action);
        customIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        customIntent.putExtra(HtmlBannerWebView.EXTRA_AD_CLICK_DATA, adData);

        String errorMessage = "Could not handle custom intent: " + action
                + ". Is your intent spelled correctly?";

        launchIntentForUserClick(mContext, customIntent, errorMessage);
    }

    boolean launchIntentForUserClick(Context context, Intent intent, String errorMessage) {
        if (!mHtmlWebView.wasClicked()) {
            return false;
        }

        boolean wasIntentStarted = Utils.executeIntent(context, intent, errorMessage);
        if (wasIntentStarted) {
            mHtmlWebViewListener.onClicked();
            mHtmlWebView.onResetUserClick();
        }

        return wasIntentStarted;
    }
}
