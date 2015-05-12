package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.VisibleForTesting;

import java.util.Map;

/*
 * Tested with Chartboost SDK 5.0.4.
 */
class ChartboostInterstitial extends CustomEventInterstitial {
    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";
    public static final String APP_SIGNATURE_KEY = "appSignature";
    public static final String LOCATION_KEY = "location";
    public static final String LOCATION_DEFAULT = "Default";

    private String appId;
    private String appSignature;
    private String location;

    /*
     * Note: Chartboost recommends implementing their specific Activity lifecycle callbacks in your
     * Activity's onStart(), onStop(), onBackPressed() methods for proper results. Please see their
     * documentation for more information.
     */

    ChartboostInterstitial() {
        location = LOCATION_DEFAULT;
    }

    static SingletonChartboostDelegate getDelegate() {
        return SingletonChartboostDelegate.instance;
    }

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener interstitialListener,
                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (!(context instanceof Activity)) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (extrasAreValid(serverExtras)) {
            setAppId(serverExtras.get(APP_ID_KEY));
            setAppSignature(serverExtras.get(APP_SIGNATURE_KEY));
            setLocation(
                    serverExtras.containsKey(LOCATION_KEY)
                            ? serverExtras.get(LOCATION_KEY)
                            : LOCATION_DEFAULT);
        } else {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        // Set the listener.
        getDelegate().registerListener(location, interstitialListener);

        // Start Chartboost and get an ad.
        Activity activity = (Activity) context;

        Chartboost.startWithAppId(activity, appId, appSignature);
        Chartboost.setFramework(Chartboost.CBFramework.CBFrameworkMoPub);
        Chartboost.setImpressionsUseActivities(false);
        Chartboost.setDelegate(getDelegate());
        Chartboost.setShouldRequestInterstitialsInFirstSession(true);
        Chartboost.onCreate(activity);
        Chartboost.onStart(activity);
        Chartboost.cacheInterstitial(location);
    }

    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing Chartboost interstitial ad.");
        Chartboost.showInterstitial(location);
    }

    @Override
    protected void onInvalidate() {
        getDelegate().unregisterListener(location);
    }

    private void setAppId(String appId) {
        this.appId = appId;
    }

    private void setAppSignature(String appSignature) {
        this.appSignature = appSignature;
    }

    private void setLocation(String location) {
        this.location = location;
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APP_ID_KEY) && serverExtras.containsKey(APP_SIGNATURE_KEY);
    }

    @VisibleForTesting
    @Deprecated
    @SuppressWarnings("unused")
    public static void resetDelegate() {
        SingletonChartboostDelegate.instance = new SingletonChartboostDelegate();
    }

}
