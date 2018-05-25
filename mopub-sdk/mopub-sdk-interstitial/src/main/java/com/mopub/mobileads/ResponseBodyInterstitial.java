package com.mopub.mobileads;

import android.content.Context;

import com.mopub.common.AdReport;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public abstract class ResponseBodyInterstitial extends CustomEventInterstitial {
    private EventForwardingBroadcastReceiver mBroadcastReceiver;
    protected Context mContext;
    protected AdReport mAdReport;
    protected long mBroadcastIdentifier;
    protected ExternalViewabilitySessionManager mExternalViewabilitySessionManager;

    abstract protected void extractExtras(Map<String, String> serverExtras);
    abstract protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener);
    public abstract void showInterstitial();

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {

        mContext = context;

        if (extrasAreValid(serverExtras)) {
            extractExtras(serverExtras);
        } else {
            customEventInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            return;
        }


        try {
            mAdReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            Long boxedBroadcastId = (Long) localExtras.get(BROADCAST_IDENTIFIER_KEY);
            if (boxedBroadcastId == null) {
                MoPubLog.e("Broadcast Identifier was not set in localExtras");
                customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                return;
            }
            mBroadcastIdentifier = boxedBroadcastId;
        } catch (ClassCastException e) {
            MoPubLog.e("LocalExtras contained an incorrect type.");
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            return;
        }

        mBroadcastReceiver = new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                mBroadcastIdentifier);
        mBroadcastReceiver.register(mBroadcastReceiver, context);

        preRenderHtml(customEventInterstitialListener);
    }

    @Override
    public void onInvalidate() {
        if (mBroadcastReceiver != null) {
            mBroadcastReceiver.unregister(mBroadcastReceiver);
        }
    }

    private boolean extrasAreValid(Map<String,String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
