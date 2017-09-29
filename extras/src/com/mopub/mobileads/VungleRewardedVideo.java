package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;

import com.vungle.publisher.AdConfig;
import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.env.WrapperFramework;
import com.vungle.publisher.inject.Injector;

import java.util.Map;

/**
 * A custom event for showing Vungle rewarded videos.
 *
 * Certified with Vungle SDK 5.1.0
 */
public class VungleRewardedVideo extends CustomEventRewardedVideo {

    private static final String MAIN_TAG = "MoPub";
    private static final String SUB_TAG = "Vungle Rewarded: ";

    /*
     * These constants are intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String PLACEMENT_ID_KEY = "pid";
    public static final String PLACEMENT_IDS_KEY = "pids";

    public static final String VUNGLE_NETWORK_ID_DEFAULT = "vngl_id";

    // Version of the adapter, intended for Vungle internal use.
    private static final String VERSION = "5.1.0";


    private static VunglePub sVunglePub;
    private VungleRewardedVideoListener mVungleRewardedVideoListener;
    private static boolean sInitialized;

    private String mAppId;
    private String mPlacementId;
    private String[] mPlacementIds;

    private boolean mIsPlaying;


    private static final LifecycleListener sLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull final Activity activity) {
            super.onPause(activity);
            sVunglePub.onPause();
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
            sVunglePub.onResume();
        }
    };

    private String mAdUnitId;
    private String mCustomerId;


    public VungleRewardedVideo() {
        mVungleRewardedVideoListener = new VungleRewardedVideoListener();
    }

    @Nullable
    @Override
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return mVungleRewardedVideoListener;
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
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
            if (!sInitialized) {
                Injector injector = Injector.getInstance();
                injector.setWrapperFramework(WrapperFramework.mopub);
                injector.setWrapperFrameworkVersion(VERSION.replace('.', '_'));
                sVunglePub = VunglePub.getInstance();
                String appId = serverExtras.containsKey(APP_ID_KEY) ? serverExtras.get(APP_ID_KEY) : DEFAULT_VUNGLE_APP_ID;
                sVunglePub.init(launcherActivity, appId);
                sInitialized = true;
                return true;
            }
            return false;
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

        if (!sVunglePub.isInitialized()) {
            sVunglePub.init(activity, mAppId, mPlacementIds, new VungleInitListener() {
                @Override
                public void onSuccess() {
                    Log.d(MAIN_TAG, SUB_TAG + "SDK is initialized successfully.");

                    sVunglePub.addEventListeners(mVungleRewardedVideoListener);
                    sVunglePub.loadAd(mPlacementId);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.w(MAIN_TAG, SUB_TAG + "Initialization is failed.");
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(VungleRewardedVideo.class, mPlacementId, MoPubErrorCode.INTERNAL_ERROR);
                }
            });
        }
        else {
            sVunglePub.addEventListeners(mVungleRewardedVideoListener);
            sVunglePub.loadAd(mPlacementId);
        }

        Object adUnitObject = localExtras.get(DataKeys.AD_UNIT_ID_KEY);

        if (adUnitObject instanceof String) {
            mAdUnitId = (String) adUnitObject;
        }

        Object customerIdObject = localExtras.get(DataKeys.REWARDED_VIDEO_CUSTOMER_ID);
        if (customerIdObject instanceof String && !TextUtils.isEmpty((String) customerIdObject)) {
            mCustomerId = (String) customerIdObject;
        }
    }

    @Override
    protected boolean hasVideoAvailable() {
        return sVunglePub.isAdPlayable(mPlacementId);
    }

    @Override
    protected void showVideo() {
        final AdConfig adConfig = new AdConfig();
        setUpMediationSettingsForRequest(adConfig);
        sVunglePub.playAd(mPlacementId, adConfig);

        mIsPlaying = true;
    }

    @Override
    protected void onInvalidate() {
        sVunglePub.removeEventListeners(mVungleRewardedVideoListener);
        mVungleRewardedVideoListener = null;

        Log.d(MAIN_TAG, SUB_TAG + "onInvalidate is called for Placement ID:" + mPlacementId);
    }


    //private functions
    private boolean validateIdsInServerExtras (Map<String, String> serverExtras) {
        boolean isAllDataValid = true;

        if (serverExtras.containsKey(APP_ID_KEY)) {
            mAppId = serverExtras.get(APP_ID_KEY);
            if (mAppId.isEmpty()) {
                Log.w(MAIN_TAG, SUB_TAG + "App ID is empty.");
                isAllDataValid = false;
            }
        } else {
            Log.w(MAIN_TAG, SUB_TAG + "AppID is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
            mPlacementId = serverExtras.get(PLACEMENT_ID_KEY);
            if (mPlacementId.isEmpty()) {
                Log.w(MAIN_TAG, SUB_TAG + "Placement ID for this Ad Unit is empty.");
                isAllDataValid = false;
            }
        } else {
            Log.w(MAIN_TAG, SUB_TAG + "Placement ID for this Ad Unit is not in serverExtras.");
            isAllDataValid = false;
        }

        if (serverExtras.containsKey(PLACEMENT_IDS_KEY)) {
            mPlacementIds = serverExtras.get(PLACEMENT_IDS_KEY).replace(" ", "").split(",", 0);
            if (mPlacementIds.length == 0) {
                Log.w(MAIN_TAG, SUB_TAG + "Placement IDs are empty.");
                isAllDataValid = false;
            }
        } else {
            Log.w(MAIN_TAG, SUB_TAG + "Placement IDs for this Ad Unit is not in serverExtras.");
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
                Log.w(MAIN_TAG, SUB_TAG + "Placement IDs for this Ad Unit is not in the array of Placement IDs");
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
     * VungleAdEventListener
     */
    private class VungleRewardedVideoListener implements VungleAdEventListener, CustomEventRewardedVideoListener {
        @Override
        public void onAdEnd(@NonNull String placementReferenceId, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
            if (mPlacementId.equals(placementReferenceId)) {
                Log.d(MAIN_TAG, SUB_TAG + "onAdEnd - Placement ID: " + placementReferenceId + ", wasSuccessfulView: " + wasSuccessfulView + ", wasCallToActionClicked: " + wasCallToActionClicked);

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
            }
        }

        @Override
        public void onAdStart(@NonNull String placementReferenceId) {
            if (mPlacementId.equals(placementReferenceId)) {
                Log.d(MAIN_TAG, SUB_TAG + "onAdStart - Placement ID: " + placementReferenceId);

                mIsPlaying = true;

                MoPubRewardedVideoManager.onRewardedVideoStarted(VungleRewardedVideo.class,
                        mPlacementId);
            }
        }

        @Override
        public void onUnableToPlayAd(@NonNull String placementReferenceId, String reason) {
            if (mPlacementId.equals(placementReferenceId)) {
                Log.d(MAIN_TAG, SUB_TAG + "onUnableToPlayAd - Placement ID: " + placementReferenceId + ", reason: " + reason);

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
                        Log.d(MAIN_TAG, SUB_TAG + "rewarded video ad successfully loaded - Placement ID: " + placementReferenceId);
                        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(VungleRewardedVideo.class,
                                mPlacementId);
                    }
                    else {
                        Log.d(MAIN_TAG, SUB_TAG + "rewarded video ad is not loaded - Placement ID: " + placementReferenceId);
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
