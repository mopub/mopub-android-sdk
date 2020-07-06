// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

public class HtmlWebView extends BaseHtmlWebView {

    private HtmlWebViewClient mHtmlWebViewClient;

    public HtmlWebView(Context context) {
        super(context);
    }

    public void init(BaseWebViewListener baseWebViewListener, String clickthroughUrl, String dspCreativeId) {
        super.init();
        mHtmlWebViewClient = new HtmlWebViewClient(this, baseWebViewListener, clickthroughUrl, dspCreativeId);
        setWebViewClient(mHtmlWebViewClient);
    }
}
