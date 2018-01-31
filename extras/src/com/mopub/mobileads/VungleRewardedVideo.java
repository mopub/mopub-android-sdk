package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import com.vungle.publisher.AdConfig;

import java.util.Map;

/**
 * A custom event for showing Vungle rewarded videos.
 *
 * Certified with Vungle SDK 5.3.0
 */
public class VungleRewardedVideo extends CustomEventRewardedVideo {

    private static final String REWARDED_TAG = "Vungle Rewarded: ";

    /*
     * These constants are intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String PLACEMENT_ID_KEY = "pid";
    public static final String PLACEMENT_IDS_KEY = "pids";

    public static final String VUNGLE_NETWORK_ID_DEFAULT = "vngl_id";
    private static final String VUNGLE_DEFAULT_APP_ID = "YOUR_APP_ID_HERE";
    private static final String[] VUNGLE_DEFAULT_ALL_PLACEMENT_IDS = {"PLACEMENT_ID_1", "PLACEMENT_ID_2", "..."};

    private static VungleRouter sVungleRouter;
    private VungleRewardedRouterListener mVungleRewardedRouterListener;
    private static boolean sInitialized;
    private String mAppId;
    private String mPlacementId;
    private String[] mPlacementIds;
    private boolean mIsPlaying;

    private String mAdUnitId;
    private String mCustomerId;


    public VungleRewardedVideo() {
        sVungleRouter = VungleRouter.getInstance();

        if (mVungleRewardedRouterListener == null) {
            mVungleRewardedRouterListener = new VungleRewardedRouterListener();
        }
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sVungleRouter.getLifecycleListener();
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (VungleRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            if (!validateIdsInServerExtras(serverExtras)) {
                mAppId = VUNGLE_DEFAULT_APP_ID;
                mPlacementIds = VUNGLE_DEFAULT_ALL_PLACEMENT_IDS;
            }

            if (!sVungleRouter.isVungleInitialized()) {
                sVungleRouter.initVungle(launcherActivity, mAppId, mPlacementIds);
            }

            sInitialized = true;

            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity, @NonNull final Map<String, Object> localExtras, @NonNull final Map<String, String> serverExtras) throws Exception {
        mIsPlaying = false;

        if (!validateIdsInServerExtras(serverExtras)) {
            mPlacementId = VUNGLE_NETWORK_ID_DEFAULT;
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);

            return;
        }

        Object adUnitObject = localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        if (adUnitObject instanceof String) {
            mAdUnitId = (String) adUnitObject;
        }

        Object customerIdObject = localExtras.get(DataKeys.REWARDED_AD_CUSTOMER_ID_KEY);
        if (customerIdObject instanceof String && !TextUtils.isEmpty((String) customerIdObject)) {
            mCustomerId = (String) customerIdObject;
        }

        if (sVungleRouter.isVungleInitialized()) {
            sVungleRouter.loadAdForPlacement(mPlacementId, mVungleRewardedRouterListener);
        }
        else {
            MoPubLog.d(REWARDED_TAG + "There should not be this case. loadWithSdkInitialized is called before the SDK starts initialization for Placement ID: " + mPlacementId);
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return sVungleRouter.isAdPlayableForPlacement(mPlacementId);
    }

    @Override
    protected void showVideo() {
        final AdConfig adConfig = new AdConfig();
        setUpMediationSettingsForRequest(adConfig);

        sVungleRouter.playAdForPlacement(mPlacementId, adConfig);
        mIsPlaying = true;
    }

    @Override
    protected void onInvalidate() {
        MoPubLog.d(REWARDED_TAG + "onInvalidate is called for Placement ID:" + mPlacementId);

        sVungleRouter.removeRouterListener(mPlacementId);
        mVungleRewardedRouterListener = null;
    }


    //private functions
    private boolean validateIdsInServerExtras (Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId.isEmpty()) {
                MoPubLog.w(REWARDED_TAG + "App ID is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(REWARDED_TAG + "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId.isEmpty()) {
                MoPubLog.w(REWARDED_TAG + "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(REWARDED_TAG + "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            mPlacementIds = serverExtras.get(PLACEMENT_IDS_KEY).replace(" ", "").split(",", 0);
            if (mPlacementIds.length == 0) {
                MoPubLog.w(REWARDED_TAG + "Placement IDs are empty.");
                isAllDataValid = false;
            }
        } else {
            MoPubLog.w(REWARDED_TAG + "Placement IDs for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (isAllDataValid) {
            boolean foundInList = false;
            for (String pid:  mPlacementIds) {
                if(pid.equals(mPlacementId)) {
                    foundInList = true;
                }
            }
            if(!foundInList) {
                MoPubLog.w(REWARDED_TAG + "Placement IDs for this Ad Unit is not in the array of Placement IDs");
                isAllDataValid = false;
            }
        }

        return isAllDataValid;
    }

    private void setUpMediationSettingsForRequest(AdConfig adConfig) {
        final VungleMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(VungleMediationSettings.class);
        final VungleMediationSettings instanceMediationSettings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(VungleMediationSettings.class, mAdUnitId);

        // Local options override global options.
        // The two objects are not merged.
        if (instanceMediationSettings != null) {
            modifyAdConfig(adConfig, instanceMediationSettings);
        } else if (globalMediationSettings != null) {
            modifyAdConfig(adConfig, globalMediationSettings);
        }
    }

    private void modifyAdConfig(AdConfig adConfig, VungleMediationSettings mediationSettings) {
        if (!TextUtils.isEmpty(mediationSettings.body)) {
            adConfig.setIncentivizedCancelDialogBodyText(mediationSettings.body);
        }
        if (!TextUtils.isEmpty(mediationSettings.closeButtonText)) {
            adConfig.setIncentivizedCancelDialogCloseButtonText(mediationSettings.closeButtonText);
        }
        if (!TextUtils.isEmpty(mediationSettings.keepWatchingButtonText)) {
            adConfig.setIncentivizedCancelDialogKeepWatchingButtonText(mediationSettings.keepWatchingButtonText);
        }
        if (!TextUtils.isEmpty(mediationSettings.title)) {
            adConfig.setIncentivizedCancelDialogTitle(mediationSettings.title);
        }
        if (!TextUtils.isEmpty(mCustomerId)) {
            adConfig.setIncentivizedUserId(mCustomerId);
        } else if (!TextUtils.isEmpty(mediationSettings.userId)) {
            adConfig.setIncentivizedUserId(mediationSettings.userId);
        }
    }


    /*
     * VungleRewardedRouterListener
     */
    private class VungleRewardedRouterListener implements VungleRouterListener {
        @Override
        public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(REWARDED_TAG + "onAdEnd - Placement ID: " + placementReferenceId + ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: " + wasCallToActionClicked);

                mIsPlaying = false;

                if (wasSuccessfulView) {
                    // Vungle does not provide a callback when a user should be rewarded.
                    // You will need to provide your own reward logic if you receive a reward with
                    // "NO_REWARD_LABEL" && "NO_REWARD_AMOUNT"
                    MoPubRewardedVideoManager.onRewardedVideoCompleted(VungleRewardedVideo.class,
                            mPlacementId,
                            MoPubReward.success(MoPubReward.NO_REWARD_LABEL,
                                    MoPubReward.NO_REWARD_AMOUNT));
                }

                if (wasCallToActionClicked) {
                    MoPubRewardedVideoManager.onRewardedVideoClicked(VungleRewardedVideo.class,
                            mPlacementId);
                }

                MoPubRewardedVideoManager.onRewardedVideoClosed(VungleRewardedVideo.class,
                        mPlacementId);

                sVungleRouter.removeRouterListener(mPlacementId);
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(REWARDED_TAG + "onAdStart - Placement ID: " + placementReferenceId);

                mIsPlaying = true;

                MoPubRewardedVideoManager.onRewardedVideoStarted(VungleRewardedVideo.class,
                        mPlacementId);
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            if (mPlacementId.equals(placementReferenceId)) {
                MoPubLog.d(REWARDED_TAG + "onUnableToPlayAd - Placement ID: " + placementReferenceId + ", reason: " + reason);

                mIsPlaying = false;
                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class,
                        mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
            }
        }

