package com.mopub.mobileads;

import android.app.Activity;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

/**
 * A custom event for showing Chartboost rewarded videos.
 *
 * Certified with Chartboost 7.0.1
 */
public class ChartboostRewardedVideo extends CustomEventRewardedVideo {
    @NonNull private static final LifecycleListener sLifecycleListener =
            new ChartboostLifecycleListener();

    @NonNull private String mLocation = ChartboostShared.LOCATION_DEFAULT;
    @NonNull private final Handler mHandler;

    public ChartboostRewardedVideo() {
        mHandler = new Handler();
    }

    @Override
    @NonNull
    public CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return ChartboostShared.getDelegate();
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
        // We need to attempt to reinitialize Chartboost on each request, in case an interstitial has been
        // loaded and used since then.
        ChartboostShared.initializeSdk(launcherActivity, serverExtras);  // throws IllegalStateException

        // Always return true so that the lifecycle listener is registered even if an interstitial
        // did the initialization.
        return true;
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras)
            throws Exception {

        if (serverExtras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = serverExtras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        ChartboostShared.getDelegate().registerRewardedVideoLocation(mLocation);
        setUpMediationSettingsForRequest((String) localExtras.get(DataKeys.AD_UNIT_ID_KEY));

        // We do this to ensure that the custom event manager has a chance to get the listener
        // and ad unit ID before any delegate callbacks are made.
        mHandler.post(new Runnable() {
            public void run() {
                if (Chartboost.hasRewardedVideo(mLocation)) {
                    ChartboostShared.getDelegate().didCacheRewardedVideo(mLocation);
                } else {
                    Chartboost.cacheRewardedVideo(mLocation);
                }
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
        ChartboostShared.getDelegate().unregisterRewardedVideoLocation(mLocation);
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

        @NonNull
        public String getCustomId() {
            return mCustomId;
        }
    }
}
