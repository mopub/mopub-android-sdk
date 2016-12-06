package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.Nullable;

import com.mopub.common.VisibleForTesting;

import java.util.Map;

class RewardedVastVideoInterstitial extends VastVideoInterstitial {

    interface CustomEventRewardedVideoInterstitialListener extends CustomEventInterstitialListener {
        void onVideoComplete();
    }

    @Nullable private RewardedVideoBroadcastReceiver mRewardedVideoBroadcastReceiver;

    public RewardedVastVideoInterstitial() {
        super();
    }

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        super.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        if (customEventInterstitialListener instanceof CustomEventRewardedVideoInterstitialListener) {
            mRewardedVideoBroadcastReceiver = new RewardedVideoBroadcastReceiver(
                    (CustomEventRewardedVideoInterstitialListener) customEventInterstitialListener,
                    mBroadcastIdentifier);
            mRewardedVideoBroadcastReceiver.register(mRewardedVideoBroadcastReceiver, context);
        }
    }

    @Override
    public void onVastVideoConfigurationPrepared(final VastVideoConfig vastVideoConfig) {
        if (vastVideoConfig != null) {
            vastVideoConfig.setIsRewardedVideo(true);
        }
        super.onVastVideoConfigurationPrepared(vastVideoConfig);
    }

    @Override
    public void onInvalidate() {
        super.onInvalidate();
        if (mRewardedVideoBroadcastReceiver != null) {
            mRewardedVideoBroadcastReceiver.unregister(mRewardedVideoBroadcastReceiver);
        }
    }

    @VisibleForTesting
    @Deprecated
    @Nullable
    RewardedVideoBroadcastReceiver getRewardedVideoBroadcastReceiver() {
        return mRewardedVideoBroadcastReceiver;
    }
}
