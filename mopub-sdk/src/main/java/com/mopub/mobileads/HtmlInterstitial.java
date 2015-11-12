package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.webkit.WebView;

import com.integralads.verification.app_verification_library.AvidManager;
import com.mopub.common.CreativeOrientation;

import java.security.AccessControlContext;
import java.util.Map;

import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REDIRECT_URL_KEY;
import static com.mopub.common.DataKeys.SCROLLABLE_KEY;

public class HtmlInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;
    private boolean mIsScrollable;
    private String mRedirectUrl;
    private String mClickthroughUrl;
    @NonNull
    private CreativeOrientation mOrientation;
    private WebView webView;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
        mIsScrollable = Boolean.valueOf(serverExtras.get(SCROLLABLE_KEY));
        mRedirectUrl = serverExtras.get(REDIRECT_URL_KEY);
        mClickthroughUrl = serverExtras.get(CLICKTHROUGH_URL_KEY);
        mOrientation = CreativeOrientation.fromHeader(serverExtras.get(CREATIVE_ORIENTATION_KEY));
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        webView = MoPubActivity.preRenderHtml(mContext, mAdReport, customEventInterstitialListener, mHtmlData);
        if (mContext instanceof Activity) {
            AvidManager.getInstance().registerAdView(webView, (Activity)mContext);
        } else {
            webView = null;
        }
    }

    @Override
    public void showInterstitial() {
        MoPubActivity.start(mContext, mHtmlData, mAdReport, mIsScrollable,
                mRedirectUrl, mClickthroughUrl, mOrientation,
                mBroadcastIdentifier);
        unregisterWebView();
    }

    @Override
    public void onInvalidate() {
        super.onInvalidate();
        unregisterWebView();
    }

    private void unregisterWebView() {
        if (webView != null) {
            AvidManager.getInstance().unregisterAdView(webView);
            webView = null;
        }
    }
}
