package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.ads.AdError;
import com.facebook.ads.RewardedVideoAd;
import com.facebook.ads.RewardedVideoAdListener;
import com.mopub.common.LifecycleListener;
import com.facebook.ads.Ad;
import com.mopub.common.MoPubReward;

import java.util.Map;

/**
 * Certified with Facebook Audience Network 4.26.1
 */
public class FacebookRewardedVideo extends CustomEventRewardedVideo implements RewardedVideoAdListener {

    @Nullable
    private RewardedVideoAd mRewardedVideoAd;
    @Nullable
    private String mPlacementId;
    private String TAG = "mopub";

    /**
     * CustomEventRewardedVideo implementation
     */

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        // Facebook doesn't have a dedicated initialization call, so we return false and do nothing.
        return false;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (!serverExtras.isEmpty()) {
            mPlacementId = serverExtras.get("placement_id");

            if (!TextUtils.isEmpty(mPlacementId)) {
                if (mRewardedVideoAd != null) {
                    mRewardedVideoAd.destroy();
                    mRewardedVideoAd = null;
                }
                Log.d(TAG, "Creating a Facebook Rewarded Video instance, and registering callbacks.");
                mRewardedVideoAd = new RewardedVideoAd(activity, mPlacementId);
                mRewardedVideoAd.setAdListener(this);
            } else {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, getAdNetworkId(), MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                Log.d(TAG, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR.toString());
                Log.d(TAG, "Placement ID is null or empty.");
                return;
            }
        }

        if (mRewardedVideoAd.isAdLoaded()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
            return;
        }

        if (mRewardedVideoAd != null) {
            Log.d(TAG, "Sending Facebook an ad request.");
            mRewardedVideoAd.loadAd();
        }
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return (mRewardedVideoAd != null) ? (mRewardedVideoAd.getPlacementId()) : ("");
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedVideoAd != null) {
            Log.d(TAG, "Performing cleanup tasks...");
            mRewardedVideoAd.setAdListener(null);
            mRewardedVideoAd.destroy();
            mRewardedVideoAd = null;
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedVideoAd != null && mRewardedVideoAd.isAdLoaded();
    }

    @Override
    protected void showVideo() {
        if (hasVideoAvailable()) {
            Log.d(TAG, "Facebook Rewarded Video creative is available. Showing...");
            mRewardedVideoAd.show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, MoPubErrorCode.VIDEO_NOT_AVAILABLE);
            Log.d(TAG, "Facebook Rewarded Video creative is not available. Try re-requesting.");
        }
    }

    @Override
    public void onRewardedVideoCompleted() {
        Log.d(TAG, "Facebook Rewarded Video creative is completed. Awarding the user.");
        MoPubRewardedVideoManager.onRewardedVideoCompleted(FacebookRewardedVideo.class, mPlacementId, MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT));
    }

    @Override
    public void onLoggingImpression(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoStarted(FacebookRewardedVideo.class, mPlacementId);
        Log.d(TAG, "Facebook Rewarded Video creative started playing.");
    }

    @Override
    public void onRewardedVideoClosed() {
        MoPubRewardedVideoManager.onRewardedVideoClosed(FacebookRewardedVideo.class, mPlacementId);
        Log.d(TAG, "Facebook Rewarded Video creative closed.");
    }

    @Override
    public void onAdLoaded(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FacebookRewardedVideo.class, mPlacementId);
        Log.d(TAG, "Facebook Rewarded Video creative cached.");
    }

    @Override
    public void onAdClicked(Ad ad) {
        MoPubRewardedVideoManager.onRewardedVideoClicked(FacebookRewardedVideo.class, mPlacementId);
        Log.d(TAG, "Facebook Rewarded Video creative clicked.");
    }

    @Override
    public void onError(Ad ad, AdError adError) {
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FacebookRewardedVideo.class, mPlacementId, mapErrorCode(adError.getErrorCode()));
        Log.d(TAG, "Loading/Playing Facebook Rewarded Video creative encountered an error: " + mapErrorCode(adError.getErrorCode()).toString());
    }

    @NonNull
    private static MoPubErrorCode mapErrorCode(int error) {
        switch (error) {
            case AdError.NO_FILL_ERROR_CODE:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case AdError.INTERNAL_ERROR_CODE:
                return MoPubErrorCode.INTERNAL_ERROR;
            case AdError.NETWORK_ERROR_CODE:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.UNSPECIFIED;
        }
    }
}
