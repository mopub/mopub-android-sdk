package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;

import java.util.Map;

/**
 * A custom event for showing Unity rewarded videos.
 *
 * Certified with Unity 2.0.0
 */
public class UnityRewardedVideo extends CustomEventRewardedVideo {
    private static final String GAME_ID_KEY = "gameId";
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final UnityAdsListener sUnityAdsListener = new UnityAdsListener();

    private static boolean sInitialized = false;
    private static String sPlacementId = UnityRouter.DEFAULT_PLACEMENT_ID;

    @Nullable private Activity mLauncherActivity;

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
        if (sInitialized) {
            return false;
        }

        UnityRouter.initUnityAds(serverExtras, launcherActivity, sUnityAdsListener, new Runnable() {
            @Override
            public void run() {
                throw new IllegalStateException("Unity rewarded video initialization failed due " +
                        "to empty or missing " + GAME_ID_KEY);
            }
        });
        mLauncherActivity = launcherActivity;
        sInitialized = true;

        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
                                          @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {

        sPlacementId = UnityRouter.placementIdForServerExtras(serverExtras);
        UnityRouter.initPlacement(sPlacementId, new Runnable() {
            @Override
            public void run() {
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            }
        }, new Runnable() {
            @Override
            public void run() {
                loadRewardedVideo(sPlacementId);
            }
        });
    }

    @Override
    public boolean hasVideoAvailable() {
        return UnityRouter.hasVideoAvailable(sPlacementId);
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
        UnityAds.setListener(null);
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

    private static class UnityAdsListener implements IUnityAdsListener,
            CustomEventRewardedVideoListener {
        @Override
        public void onUnityAdsReady(String placementId) {
            if (UnityRouter.hasVideoAvailable(placementId)) {
                MoPubLog.d("Unity rewarded video cached for placement " + placementId + ".");
                loadRewardedVideo(placementId);
            }
        }

        @Override
        public void onUnityAdsStart(String placementId) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(UnityRewardedVideo.class, placementId);
            MoPubLog.d("Unity rewarded video started for placement " + placementId + ".");
        }

        @Override
        public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {
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
            } else {
                MoPubRewardedVideoManager.onRewardedVideoCompleted(
                        UnityRewardedVideo.class,
                        placementId,
                        MoPubReward.failure());
                MoPubLog.d("Unity rewarded video skipped for placement " + placementId);
            }
            MoPubRewardedVideoManager.onRewardedVideoClosed(UnityRewardedVideo.class, sPlacementId);
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
            MoPubLog.d("Unity rewarded video cache failed for placement " + sPlacementId + ".");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class,
                    sPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    private static void loadRewardedVideo(String placementId) {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, placementId);
    }

    @VisibleForTesting
    void reset() {
        sInitialized = false;
        sPlacementId = UnityRouter.DEFAULT_PLACEMENT_ID;
    }
}
