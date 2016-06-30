package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;

import java.util.Map;

/**
 * A custom event for showing Unity rewarded videos.
 *
 * Certified with Unity 2.0.0
 */
public class UnityRewardedVideo extends CustomEventRewardedVideo {
    private static final String DEFAULT_PLACEMENT_ID = "";
    private static final String GAME_ID_KEY = "gameId";
    private static final String ZONE_ID_KEY = "zoneId";
    private static final String PLACEMENT_ID_KEY = "placementId";
    private static final LifecycleListener sLifecycleListener = new UnityLifecycleListener();
    private static final UnityAdsListener sUnityAdsListener = new UnityAdsListener();

    private static boolean sInitialized = false;
    @NonNull private static String sPlacementId = DEFAULT_PLACEMENT_ID;

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

        String gameId;
        if (serverExtras.containsKey(GAME_ID_KEY)) {
            gameId = serverExtras.get(GAME_ID_KEY);
            if (TextUtils.isEmpty(gameId)) {
                throw new IllegalStateException("Unity rewarded video initialization failed due " +
                        "to empty " + GAME_ID_KEY);
            }
        } else {
            throw new IllegalStateException("Unity rewarded video initialization failed due to " +
                    "missing " + GAME_ID_KEY);
        }

        MediationMetaData mediationMetaData = new MediationMetaData(launcherActivity.getApplicationContext());
        mediationMetaData.setName("MoPub");
        mediationMetaData.setVersion(MoPub.SDK_VERSION);
        mediationMetaData.commit();

        UnityAds.initialize(launcherActivity, gameId, sUnityAdsListener);
        mLauncherActivity = launcherActivity;
        sInitialized = true;

        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {

        String placementId = null;
        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else if (serverExtras.containsKey(ZONE_ID_KEY)) {
            placementId = serverExtras.get(ZONE_ID_KEY);
        }
        sPlacementId = TextUtils.isEmpty(placementId) ? sPlacementId : placementId;

        if (TextUtils.isEmpty(sPlacementId)) {
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, sPlacementId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        } else if (hasVideoAvailable(sPlacementId)) {
            loadRewardedVideo(sPlacementId);
        }
    }

    @Override
    public boolean hasVideoAvailable() {
        return hasVideoAvailable(sPlacementId);
    }

    private static boolean hasVideoAvailable(String placementId) {
        return UnityAds.isReady(placementId) && UnityAds.getPlacementState(placementId) == UnityAds.PlacementState.READY;
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
            // TODO: is there something we should do in place of changeActivity?
//            UnityAds.changeActivity(activity);
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
            // TODO: is there something we should do in place of changeActivity?
//            UnityAds.changeActivity(activity);
        }

    }

    private static class UnityAdsListener implements IUnityAdsListener,
            CustomEventRewardedVideoListener {
        @Override
        public void onUnityAdsReady(String placementId) {
            if (hasVideoAvailable(placementId)) {
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
            if (finishState == UnityAds.FinishState.COMPLETED) {
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
        }

        @Override
        public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {
            // TODO: since we're not passed the placementId, I'm using the static placement id - is this OK?
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
        sPlacementId = DEFAULT_PLACEMENT_ID;
    }
}
