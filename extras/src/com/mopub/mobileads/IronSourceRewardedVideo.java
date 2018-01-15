package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;

import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.model.Placement;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;

import java.util.Arrays;
import java.util.Map;

import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoClosed;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoCompleted;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadFailure;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoLoadSuccess;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoPlaybackError;
import static com.mopub.mobileads.MoPubRewardedVideoManager.onRewardedVideoStarted;

public class IronSourceRewardedVideo extends CustomEventRewardedVideo implements ISDemandOnlyRewardedVideoListener {

    /**
     * private vars
     */
    private static final String TAG = IronSourceRewardedVideo.class.getSimpleName();

    private static final String IRON_SOURCE_AD_NETWORK_ID = "ironsrc_id";

    // Configuration keys
    private static final String APPLICATION_KEY  = "applicationKey";
    private static final String APP_KEY          = "appKey";
    private static final String TEST_ENABLED_KEY = "isTestEnabled";
    private static final String PLACEMENT_KEY    = "placementName";
    private static final String INSTANCE_ID_KEY  = "instanceId";

    private static final String MEDIATION_TYPE   = "mopub" ;

    // This is the instance id used inside ironSource SDK
    private String mInstanceId = null;

    // This is the placement name used inside ironSource SDK
    private String mPlacementName = null;

    private static boolean mInitRewardedVideoSuccessfully;
    private boolean mIsTestEnabled;

    private int rewardAmount;
    private String rewardName;

    /**
     *  Mopub API
     */

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {

        return null;
    }

    @Override
    protected void onInvalidate() {

    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return IRON_SOURCE_AD_NETWORK_ID;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {

        String applicationKey = "";
        mInstanceId = "0";
        if (serverExtras.get(APPLICATION_KEY) != null) {
            applicationKey = serverExtras.get(APPLICATION_KEY);
        } else if (serverExtras.get(APP_KEY) != null) {
            //try appKey if applicationKey doesn't exists (fallback)
            applicationKey = serverExtras.get(APP_KEY);
        }

        if (serverExtras.get(PLACEMENT_KEY) != null) {
            mPlacementName = serverExtras.get(PLACEMENT_KEY);
        }

        if (serverExtras.get(TEST_ENABLED_KEY) != null) {
            mIsTestEnabled = Boolean.valueOf(serverExtras.get(TEST_ENABLED_KEY));
        }

        if (serverExtras.get(INSTANCE_ID_KEY) != null) {
            mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
        }

        onLog("server extras: " + Arrays.toString(serverExtras.entrySet().toArray()));

        initIronSourceSDK(launcherActivity, applicationKey);

        return true;

    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) throws Exception {
        if (IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId)) {
            onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
    }

    @Override
    protected boolean isReady() {
        return IronSource.isISDemandOnlyRewardedVideoAvailable(mInstanceId);
    }

    @Override
    protected void showVideo() {
        if (TextUtils.isEmpty(mPlacementName)) {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
        } else {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId,mPlacementName);
        }
    }

    @Override
    protected void show() {
        if (TextUtils.isEmpty(mPlacementName)) {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId);
        } else {
            IronSource.showISDemandOnlyRewardedVideo(mInstanceId,mPlacementName);
        }
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

    /**
     * Class Helper Methods
     **/

    private void initIronSourceSDK(Activity activity, String appKey) {
        if (!mInitRewardedVideoSuccessfully) {
            onLog("init RewardedVideo");
            IronSource.setISDemandOnlyRewardedVideoListener(this);
            IronSource.setMediationType(MEDIATION_TYPE);
            IronSource.initISDemandOnly(activity, appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            mInitRewardedVideoSuccessfully = true;
        }
    }

    private void onLog(String message) {
        if (mIsTestEnabled) {
            Log.d(TAG, message);
        }
    }

    private MoPubErrorCode getMoPubErrorMessage(IronSourceError ironSourceError) {
        if (ironSourceError == null) {
            return MoPubErrorCode.INTERNAL_ERROR;
        }

        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;

            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                return MoPubErrorCode.VIDEO_CACHE_ERROR;

            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                return MoPubErrorCode.NETWORK_NO_FILL;

            case IronSourceError.ERROR_CODE_GENERIC:
                return MoPubErrorCode.INTERNAL_ERROR;

            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;

            default:
                return MoPubErrorCode.NETWORK_TIMEOUT;
        }
    }

    /**
     * IronSource RewardedVideo Listener
     **/

    //Invoked when there is a change in the ad availability status.
    @Override
    public void onRewardedVideoAvailabilityChanged(String instanceId, boolean available) {
        onLog("onRewardedVideoAvailabilityChanged");

        //We handle callbacks only for
        if (!mInstanceId.equals(instanceId))
            return;

        if (available) {
            onRewardedVideoLoadSuccess(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
            onLog("onRewardedVideoLoadSuccess");
        } else {
            onRewardedVideoLoadFailure(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID, MoPubErrorCode.NETWORK_NO_FILL);
            onLog("onRewardedVideoLoadFailure");
        }
        onLog("onVideoAvailabilityChanged");
    }

    //Invoked when the RewardedVideo ad view has opened.
    @Override
    public void onRewardedVideoAdOpened(String instanceId) {
        onLog("onRewardedVideoAdOpened");
        onRewardedVideoStarted(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);
    }

    //Invoked when the user is about to return to the application after closing the RewardedVideo ad.
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        onLog("onRewardedVideoAdClosed, rewardName: " + rewardName + " rewardAmount: " + rewardAmount);
        onRewardedVideoClosed(IronSourceRewardedVideo.class, IRON_SOURCE_AD_NETWORK_ID);

    }

    //Invoked when the user completed the video and should be rewarded.
    @Override
    public void onRewardedVideoAdRewarded(String instanceId, Placement placement) {
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
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        onLog("onRewardedVideoShowFail");

        onRewardedVideoPlaybackError(IronSourceRewardedVideo.class,
                IRON_SOURCE_AD_NETWORK_ID,
                getMoPubErrorMessage(ironSourceError));
    }

    //Invoked when the video ad was clicked by the user.
    @Override
    public void onRewardedVideoAdClicked(String instanceId, Placement placement) {
        onLog("onRewardedVideoAdClicked");
    }
}
