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
 * A custom event for showing MoPub rewarded videos.
 */
public class MoPubRewardedVideo extends CustomEventRewardedVideo {

    @NonNull private static final String MOPUB_REWARDED_VIDEO_ID = "mopub_rewarded_video_id";

    @NonNull private RewardedVastVideoInterstitial mRewardedVastVideoInterstitial;
    @Nullable private String mRewardedVideoCurrencyName;
    private int mRewardedVideoCurrencyAmount;
    private boolean mIsLoaded;

    public MoPubRewardedVideo() {
        mRewardedVastVideoInterstitial = new RewardedVastVideoInterstitial();
    }

    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        // Since MoPub is the SDK, there is no reason to get the SDK video listener
        // since we have direct access.
        return null;
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        // RewardedVastVideoInterstitial will handle all lifecycle events.
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return MOPUB_REWARDED_VIDEO_ID;
    }

    @Override
    protected void onInvalidate() {
        mRewardedVastVideoInterstitial.onInvalidate();
        mIsLoaded = false;
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

        final Object rewardedVideoCurrencyName = localExtras.get(
                DataKeys.REWARDED_VIDEO_CURRENCY_NAME_KEY);
        if (rewardedVideoCurrencyName instanceof String) {
            mRewardedVideoCurrencyName = (String) rewardedVideoCurrencyName;
        } else {
            MoPubLog.d("No currency name specified for rewarded video. Using the default name.");
            mRewardedVideoCurrencyName = MoPubReward.NO_REWARD_LABEL;
        }

        final Object rewardedVideoCurrencyAmount = localExtras.get(
                DataKeys.REWARDED_VIDEO_CURRENCY_AMOUNT_STRING_KEY);
        if (rewardedVideoCurrencyAmount instanceof String) {
            try {
                mRewardedVideoCurrencyAmount = Integer.parseInt(
                        (String) rewardedVideoCurrencyAmount);
            } catch (NumberFormatException e) {
                MoPubLog.d(
                        "Unable to convert currency amount: " + rewardedVideoCurrencyAmount +
                                ". Using the default reward amount: " +
                                MoPubReward.DEFAULT_REWARD_AMOUNT);
                mRewardedVideoCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
            }
        } else {
            MoPubLog.d(
                    "No currency amount specified for rewarded video. Using the default reward amount: " +
                            MoPubReward.DEFAULT_REWARD_AMOUNT);
            mRewardedVideoCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
        }

        if (mRewardedVideoCurrencyAmount < 0) {
            MoPubLog.d(
                    "Negative currency amount specified for rewarded video. Using the default reward amount: " +
                            MoPubReward.DEFAULT_REWARD_AMOUNT);
            mRewardedVideoCurrencyAmount = MoPubReward.DEFAULT_REWARD_AMOUNT;
        }

        mRewardedVastVideoInterstitial.loadInterstitial(activity, new MoPubRewardedVideoListener(),
                localExtras, serverExtras);
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mIsLoaded;
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            MoPubLog.d("Showing MoPub rewarded video.");
            mRewardedVastVideoInterstitial.showInterstitial();
        } else {
            MoPubLog.d("Unable to show MoPub rewarded video");
        }
    }

    private class MoPubRewardedVideoListener implements CustomEventInterstitial.CustomEventInterstitialListener, RewardedVastVideoInterstitial.CustomEventRewardedVideoInterstitialListener {
        @Override
        public void onInterstitialLoaded() {
            mIsLoaded = true;
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(MoPubRewardedVideo.class,
                    MOPUB_REWARDED_VIDEO_ID);
        }

        @Override
        public void onInterstitialFailed(final MoPubErrorCode errorCode) {
            switch (errorCode) {
                case VIDEO_PLAYBACK_ERROR:
                    MoPubRewardedVideoManager.onRewardedVideoPlaybackError(MoPubRewardedVideo.class,
                            MOPUB_REWARDED_VIDEO_ID, errorCode);
                    break;
                default:
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(MoPubRewardedVideo.class,
                            MOPUB_REWARDED_VIDEO_ID, errorCode);
            }
        }

        @Override
        public void onInterstitialShown() {
            MoPubRewardedVideoManager.onRewardedVideoStarted(MoPubRewardedVideo.class,
                    MOPUB_REWARDED_VIDEO_ID);
        }

        @Override
        public void onInterstitialClicked() {
            MoPubRewardedVideoManager.onRewardedVideoClicked(MoPubRewardedVideo.class,
                    MOPUB_REWARDED_VIDEO_ID);
        }

        @Override
        public void onLeaveApplication() {
        }

        @Override
        public void onInterstitialDismissed() {
            MoPubRewardedVideoManager.onRewardedVideoClosed(MoPubRewardedVideo.class,
                    MOPUB_REWARDED_VIDEO_ID);
        }

        @Override
        public void onVideoComplete() {
            if (mRewardedVideoCurrencyName == null) {
                MoPubLog.d("No rewarded video was loaded, so no reward is possible");
            } else {
                MoPubRewardedVideoManager.onRewardedVideoCompleted(MoPubRewardedVideo.class,
                        MOPUB_REWARDED_VIDEO_ID,
                        MoPubReward.success(mRewardedVideoCurrencyName,
                                mRewardedVideoCurrencyAmount));
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    void setRewardedVastVideoInterstitial(
            @NonNull final RewardedVastVideoInterstitial rewardedVastVideoInterstitial) {
        mRewardedVastVideoInterstitial = rewardedVastVideoInterstitial;
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    String getRewardedVideoCurrencyName() {
        return mRewardedVideoCurrencyName;
    }

    @Deprecated
    @VisibleForTesting
    int getRewardedVideoCurrencyAmount() {
        return mRewardedVideoCurrencyAmount;
    }

    @Deprecated
    @VisibleForTesting
    void setIsLoaded(final boolean isLoaded) {
        mIsLoaded = isLoaded;
    }
}
