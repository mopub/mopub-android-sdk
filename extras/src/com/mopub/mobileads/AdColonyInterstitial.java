package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.jirbo.adcolony.AdColony;
import com.jirbo.adcolony.AdColonyAd;
import com.jirbo.adcolony.AdColonyAdListener;
import com.jirbo.adcolony.AdColonyVideoAd;
import com.mopub.common.util.Json;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * Tested with AdColony SDK 2.0.3.
 */
public class AdColonyInterstitial extends CustomEventInterstitial implements AdColonyAdListener {
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

    private static boolean isAdColonyConfigured = false;

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private final Handler mHandler;
    private AdColonyVideoAd mAdColonyVideoAd;
    private final ScheduledThreadPoolExecutor mScheduledThreadPoolExecutor;
    private boolean mIsLoading;

    public AdColonyInterstitial() {
        mScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        mHandler = new Handler();
    }

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            customEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        String clientOptions = DEFAULT_CLIENT_OPTIONS;
        String appId = DEFAULT_APP_ID;
        String[] allZoneIds = DEFAULT_ALL_ZONE_IDS;
        String zoneId = DEFAULT_ZONE_ID;

        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (extrasAreValid(serverExtras)) {
            clientOptions = serverExtras.get(CLIENT_OPTIONS_KEY);
            appId = serverExtras.get(APP_ID_KEY);
            allZoneIds = extractAllZoneIds(serverExtras);
            zoneId = serverExtras.get(ZONE_ID_KEY);
        }

        if (!isAdColonyConfigured) {
            AdColony.configure((Activity)context, clientOptions, appId, allZoneIds);
            isAdColonyConfigured = true;
        }

        mAdColonyVideoAd = new AdColonyVideoAd(zoneId);
        mAdColonyVideoAd.withListener(this);

        scheduleOnInterstitialLoaded();
    }

    @Override
    protected void showInterstitial() {
        if (mAdColonyVideoAd.isReady()) {
            mAdColonyVideoAd.show();
        } else {
            Log.d("MoPub", "Tried to show a AdColony interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyVideoAd != null) {
            mAdColonyVideoAd.withListener(null);
        }

        mScheduledThreadPoolExecutor.shutdownNow();
        mIsLoading = false;
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

    private void scheduleOnInterstitialLoaded() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mAdColonyVideoAd.isReady()) {
                    Log.d("MoPub", "AdColony interstitial ad successfully loaded.");
                    mIsLoading = false;
                    mScheduledThreadPoolExecutor.shutdownNow();
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialLoaded();
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

    /*
     * AdColonyAdListener implementation
     */

    @Override
    public void onAdColonyAdStarted(AdColonyAd adColonyAd) {
        Log.d("MoPub", "AdColony interstitial ad shown.");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCustomEventInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onAdColonyAdAttemptFinished(AdColonyAd adColonyAd) {
        Log.d("MoPub", "AdColony interstitial ad dismissed.");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCustomEventInterstitialListener.onInterstitialDismissed();
            }
        });
    }

    @Deprecated // for testing
    ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return mScheduledThreadPoolExecutor;
    }

    @Deprecated // for testing
    void resetAdColonyConfigured() {
        isAdColonyConfigured = false;
    }
}
