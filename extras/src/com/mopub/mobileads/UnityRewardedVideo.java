package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

/**
 * A custom event for showing Unity rewarded videos.
 *
 * Certified with Unity Ads 2.1.1
 */
public class UnityRewardedVideo extends CustomEventRewardedVideo {
    private static final String GAME_ID_KEY = "gameId";
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final UnityAdsListener sUnityAdsListener = new UnityAdsListener();
    private static String sPlacementId = "";

    @Nullable
    private Activity mLauncherActivity;

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sUnityAdsListener;
    }

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return sPlacementId;
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (UnityRewardedVideo.class) {
            if (UnityAds.isInitialized()) {
                return false;
            }

            try {
                UnityRouter.initUnityAds(serverExtras, launcherActivity);
                UnityRouter.addListener(sPlacementId, sUnityAdsListener);
            } catch (UnityRouter.UnityAdsException e) {
                MoPubLog.e("Failed to initialize Unity Ads.", e);
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, UnityRouter.UnityAdsUtils.getMoPubErrorCode(e.getErrorCode()));
            }

            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) throws Exception {

        sPlacementId = UnityRouter.placementIdForServerExtras(serverExtras, sPlacementId);
        mLauncherActivity = activity;

        UnityRouter.addListener(sPlacementId, sUnityAdsListener);
        if (UnityAds.isReady()) {
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, sPlacementId);
        }
    }

    @Override
    public boolean hasVideoAvailable() {
        return UnityAds.isReady(sPlacementId);
    }

    @Override
    public void showVideo() {
        if (hasVideoAvailable()) {
            UnityAds.show(mLauncherActivity, sPlacementId);
        } else {
            MoPubLog.d("Attempted to show Unity rewarded video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        UnityRouter.removeListener(sPlacementId);
    }

    private static final class UnityLifecycleListener extends BaseLifecycleListener {
        @Override
        public void onCreate(@NonNull final Activity activity) {
            super.onCreate(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
        }

    }

    private static class UnityAdsListener implements IUnityAdsExtendedListener,
            CustomEventRewardedVideoListener {
        @Override
        public void onUnityAdsReady(String placementId) {
            if (placementId.equals(sPlacementId)) {
                MoPubLog.d("Unity rewarded video cached for placement " + placementId + ".");
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, placementId);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(UnityRewardedVideo.class, placementId);
            MoPubLog.d("Unity rewarded video started for placement " + placementId + ".");
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
            MoPubLog.d("Unity Ad finished with finish state = " + finishState);
            if (finishState == UnityAds.FinishState.ERROR) {
                MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                        UnityRewardedVideo.class,
                        sPlacementId,
                        MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                MoPubLog.d("Unity rewarded video encountered a playback error for placement " + placementId);
            } else if (finishState == UnityAds.FinishState.COMPLETED) {
                MoPubRewardedVideoManager.onRewardedVideoCompleted(
                        UnityRewardedVideo.class,
                        sPlacementId,
                        MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));
                MoPubLog.d("Unity rewarded video completed for placement " + placementId);
            } else if (finishState == UnityAds.FinishState.SKIPPED) {
                MoPubLog.d("Unity ad was skipped, no reward will be given.");
            }
            MoPubRewardedVideoManager.onRewardedVideoClosed(UnityRewardedVideo.class, sPlacementId);
            UnityRouter.removeListener(placementId);
        }

        @Override
        public void onUnityAdsClick(String placementId) {
            MoPubRewardedVideoManager.onRewardedVideoClicked(UnityRewardedVideo.class, placementId);
            MoPubLog.d("Unity rewarded video clicked for placement " + placementId + ".");
        }

        // @Override
        public void onUnityAdsPlacementStateChanged(String placementId, UnityAds.PlacementState oldState, UnityAds.PlacementState newState) {

        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
            MoPubLog.d("Unity rewarded video cache failed for placement " + sPlacementId + ".");
            MoPubErrorCode errorCode = UnityRouter.UnityAdsUtils.getMoPubErrorCode(unityAdsError);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, errorCode);
        }
    }

    @VisibleForTesting
    void reset() {
        sPlacementId = "";
    }
}