        @Override
        public void onAdAvailabilityUpdate(@NonNull String placementReferenceId, boolean isAdAvailable) {
            if (mPlacementId.equals(placementReferenceId)) {
                if (!mIsPlaying) {
                    if (isAdAvailable) {
                        MoPubLog.d(REWARDED_TAG + "rewarded video ad successfully loaded - Placement ID: " + placementReferenceId);
                        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(VungleRewardedVideo.class,
                                mPlacementId);
                    }
                    else {
                        MoPubLog.d(REWARDED_TAG + "rewarded video ad is not loaded - Placement ID: " + placementReferenceId);
                        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class,
                                mPlacementId, MoPubErrorCode.NETWORK_NO_FILL);
                    }
                }
            }
        }
    }


    public static class VungleMediationSettings implements MediationSettings {
        @Nullable private final String userId;
        @Nullable private final String title;
        @Nullable private final String body;
        @Nullable private final String closeButtonText;
        @Nullable private final String keepWatchingButtonText;

        public static class Builder {
            @Nullable private String userId;
            @Nullable private String title;
            @Nullable private String body;
            @Nullable private String closeButtonText;
            @Nullable private String keepWatchingButtonText;

            public Builder withUserId(@NonNull final String userId) {
                this.userId = userId;
                return this;
            }

            public Builder withCancelDialogTitle(@NonNull final String title) {
                this.title = title;
                return this;
            }

            public Builder withCancelDialogBody(@NonNull final String body) {
                this.body = body;
                return this;
            }

            public Builder withCancelDialogCloseButton(@NonNull final String buttonText) {
                this.closeButtonText = buttonText;
                return this;
            }

            public Builder withCancelDialogKeepWatchingButton(@NonNull final String buttonText) {
                this.keepWatchingButtonText = buttonText;
                return this;
            }

            public VungleMediationSettings build() {
                return new VungleMediationSettings(this);
            }
        }

        private VungleMediationSettings(@NonNull final Builder builder) {
            this.userId = builder.userId;
            this.title = builder.title;
            this.body = builder.body;
            this.closeButtonText = builder.closeButtonText;
            this.keepWatchingButtonText = builder.keepWatchingButtonText;
        }
    }
}
