package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.List;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

class HtmlWebViewClient extends WebViewClient {
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

        if (isApplicationUrl(url) && !canHandleApplicationUrl(url)) {
            return true;
        }

        url = urlWithClickTrackingRedirect(url);
        Log.d("MoPub", "Ad clicked. Click URL: " + url);

        if (isApplicationUrl(url)) {
            launchApplicationUrl(url);
            return true;
        }

        showBrowserForUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        // If the URL being loaded shares the redirectUrl prefix, open it in the browser.
        if (mRedirectUrl != null && url.startsWith(mRedirectUrl)) {
            url = urlWithClickTrackingRedirect(url);
            view.stopLoading();
            showBrowserForUrl(url);
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

    private boolean isApplicationUrl(String url) {
        return isMarketUrl(url) || isAmazonUrl(url);
    }

    private boolean isMarketUrl(String url) {
        return url.startsWith("market://");
    }

    private boolean isAmazonUrl(String url) {
        return url.startsWith("amzn://");
    }

    private boolean canHandleApplicationUrl(String url) {
        // Determine which activities can handle the market intent
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);

        // If there are no relevant activities, don't follow the link
        boolean isIntentSafe = activities.size() > 0;
        if (!isIntentSafe) {
            Log.w("MoPub", "Could not handle application specific action: " + url + ". " +
                    "You may be running in the emulator or another device which does not " +
                    "have the required application.");
            return false;
        }

        return true;
    }

    private String urlWithClickTrackingRedirect(String url) {
        if (mClickthroughUrl == null) {
            return url;
        } else {
            String encodedUrl = Uri.encode(url);
            return mClickthroughUrl + "&r=" + encodedUrl;
        }
    }

    private void launchApplicationUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        String errorMessage = "Unable to open external app store.";

        launchIntentForUserClick(mContext, intent, errorMessage);
    }

    private void showBrowserForUrl(String url) {
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
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
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
        if (!mHtmlWebView.hasUserClicked()) {
            return false;
        }

        boolean wasIntentStarted = executeIntent(context, intent, errorMessage);
        if (wasIntentStarted) {
            mHtmlWebViewListener.onClicked();
            mHtmlWebView.resetUserClicked();
        }

        return wasIntentStarted;
    }

    private boolean executeIntent(Context context, Intent intent, String errorMessage) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.d("MoPub", (errorMessage != null)
                    ? errorMessage
                    : "Unable to start intent.");
            return false;
        }
        return true;
    }
}
