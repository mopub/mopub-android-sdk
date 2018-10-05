// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.CreativeOrientation;

import java.util.Map;

import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;

public class HtmlInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;
    private String mClickthroughUrl;
    @NonNull
    private CreativeOrientation mOrientation;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
        mClickthroughUrl = serverExtras.get(CLICKTHROUGH_URL_KEY);
        mOrientation = CreativeOrientation.fromHeader(serverExtras.get(CREATIVE_ORIENTATION_KEY));
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        MoPubActivity.preRenderHtml(this, mContext, mAdReport, customEventInterstitialListener, mHtmlData, mClickthroughUrl, mBroadcastIdentifier);
    }

    @Override
    public void showInterstitial() {
        MoPubActivity.start(mContext, mHtmlData, mAdReport, mClickthroughUrl, mOrientation,
                mBroadcastIdentifier);
    }
}
