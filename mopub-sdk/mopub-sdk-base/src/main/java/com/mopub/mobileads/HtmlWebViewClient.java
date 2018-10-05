// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;

import java.util.EnumSet;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

class HtmlWebViewClient extends WebViewClient {
    static final String MOPUB_FINISH_LOAD = "mopub://finishLoad";
    static final String MOPUB_FAIL_LOAD = "mopub://failLoad";

    private final EnumSet<UrlAction> SUPPORTED_URL_ACTIONS = EnumSet.of(
            UrlAction.HANDLE_MOPUB_SCHEME,
            UrlAction.IGNORE_ABOUT_SCHEME,
            UrlAction.HANDLE_PHONE_SCHEME,
            UrlAction.OPEN_APP_MARKET,
            UrlAction.OPEN_NATIVE_BROWSER,
            UrlAction.OPEN_IN_APP_BROWSER,
            UrlAction.HANDLE_SHARE_TWEET,
            UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
            UrlAction.FOLLOW_DEEP_LINK);

    private final Context mContext;
    private final String mDspCreativeId;
    private final HtmlWebViewListener mHtmlWebViewListener;
    private final BaseHtmlWebView mHtmlWebView;
    private final String mClickthroughUrl;

    HtmlWebViewClient(HtmlWebViewListener htmlWebViewListener,
            BaseHtmlWebView htmlWebView, String clickthrough, String dspCreativeId) {
        mHtmlWebViewListener = htmlWebViewListener;
        mHtmlWebView = htmlWebView;
        mClickthroughUrl = clickthrough;
        mDspCreativeId = dspCreativeId;
        mContext = htmlWebView.getContext();
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        new UrlHandler.Builder()
                .withDspCreativeId(mDspCreativeId)
                .withSupportedUrlActions(SUPPORTED_URL_ACTIONS)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (mHtmlWebView.wasClicked()) {
                            mHtmlWebViewListener.onClicked();
                            mHtmlWebView.onResetUserClick();
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withMoPubSchemeListener(new UrlHandler.MoPubSchemeListener() {
                    @Override
                    public void onFinishLoad() {
                        mHtmlWebViewListener.onLoaded(mHtmlWebView);
                    }

                    @Override
                    public void onClose() {
                        mHtmlWebViewListener.onCollapsed();
                    }

                    @Override
                    public void onFailLoad() {
                        mHtmlWebView.stopLoading();
                        mHtmlWebViewListener.onFailed(UNSPECIFIED);
                    }
                })
                .build().handleUrl(mContext, url, mHtmlWebView.wasClicked());
        return true;
    }
}
