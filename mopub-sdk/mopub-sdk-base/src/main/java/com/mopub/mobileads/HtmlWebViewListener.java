package com.mopub.mobileads;

public interface HtmlWebViewListener {
    void onLoaded(BaseHtmlWebView mHtmlWebView);
    void onFailed(MoPubErrorCode unspecified);
    void onClicked();
    void onCollapsed();
}
