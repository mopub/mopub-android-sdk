package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

/**
 * Contains the common logic for rewarded ads.
 */
public abstract class MoPubRewardedAd extends CustomEventRewardedAd {

    private boolean mIsLoaded;
    @Nullable private String mRewardedAdCurrencyName;
    private int mRewardedAdCurrencyAmount;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        // This custom event does not need additional lifecycle listeners.
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        // No additional initialization is necessary.
        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        Preconditions.checkNotNull(activity, "activity cannot be null");
        Preconditions.checkNotNull(localExtras, "localExtras cannot be null");
        Preconditions.checkNotNull(serverExtras, "serverExtras cannot be null");

        final Object rewardedAdCurrencyName = localExtras.get(
                DataKeys.REWARDED_AD_CURRENCY_NAME_KEY);
        if (rewardedAdCurrencyName instanceof String) {
            mRewardedAdCurrencyName = (String) rewardedAdCurrencyName;
        } else {
            MoPubLog.d("No currency name specified for rewarded video. Using the default name.");
            mRewardedAdCurrencyName = MoPubReward.NO_REWARD_LABEL;
        }

        final Object rewardedAdCurrencyAmount = localExtras.get(
                DataKeys.REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY);
        if (rewardedAdCurrencyAmount instanceof String) {
            try {
                mRewardedAdCurrencyAmount = Integer.parseInt(
                        (String) rewardedAdCurrencyAmount);
            } catch (NumberFormatException e) {
                MoPubLog.d(
                        "Unable to convert currency amount: " + rewardedAdCurrencyAmount +
                                ". Using the default reward amount: " +
                                MoPubReward.DEFAULT_REWARD_AMOUNT);
                mRewardedAdCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
            }
        } else {
            MoPubLog.d(
                    "No currency amount specified for rewarded ad. Using the default reward amount: " +
                            MoPubReward.DEFAULT_REWARD_AMOUNT);
            mRewardedAdCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
        }

        if (mRewardedAdCurrencyAmount < 0) {
            MoPubLog.d(
                    "Negative currency amount specified for rewarded ad. Using the default reward amount: " +
                            MoPubReward.DEFAULT_REWARD_AMOUNT);
            mRewardedAdCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
        }
    }

    @Override
    protected void onInvalidate() {
        mIsLoaded = false;
    }

    @Override
    protected boolean isReady() {
        return mIsLoaded;
    }

    protected class MoPubRewardedAdListener implements CustomEventInterstitial.CustomEventInterstitialListener {

        @NonNull final Class<? extends MoPubRewardedAd>  mCustomEventClass;

        public MoPubRewardedAdListener(@NonNull final Class<? extends MoPubRewardedAd>
                customEventClass) {
            Preconditions.checkNotNull(customEventClass);

            mCustomEventClass = customEventClass;
        }

        @Override
        public void onInterstitialLoaded() {
            mIsLoaded = true;
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(mCustomEventClass,
                    getAdNetworkId());
        }

        @Override
        public void onInterstitialFailed(final MoPubErrorCode errorCode) {
            switch (errorCode) {
                case VIDEO_PLAYBACK_ERROR:
                    MoPubRewardedVideoManager.onRewardedVideoPlaybackError(mCustomEventClass,
                            getAdNetworkId(), errorCode);
                    break;
                default:
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(mCustomEventClass,
                            getAdNetworkId(), errorCode);
            }
        }

        @Override
        public void onInterstitialShown() {
            MoPubRewardedVideoManager.onRewardedVideoStarted(mCustomEventClass, getAdNetworkId());
        }

        @Override
        public void onInterstitialClicked() {
            MoPubRewardedVideoManager.onRewardedVideoClicked(mCustomEventClass, getAdNetworkId());
        }

        @Override
        public void onLeaveApplication() {
        }

        @Override
        public void onInterstitialDismissed() {
            MoPubRewardedVideoManager.onRewardedVideoClosed(mCustomEventClass, getAdNetworkId());
        }
    }

    @Nullable
    protected String getRewardedAdCurrencyName() {
        return mRewardedAdCurrencyName;
    }

    protected int getRewardedAdCurrencyAmount() {
        return mRewardedAdCurrencyAmount;
    }

    @Deprecated
    @VisibleForTesting
    void setIsLoaded(final boolean isLoaded) {
        mIsLoaded = isLoaded;
    }

}
