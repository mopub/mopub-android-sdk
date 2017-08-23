package com.mopub.mobileads;
import java.util.Arrays;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.config.ConfigFile;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.RewardedVideoListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;

import java.util.Map;

import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoClosed;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoCompleted;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadFailure;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadSuccess;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoPlaybackError;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoStarted;

public class IronSourceRewardedVideo extends CustomEventRewardedAd implements LifecycleListener  {

    /**
     * private vars
     */
    private static final String TAG = IronSourceRewardedVideo.class.getSimpleName();

    private static final String ADAPTER_VERSION = "2.5.2";
    private static final String ADAPTER_NAME = "Mopub";
    private static final String IRON_SOURCE_AD_NETWORK_ID = "ironsrc_id";

    private String applicationKey;
    private String placementName;
    private boolean isTestEnabled;

    private static ironSourceRewardedVideoListener sIronSrcRvListener;

    private int rewardAmount;
    private String rewardName;
    private static boolean isSDKRVInitSuccess;

    IronSourceRewardedVideo() {
        sIronSrcRvListener = new ironSourceRewardedVideoListener();
    }

    /**
     * Activity Lifecycle Helper Methods
     **/
    public static void onActivityPaused(Activity activity) {
        IronSource.onPause(activity);
    }

    public static void onActivityResumed(Activity activity) {
        IronSource.onResume(activity);
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return this;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return IRON_SOURCE_AD_NETWORK_ID;
    }

    @Override
    protected void onInvalidate() {

    }

    @Override
    protected boolean isReady() {
        return IronSource.isRewardedVideoAvailable();
    }

    @Override
    protected void show() {
        if (TextUtils.isEmpty(placementName)) {
            IronSource.showRewardedVideo();
        } else {
            IronSource.showRewardedVideo(placementName);
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        if (serverExtras.get("applicationKey") != null) {
            applicationKey = serverExtras.get("applicationKey");
        } else if (serverExtras.get("appKey") != null) {
            //try appKey if applicationKey doesn't exists (fallback)
            applicationKey = serverExtras.get("appKey");
        }

        if (serverExtras.get("placementName") != null) {
            placementName = serverExtras.get("placementName");
        }
        if (serverExtras.get("isTestEnabled") != null) {
            isTestEnabled = Boolean.valueOf(serverExtras.get("isTestEnabled"));
        }

        onLog("server extras: " + Arrays.toString(serverExtras.entrySet().toArray()));

        IronSource.setRewardedVideoListener(sIronSrcRvListener);
        initIronSourceSDK(launcherActivity);

        return true;

    }

    private void initIronSourceSDK(Activity activity) {
        if (!isSDKRVInitSuccess) {
            ConfigFile.getConfigFile()
                    .setPluginData(ADAPTER_NAME, ADAPTER_VERSION, MoPub.SDK_VERSION);
            IronSource.setMediationType("mopub");
            IronSource.init(activity, applicationKey, IronSource.AD_UNIT.REWARDED_VIDEO, IronSource.AD_UNIT.INTERSTITIAL);
            isSDKRVInitSuccess = true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (IronSource.isRewardedVideoAvailable()) {
            onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
        }
    }

    //****************************Mopub lifeCircle*******************************

    @Override
    public void onCreate(@NonNull Activity activity) {

    }

    @Override
    public void onStart(@NonNull Activity activity) {

    }

    @Override
    public void onPause(@NonNull Activity activity) {
        IronSource.onPause(activity);
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        IronSource.onResume(activity);
    }

    @Override
    public void onRestart(@NonNull Activity activity) {

    }

    @Override
    public void onStop(@NonNull Activity activity) {

    }

    @Override
    public void onDestroy(@NonNull Activity activity) {

    }

    @Override
    public void onBackPressed(@NonNull Activity activity) {

    }

    //****************************IronSource RewardedVideoListener Start*******************************

    private class ironSourceRewardedVideoListener implements RewardedVideoListener {

        //Invoked when the RewardedVideo ad view has opened.
        @Override
        public void onRewardedVideoAdOpened() {
            onLog("onRewardedVideoAdOpened");
            rewardName = MoPubReward.NO_REWARD_LABEL;
            rewardAmount = 0;
            onRewardedVideoStarted(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
        }

        //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
        @Override
        public void onRewardedVideoAdClosed() {
            onLog("onRewardedVideoAdClosed, rewardName: " + rewardName + " rewardAmount: " + rewardAmount);
            onRewardedVideoClosed(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);

        }

        //Invoked when there is a change in the ad availability status.
        @Override
        public void onRewardedVideoAvailabilityChanged(boolean available) {
            if (available) {
                onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
                onLog("onRewardedVideoLoadSuccess");
            } else {
                onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.NETWORK_NO_FILL);
                onLog("onRewardedVideoLoadFailure");
            }
            onLog("onVideoAvailabilityChanged");
        }

        //Invoked when the video ad starts playing. (Available for: AdColony, Vungle, AppLovin, UnityAds)
        @Override
        public void onRewardedVideoAdStarted() {
            onLog("onVideoStart");
        }

        //Invoked when the video ad finishes playing. (Available for: AdColony, Flurry, Vungle, AppLovin, UnityAds)
        @Override
        public void onRewardedVideoAdEnded() {
            onLog("onVideoEnd");
        }

        //Invoked when the user completed the video and should be rewarded.
        @Override
        public void onRewardedVideoAdRewarded(Placement placement) {
            //Placement can return null if the placementName is not valid.
            if (placement != null) {
                rewardName = placement.getRewardName();
                rewardAmount = placement.getRewardAmount();
                MoPubReward reward = MoPubReward.success(rewardName, rewardAmount);
                onRewardedVideoCompleted(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, reward);
            }
            onLog("onRewardedVideoAdRewarded");
        }

        //Invoked when an Ad failed to display.
        @Override
        public void onRewardedVideoAdShowFailed(IronSourceError ironSourceError) {
            setMopubErrorMessage(ironSourceError);
            onLog("onRewardedVideoShowFail");
        }

    }

    private void onLog(String message) {
        if (isTestEnabled) {
            Log.d(TAG, message);
        }
    }

    private void setMopubErrorMessage(IronSourceError ironSourceError) {
        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
                break;
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.VIDEO_CACHE_ERROR);
                break;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.NETWORK_NO_FILL);
                break;
            case IronSourceError.ERROR_CODE_GENERIC:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.INTERNAL_ERROR);
                break;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.NO_CONNECTION);
                break;
            default:
                onRewardedVideoPlaybackError(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.NETWORK_TIMEOUT);
                break;
        }
    }

    //****************************IronSource RewardedVideoListener End*******************************
}
