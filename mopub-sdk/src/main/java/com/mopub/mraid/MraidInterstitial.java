package com.mopub.mraid;


import android.support.annotation.NonNull;

import com.integralads.avid.library.mopub.session.AbstractAvidAdSession;
import com.integralads.avid.library.mopub.session.AvidAdSessionManager;
import com.mopub.mobileads.MraidActivity;
import com.mopub.mobileads.ResponseBodyInterstitial;

import java.util.Map;

import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;

class MraidInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;
    private String avidAdSessionId;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mHtmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
    }

    @Override
    protected void preRenderHtml(@NonNull CustomEventInterstitialListener
            customEventInterstitialListener) {
        avidAdSessionId = MraidActivity.preRenderHtml(mContext, customEventInterstitialListener, mHtmlData);
    }

    @Override
    public void showInterstitial() {
        MraidActivity.start(mContext, mAdReport, mHtmlData, mBroadcastIdentifier, avidAdSessionId);
    }

    @Override
    public void onInvalidate() {
        super.onInvalidate();
        if(avidAdSessionId != null) {
            AbstractAvidAdSession avidAdSession = AvidAdSessionManager.findAvidAdSessionById(avidAdSessionId);
            if(avidAdSession != null) {
                avidAdSession.endSession();
            }
            avidAdSessionId = null;
        }
    }
}
