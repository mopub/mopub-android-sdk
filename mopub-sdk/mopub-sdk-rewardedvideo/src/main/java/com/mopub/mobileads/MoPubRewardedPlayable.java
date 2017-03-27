package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;

import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mraid.RewardedMraidInterstitial;

import java.util.Map;

/**
 * A custom event for showing MoPub rewarded playables.
 */
public class MoPubRewardedPlayable extends MoPubRewardedAd {

    @NonNull private static final String MOPUB_REWARDED_PLAYABLE_ID = "mopub_rewarded_playable_id";
    @NonNull private RewardedMraidInterstitial mRewardedMraidInterstitial;

    public MoPubRewardedPlayable() {
        mRewardedMraidInterstitial = new RewardedMraidInterstitial();
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        super.loadWithSdkInitialized(activity, localExtras, serverExtras);

        mRewardedMraidInterstitial.loadInterstitial(activity, new MoPubRewardedPlayableListener(),
                localExtras, serverExtras);
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return MOPUB_REWARDED_PLAYABLE_ID;
    }

    @Override
    protected void onInvalidate() {
        mRewardedMraidInterstitial.onInvalidate();
        super.onInvalidate();
    }

    @Override
    protected void show() {
        if (isReady()) {
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
}
