// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

public interface HtmlWebViewListener {
    void onLoaded(BaseHtmlWebView mHtmlWebView);
    void onFailed(MoPubErrorCode unspecified);
    void onClicked();
    void onCollapsed();
}
