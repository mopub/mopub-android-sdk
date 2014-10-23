package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.IntentUtils;
import com.mopub.mobileads.util.Utils;

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

        MoPubLog.d("Ad clicked. Click URL: " + url);

        // this is added because http/s can also be intercepted
        if (!isWebSiteUrl(url) && IntentUtils.canHandleApplicationUrl(mContext, url)) {
            if (launchApplicationUrl(url)) {
                return true;
            }
        }

        showMoPubBrowserForUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // If the URL being loaded shares the redirectUrl prefix, open it in the browser.
        if (mRedirectUrl != null && url.startsWith(mRedirectUrl)) {
            view.stopLoading();
            showMoPubBrowserForUrl(url);
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
            MoPubLog.w("Could not handle url: " + url);
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

    private boolean launchApplicationUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Unable to open intent.";

        return launchIntentForUserClick(mContext, intent, errorMessage);
    }

    private void showMoPubBrowserForUrl(String url) {
        if (url == null || url.equals("")) url = "about:blank";
        MoPubLog.d("Final URI to show in browser: " + url);
        Intent intent = new Intent(mContext, MoPubBrowser.class);
        intent.putExtra(MoPubBrowser.DESTINATION_URL_KEY, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Could not handle intent action. "
                + ". Perhaps you forgot to declare com.mopub.common.MoPubBrowser"
                + " in your Android manifest file.";

        boolean handledByMoPubBrowser = launchIntentForUserClick(mContext, intent, errorMessage);

        if (!handledByMoPubBrowser) {
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
            MoPubLog.w("Could not handle custom intent with uri: " + uri);
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
