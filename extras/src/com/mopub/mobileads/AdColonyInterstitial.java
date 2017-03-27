package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyZone;
import com.mopub.common.util.Json;

import java.util.Map;

public class AdColonyInterstitial extends CustomEventInterstitial {
    private static final String TAG = "AdColonyInterstitial";
    /*
     * We recommend passing the AdColony client options, app ID, all zone IDs, and current zone ID
     * in the serverExtras Map by specifying Custom Event Data in MoPub's web interface.
     *
     * Please see AdColony's documentation for more information:
     * https://github.com/AdColony/AdColony-Android-SDK-3
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

    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private AdColonyInterstitialListener mAdColonyInterstitialListener;
    private final Handler mHandler;
    private com.adcolony.sdk.AdColonyInterstitial mAdColonyInterstitial;

    public AdColonyInterstitial() {
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

        mAdColonyInterstitialListener = getAdColonyInterstitialListener();
        if (!isAdColonyConfigured()) {
            AdColony.configure((Activity) context, getAppOptions(clientOptions), appId, allZoneIds);
        }

        AdColony.requestInterstitial(zoneId, mAdColonyInterstitialListener);
    }

    @Override
    protected void showInterstitial() {
        if (mAdColonyInterstitial == null || mAdColonyInterstitial.isExpired()) {
            Log.e(TAG, "AdColony interstitial ad is null or has expired");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }
            });
        } else {
            mAdColonyInterstitial.show();
        }
    }

    @Override
    protected void onInvalidate() {
        if (mAdColonyInterstitial != null) {
            mAdColonyInterstitialListener = null;
            mAdColonyInterstitial.setListener(null);
            mAdColonyInterstitial.destroy();
            mAdColonyInterstitial = null;
        }
    }

    private AdColonyAppOptions getAppOptions(String clientOptions) {
        if (clientOptions == null || clientOptions.isEmpty()) {
            return null;
        }
        AdColonyAppOptions adColonyAppOptions = new AdColonyAppOptions();
        String[] allOptions = clientOptions.split(",");
        for (String option : allOptions) {
            String optionNameAndValue[] = option.split(":");
            if (optionNameAndValue.length == 2) {
                switch (optionNameAndValue[0]) {
                    case "store":
                        adColonyAppOptions.setOriginStore(optionNameAndValue[1]);
                        break;
                    case "version":
                        adColonyAppOptions.setAppVersion(optionNameAndValue[1]);
                        break;
                    default:
                        Log.e(TAG, "AdColony client options in wrong format - please check your MoPub dashboard");
                        return null;
                }
            } else {
                Log.e(TAG, "AdColony client options in wrong format - please check your MoPub dashboard");
                return null;
            }
        }

        return adColonyAppOptions;
    }

    private boolean isAdColonyConfigured() {
        return !AdColony.getSDKVersion().isEmpty();
    }

    private AdColonyInterstitialListener getAdColonyInterstitialListener() {
        if (mAdColonyInterstitialListener != null) {
            return mAdColonyInterstitialListener;
        } else {
            return new AdColonyInterstitialListener() {
                @Override
                public void onRequestFilled(com.adcolony.sdk.AdColonyInterstitial adColonyInterstitial) {
                    mAdColonyInterstitial = adColonyInterstitial;
                    Log.d(TAG, "AdColony interstitial ad has been successfully loaded.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialLoaded();
                        }
                    });
                }

                @Override
                public void onRequestNotFilled(AdColonyZone zone) {
                    Log.d(TAG, "AdColony interstitial ad has no fill.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                        }
                    });
                }

                @Override
                public void onClosed(com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad has been dismissed.");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialDismissed();
                        }
                    });
                }

                @Override
                public void onOpened(com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad shown: " + ad.getZoneID());
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onInterstitialShown();
                        }
                    });
                }

                @Override
                public void onExpiring(com.adcolony.sdk.AdColonyInterstitial ad) {
                    Log.d(TAG, "AdColony interstitial ad is expiring; requesting new ad");
                    AdColony.requestInterstitial(ad.getZoneID(), mAdColonyInterstitialListener);
                }

                @Override
                public void onLeftApplication(com.adcolony.sdk.AdColonyInterstitial ad) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCustomEventInterstitialListener.onLeaveApplication();
                        }
                    });
                }

                @Override
                public void onClicked(com.adcolony.sdk.AdColonyInterstitial ad) {
                    mCustomEventInterstitialListener.onInterstitialClicked();
                }
            };
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

    @Deprecated
    // For testing
    public static String getAdUnitId(MoPubInterstitial interstitial) {
        return interstitial.getMoPubInterstitialView().getAdUnitId();
    }
}
