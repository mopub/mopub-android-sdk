package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.integralads.avid.library.mopub.session.AbstractAvidAdSession;
import com.integralads.avid.library.mopub.session.AvidAdSessionManager;
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
    private String avidAdSessionId;
    @NonNull
    private CreativeOrientation mOrientation;

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
        avidAdSessionId = MoPubActivity.preRenderHtml(mContext, mAdReport, customEventInterstitialListener, mHtmlData);
    }

    @Override
    public void showInterstitial() {
        MoPubActivity.start(mContext, mHtmlData, mAdReport, mIsScrollable,
                mRedirectUrl, mClickthroughUrl, mOrientation,
                mBroadcastIdentifier, avidAdSessionId);
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
