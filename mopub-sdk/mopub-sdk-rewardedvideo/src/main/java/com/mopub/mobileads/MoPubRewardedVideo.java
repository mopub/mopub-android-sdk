package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

/**
 * A custom event for showing MoPub rewarded videos.
 */
public class MoPubRewardedVideo extends MoPubRewardedAd {

    @NonNull static final String MOPUB_REWARDED_VIDEO_ID = "mopub_rewarded_video_id";

    @Nullable private RewardedVastVideoInterstitial mRewardedVastVideoInterstitial;

    public MoPubRewardedVideo() {
        mRewardedVastVideoInterstitial = new RewardedVastVideoInterstitial();
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId != null ?  mAdUnitId : MOPUB_REWARDED_VIDEO_ID;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedVastVideoInterstitial != null) {
            mRewardedVastVideoInterstitial.onInvalidate();
        }
        mRewardedVastVideoInterstitial = null;
        super.onInvalidate();
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        super.loadWithSdkInitialized(activity, localExtras, serverExtras);

        if (mRewardedVastVideoInterstitial == null) {
            MoPubLog.w("mRewardedVastVideoInterstitial is null. Has this class been invalidated?");
            return;
        }
        mRewardedVastVideoInterstitial.loadInterstitial(activity,
                    new MoPubRewardedVideoListener(),
                    localExtras, serverExtras);
    }

    @Override
    protected void show() {
        if (isReady() && mRewardedVastVideoInterstitial != null) {
            MoPubLog.d("Showing MoPub rewarded video.");
            mRewardedVastVideoInterstitial.showInterstitial();
        } else {
            MoPubLog.d("Unable to show MoPub rewarded video");
        }
    }

    private class MoPubRewardedVideoListener extends MoPubRewardedAdListener implements RewardedVastVideoInterstitial.RewardedVideoInterstitialListener {

        public MoPubRewardedVideoListener() {
            super(MoPubRewardedVideo.class);
        }


        @Override
        public void onVideoComplete() {
            if (getRewardedAdCurrencyName() == null) {
                MoPubLog.d("No rewarded video was loaded, so no reward is possible");
            } else {
                MoPubRewardedVideoManager.onRewardedVideoCompleted(mCustomEventClass,
                        getAdNetworkId(),
                        MoPubReward.success(getRewardedAdCurrencyName(),
                                getRewardedAdCurrencyAmount()));
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    void setRewardedVastVideoInterstitial(
            @Nullable final RewardedVastVideoInterstitial rewardedVastVideoInterstitial) {
        mRewardedVastVideoInterstitial = rewardedVastVideoInterstitial;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    RewardedVastVideoInterstitial getRewardedVastVideoInterstitial() {
        return mRewardedVastVideoInterstitial;
    }
}
