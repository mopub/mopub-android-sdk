package com.mopub.mobileads;


import android.net.Uri;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;

class MraidInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        MraidActivity.preRenderHtml(mContext, customEventInterstitialListener, mHtmlData);
    }

    @Override
    protected void showInterstitial() {
        MraidActivity.start(mContext, mHtmlData, mAdConfiguration);
    }
}
