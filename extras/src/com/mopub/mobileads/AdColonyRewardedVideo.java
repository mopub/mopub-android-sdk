package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jirbo.adcolony.AdColony;
import com.jirbo.adcolony.AdColonyAd;
import com.jirbo.adcolony.AdColonyAdListener;
import com.jirbo.adcolony.AdColonyV4VCAd;
import com.jirbo.adcolony.AdColonyV4VCListener;
import com.jirbo.adcolony.AdColonyV4VCReward;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A custom event for showing AdColony rewarded videos.
 *
 * Certified with AdColony 2.0.3
 */
public class AdColonyRewardedVideo extends CustomEventRewardedVideo {
    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK/wiki/API-Details#configure-activity-activity-string-client_options-string-app_id-string-zone_ids-
     */
    private static final String DEFAULT_CLIENT_OPTIONS = "version=YOUR_APP_VERSION_HERE,store:google";
    private static final String DEFAULT_APP_ID = "YOUR_AD_COLONY_APP_ID_HERE";
    private static final String[] DEFAULT_ALL_ZONE_IDS = {"ZONE_ID_1", "ZONE_ID_2", "..."};
    private static final String DEFAULT_ZONE_ID = "YOUR_CURRENT_ZONE_ID";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String CLIENT_OPTIONS_KEY = "clientOptions";
    public static final String APP_ID_KEY = "appId";
    public static final String ALL_ZONE_IDS_KEY = "allZoneIds";
    public static final String ZONE_ID_KEY = "zoneId";

    private static boolean sInitialized = false;
    private static LifecycleListener sLifecycleListener = new BaseLifecycleListener() {
        @Override
        public void onPause(@NonNull final Activity activity) {
            super.onPause(activity);
            AdColony.pause();
        }

        @Override
        public void onResume(@NonNull final Activity activity) {
            super.onResume(activity);
            AdColony.resume(activity);
        }
    };
    private static AdColonyListener sAdColonyListener = new AdColonyListener();
    private static WeakHashMap<AdColonyAd, String> sAdToZoneIdMap = new WeakHashMap<AdColonyAd, String>();

    private AdColonyV4VCAd mAd;
    private String mZoneId;
    @Nullable private String mAdUnitId;
    private boolean mIsLoading = false;

    // For waiting and notifying the SDK:
    private final Handler mHandler;
    private final ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private ScheduledFuture<?> mFuture;

    public AdColonyRewardedVideo() {
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        mHandler = new Handler();
    }

