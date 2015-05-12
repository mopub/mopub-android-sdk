package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.*;

/**
 * A custom event for showing Chartboost rewarded videos.
 *
 * Certified with Chartboost 5.0.4
 */
public class ChartboostRewardedVideo extends CustomEventRewardedVideo {
    public static final String APP_ID_KEY = "appId";
    public static final String APP_SIGNATURE_KEY = "appSignature";
    public static final String LOCATION_KEY = "location";
    public static final String LOCATION_DEFAULT = "Default";

    @NonNull private static final LifecycleListener sLifecycleListener =
            new ChartboostLifecycleListener();
    private static boolean sInitialized = false;

    @NonNull private String mLocation = LOCATION_DEFAULT;
    @NonNull private final Handler mHandler;

    public ChartboostRewardedVideo() {
        mHandler = new Handler();
    }

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return SingletonChartboostDelegate.instance;
    }

    @Override
    @NonNull
    public LifecycleListener getLifecycleListener() {
        return sLifecycleListener;
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        return mLocation;
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) throws Exception {
        synchronized (ChartboostRewardedVideo.class) {
            if (sInitialized) {
                return false;
            }

            if (!serverExtras.containsKey(APP_ID_KEY)) {
                throw new IllegalStateException("Chartboost rewarded video initialization" +
                        " failed due to missing application ID.");
            }

            if (!serverExtras.containsKey(APP_SIGNATURE_KEY)) {
                throw new IllegalStateException("Chartboost rewarded video initialization" +
                        " failed due to missing application signature.");
            }

            final String appId = serverExtras.get(APP_ID_KEY);
            final String appSignature = serverExtras.get(APP_SIGNATURE_KEY);

            Chartboost.startWithAppId(launcherActivity, appId, appSignature);
            Chartboost.setFramework(Chartboost.CBFramework.CBFrameworkMoPub);
            Chartboost.setImpressionsUseActivities(false);
            Chartboost.setDelegate((SingletonChartboostDelegate) getVideoListenerForSdk());
            Chartboost.setShouldRequestInterstitialsInFirstSession(true);
            Chartboost.onCreate(launcherActivity);
            Chartboost.onStart(launcherActivity);

            sInitialized = true;
            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {
        if (serverExtras.containsKey(LOCATION_KEY)) {
            mLocation = serverExtras.get(LOCATION_KEY);
        } else {
            mLocation = LOCATION_DEFAULT;
        }

        ((SingletonChartboostDelegate) getVideoListenerForSdk()).getRewardedLocationsToLoad().add(mLocation);
        setUpMediationSettingsForRequest((String) localExtras.get(DataKeys.AD_UNIT_ID_KEY));

        // We do this to ensure that the custom event manager has a chance to get the listener
        // and ad unit ID before and delegate callbacks are made.
        mHandler.post(new Runnable() {
            public void run() {
                Chartboost.cacheRewardedVideo(mLocation);
            }
        });
    }

    private void setUpMediationSettingsForRequest(String moPubId) {
        final ChartboostMediationSettings globalSettings =
                MoPubRewardedVideoManager.getGlobalMediationSettings(ChartboostMediationSettings.class);
        final ChartboostMediationSettings instanceSettings =
                MoPubRewardedVideoManager.getInstanceMediationSettings(ChartboostMediationSettings.class, moPubId);

        // Instance settings override global settings.
        if (instanceSettings != null) {
            Chartboost.setCustomId(instanceSettings.getCustomId());
        } else if (globalSettings != null) {
            Chartboost.setCustomId(globalSettings.getCustomId());
        }
    }

    @Override
    public boolean hasVideoAvailable() {
        return Chartboost.hasRewardedVideo(mLocation);
    }

    @Override
    public void showVideo() {
        if (hasVideoAvailable()) {
            Chartboost.showRewardedVideo(mLocation);
        } else {
            MoPubLog.d("Attempted to show Chartboost rewarded video before it was available.");
        }
    }

    @Override
    protected void onInvalidate() {
        // This prevents sending didCache or didFailToCache callbacks.
        ((SingletonChartboostDelegate)getVideoListenerForSdk()).getRewardedLocationsToLoad().remove(mLocation);
    }

    private static final class ChartboostLifecycleListener implements LifecycleListener {
        @Override
        public void onCreate(@NonNull Activity activity) {
            Chartboost.onCreate(activity);
        }

        @Override
        public void onStart(@NonNull Activity activity) {
            Chartboost.onStart(activity);
        }

        @Override
        public void onPause(@NonNull Activity activity) {
            Chartboost.onPause(activity);
        }

        @Override
        public void onResume(@NonNull Activity activity) {
            Chartboost.onResume(activity);
        }

        @Override
        public void onRestart(@NonNull Activity activity) {
        }

        @Override
        public void onStop(@NonNull Activity activity) {
            Chartboost.onStop(activity);
        }

        @Override
        public void onDestroy(@NonNull Activity activity) {
            Chartboost.onDestroy(activity);
        }

        @Override
        public void onBackPressed(@NonNull Activity activity) {
            Chartboost.onBackPressed();
        }
    }

    public static final class ChartboostMediationSettings implements MediationSettings {
        @NonNull private final String mCustomId;

        public ChartboostMediationSettings(@NonNull final String customId) {
            mCustomId = customId;
        }

        @NonNull public String getCustomId() {
            return mCustomId;
        }
    }

    @Deprecated // for testing
    @VisibleForTesting
    @SuppressWarnings("unused")
    static void resetInitialization() {
        sInitialized = false;
    }
}
