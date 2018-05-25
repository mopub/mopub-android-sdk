package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mraid.RewardedMraidInterstitial;

import java.util.Map;

/**
 * A custom event for showing MoPub rewarded playables.
 */
public class MoPubRewardedPlayable extends MoPubRewardedAd {

    @NonNull static final String MOPUB_REWARDED_PLAYABLE_ID = "mopub_rewarded_playable_id";
    @Nullable private RewardedMraidInterstitial mRewardedMraidInterstitial;

    public MoPubRewardedPlayable() {
        mRewardedMraidInterstitial = new RewardedMraidInterstitial();
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        super.loadWithSdkInitialized(activity, localExtras, serverExtras);

        if (mRewardedMraidInterstitial == null) {
            MoPubLog.w("mRewardedMraidInterstitial is null. Has this class been invalidated?");
            return;
        }
        mRewardedMraidInterstitial.loadInterstitial(activity, new MoPubRewardedPlayableListener(),
                localExtras, serverExtras);
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mAdUnitId != null ? mAdUnitId : MOPUB_REWARDED_PLAYABLE_ID;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedMraidInterstitial != null) {
            mRewardedMraidInterstitial.onInvalidate();
        }
        mRewardedMraidInterstitial = null;
        super.onInvalidate();
    }

    @Override
    protected void show() {
        if (isReady() && mRewardedMraidInterstitial != null) {
            MoPubLog.d("Showing MoPub rewarded playable.");
            mRewardedMraidInterstitial.showInterstitial();
        } else {
            MoPubLog.d("MoPub rewarded playable not loaded. Unable to show playable.");
        }
    }

    private class MoPubRewardedPlayableListener extends MoPubRewardedAdListener implements RewardedMraidInterstitial.RewardedMraidInterstitialListener {

        public MoPubRewardedPlayableListener() {
            super(MoPubRewardedPlayable.class);
        }

        @Override
        public void onMraidComplete() {
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
    void setRewardedMraidInterstitial(
            @NonNull final RewardedMraidInterstitial rewardedMraidInterstitial) {
        mRewardedMraidInterstitial = rewardedMraidInterstitial;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    RewardedMraidInterstitial getRewardedMraidInterstitial() {
        return mRewardedMraidInterstitial;
    }
}