    @Nullable
    @Override
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return sAdColonyListener;
    }

    @Nullable
    @Override
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @NonNull
    @Override
    public String getAdNetworkId() {
        return mZoneId;
    }

    @Override
    protected void onInvalidate() {
        mScheduledThreadPoolExecutor.shutdownNow();
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {
        synchronized (AdColonyRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            String adColonyClientOptions = DEFAULT_CLIENT_OPTIONS;
            String adColonyAppId = DEFAULT_APP_ID;
            String[] adColonyAllZoneIds = DEFAULT_ALL_ZONE_IDS;

            // Set up serverExtras
            if (extrasAreValid(serverExtras)) {
                adColonyClientOptions = serverExtras.get(CLIENT_OPTIONS_KEY);
                adColonyAppId = serverExtras.get(APP_ID_KEY);
                adColonyAllZoneIds = extractAllZoneIds(serverExtras);
            }

            setUpGlobalSettings();
            AdColony.configure(launcherActivity, adColonyClientOptions, adColonyAppId, adColonyAllZoneIds);
            AdColony.addV4VCListener(sAdColonyListener);
            sInitialized = true;
            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull final Activity activity,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) throws Exception {

        mZoneId = DEFAULT_ZONE_ID;
        if (extrasAreValid(serverExtras)) {
            mZoneId = serverExtras.get(ZONE_ID_KEY);
        }
        Object adUnitObject = localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        if (adUnitObject != null && adUnitObject instanceof String) {
            mAdUnitId = (String) adUnitObject;
        }

        mAd = new AdColonyV4VCAd(mZoneId).withListener(sAdColonyListener);
        sAdToZoneIdMap.put(mAd, mZoneId);
        scheduleOnVideoReady();
    }

    @Override
    public boolean hasVideoAvailable() {
        return mAd != null && mAd.isReady() && mAd.getAvailableViews() != 0;
    }

    @Override
    public void showVideo() {
        if (this.hasVideoAvailable()) {
            boolean withConfirmationDialog = getConfirmationDialogFromSettings();
            boolean withResultsDialog = getResultsDialogFromSettings();
            mAd.withConfirmationDialog(withConfirmationDialog).withResultsDialog(withResultsDialog).show();
        } else {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(AdColonyRewardedVideo.class, mZoneId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        }
    }

    private boolean extrasAreValid(Map<String, String> extras) {
        return extras.containsKey(CLIENT_OPTIONS_KEY)
                && extras.containsKey(APP_ID_KEY)
                && extras.containsKey(ALL_ZONE_IDS_KEY)
                && extras.containsKey(ZONE_ID_KEY);
    }

    private String[] extractAllZoneIds(Map<String, String> serverExtras) {
        String[] result = Json.jsonArrayToStringArray(serverExtras.get(ALL_ZONE_IDS_KEY));

        // AdColony requires at least one valid String in the allZoneIds array.
        if (result.length == 0) {
            result = new String[]{""};
        }

        return result;
    }

    private void setUpGlobalSettings() {
        final AdColonyGlobalMediationSettings globalMediationSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(AdColonyGlobalMediationSettings.class);
        if (globalMediationSettings != null) {
            if (globalMediationSettings.getCustomId() != null) {
                AdColony.setCustomID(globalMediationSettings.getCustomId());
            }
            if (globalMediationSettings.getDeviceId() != null) {
                AdColony.setDeviceID(globalMediationSettings.getDeviceId());
            }
        }
    }

    private boolean getConfirmationDialogFromSettings() {
        final AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.withConfirmationDialog();
    }

    private boolean getResultsDialogFromSettings() {
        final AdColonyInstanceMediationSettings settings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(AdColonyInstanceMediationSettings.class, mAdUnitId);
        return settings != null && settings.withResultsDialog();
    }

    private void scheduleOnVideoReady() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mAd.isReady()) {
                    mIsLoading = false;
                    mScheduledThreadPoolExecutor.shutdownNow();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mAd.getAvailableViews() > 0) {
                                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(
                                        AdColonyRewardedVideo.class,
                                        mZoneId);
                            } else {
                                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                                        AdColonyRewardedVideo.class,
                                        mZoneId,
                                        MoPubErrorCode.NETWORK_NO_FILL);
                            }
                        }
                    });
                }
            }
        };

        if (!mIsLoading) {
            mScheduledThreadPoolExecutor.scheduleAtFixedRate(runnable, 1, 1, TimeUnit.SECONDS);
            mIsLoading = true;
        }
    }

    private static class AdColonyListener implements AdColonyAdListener,
            AdColonyV4VCListener, CustomEventRewardedVideoListener {

        @Override
        public void onAdColonyAdAttemptFinished(final AdColonyAd adColonyAd) {
            String zoneId = sAdToZoneIdMap.get(adColonyAd);
            MoPubRewardedVideoManager.onRewardedVideoClosed(AdColonyRewardedVideo.class, zoneId);
            if (adColonyAd.notShown()) {
                if (adColonyAd.canceled() || adColonyAd.skipped()) {
                    MoPubLog.d("User canceled ad playback");
                    return;
                }

                MoPubErrorCode reason = MoPubErrorCode.VIDEO_DOWNLOAD_ERROR;
                if (adColonyAd.noFill()) {
                    reason = MoPubErrorCode.NETWORK_NO_FILL;
                }

                MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                        AdColonyRewardedVideo.class,
                        zoneId,
                        reason);
            }
        }

        @Override
        public void onAdColonyAdStarted(final com.jirbo.adcolony.AdColonyAd adColonyAd) {
            MoPubRewardedVideoManager.onRewardedVideoStarted(
                    AdColonyRewardedVideo.class,
                    sAdToZoneIdMap.get(adColonyAd));
        }

        @Override
        public void onAdColonyV4VCReward(final AdColonyV4VCReward adColonyV4VCReward) {
            MoPubReward reward;
            if (adColonyV4VCReward.success()) {
                reward = MoPubReward.success(adColonyV4VCReward.name(), adColonyV4VCReward.amount());
            } else {
                reward = MoPubReward.failure();
            }
            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    AdColonyRewardedVideo.class,
                    null, // Can't deduce the zoneId from this object.
                    reward);
        }
    }

    public static final class AdColonyGlobalMediationSettings implements MediationSettings {

        @Nullable private final String mCustomId;
        @Nullable private final String mDeviceId;

        public AdColonyGlobalMediationSettings(@Nullable String customId, @Nullable String deviceId) {
            mCustomId = customId;
            mDeviceId = deviceId;
        }

        @Nullable
        public String getCustomId() {
            return mCustomId;
        }

        @Nullable
        public String getDeviceId() {
            return mDeviceId;
        }
    }

    public static final class AdColonyInstanceMediationSettings implements MediationSettings {
        private final boolean mWithConfirmationDialog;
        private final boolean mWithResultsDialog;

        public AdColonyInstanceMediationSettings(
                boolean withConfirmationDialog, boolean withResultsDialog) {
            mWithConfirmationDialog = withConfirmationDialog;
            mWithResultsDialog = withResultsDialog;
        }

        public boolean withConfirmationDialog() {
            return mWithConfirmationDialog;
        }

        public boolean withResultsDialog() {
            return mWithResultsDialog;
        }
    }
}
