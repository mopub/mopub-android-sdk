package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.chartboost.sdk.Chartboost;
import com.chartboost.sdk.ChartboostDelegate;

import java.util.HashMap;
import java.util.Map;

/*
 * Tested with Chartboost SDK 3.1.5.
 */
class ChartboostInterstitial extends CustomEventInterstitial {
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

    public ChartboostInterstitial() {
        location = LOCATION_DEFAULT;
    }

    static SingletonChartboostDelegate getDelegate() {
        return SingletonChartboostDelegate.instance;
    }

    @Deprecated // for test only
    public static void resetDelegate() {
        SingletonChartboostDelegate.instance = new SingletonChartboostDelegate();
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

        Activity activity = (Activity) context;
        Chartboost chartboost = Chartboost.sharedChartboost();

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

        if (getDelegate().hasLocation(location) &&
                getDelegate().getListener(location) != interstitialListener) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        getDelegate().registerListener(location, interstitialListener);
        chartboost.onCreate(activity, appId, appSignature, getDelegate());
        chartboost.onStart(activity);

        chartboost.cacheInterstitial(location);
    }

    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing Chartboost interstitial ad.");
        Chartboost.sharedChartboost().showInterstitial(location);
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

    private static class SingletonChartboostDelegate implements ChartboostDelegate {
        private static final CustomEventInterstitialListener NULL_LISTENER = new CustomEventInterstitialListener() {
            @Override public void onInterstitialLoaded() { }
            @Override public void onInterstitialFailed(MoPubErrorCode errorCode) { }
            @Override public void onInterstitialShown() { }
            @Override public void onInterstitialClicked() { }
            @Override public void onLeaveApplication() { }
            @Override public void onInterstitialDismissed() { }
        };
        static SingletonChartboostDelegate instance = new SingletonChartboostDelegate();
        private Map<String, CustomEventInterstitialListener> listenerForLocation =
                new HashMap<String, CustomEventInterstitialListener>();

        public void registerListener(String location, CustomEventInterstitialListener interstitialListener) {
            listenerForLocation.put(location, interstitialListener);
        }

        public void unregisterListener(String location) {
            listenerForLocation.remove(location);
        }

        public boolean hasLocation(String location) {
            return listenerForLocation.containsKey(location);
        }

        /*
         * Interstitial delegate methods
         */
        @Override
        public boolean shouldDisplayInterstitial(String location) {
            return true;
        }

        @Override
        public boolean shouldRequestInterstitial(String location) {
            return true;
        }

        @Override
        public boolean shouldRequestInterstitialsInFirstSession() {
            return true;
        }

        @Override
        public void didCacheInterstitial(String location) {
            Log.d("MoPub", "Chartboost interstitial loaded successfully.");
            getListener(location).onInterstitialLoaded();
        }

        @Override
        public void didFailToLoadInterstitial(String location) {
            Log.d("MoPub", "Chartboost interstitial ad failed to load.");
            getListener(location).onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }

        @Override
        public void didDismissInterstitial(String location) {
            // Note that this method is fired before didCloseInterstitial and didClickInterstitial.
            Log.d("MoPub", "Chartboost interstitial ad dismissed.");
            getListener(location).onInterstitialDismissed();
        }

        @Override
        public void didCloseInterstitial(String location) {
        }

        @Override
        public void didClickInterstitial(String location) {
            Log.d("MoPub", "Chartboost interstitial ad clicked.");
            getListener(location).onInterstitialClicked();
        }

        @Override
        public void didShowInterstitial(String location) {
            Log.d("MoPub", "Chartboost interstitial ad shown.");
            getListener(location).onInterstitialShown();
        }

        /*
         * More Apps delegate methods
         */
        @Override
        public boolean shouldDisplayLoadingViewForMoreApps() {
            return false;
        }

        @Override
        public boolean shouldRequestMoreApps() {
            return false;
        }

        @Override
        public boolean shouldDisplayMoreApps() {
            return false;
        }

        @Override
        public void didFailToLoadMoreApps() {
        }

        @Override
        public void didCacheMoreApps() {
        }

        @Override
        public void didDismissMoreApps() {
        }

        @Override
        public void didCloseMoreApps() {
        }

        @Override
        public void didClickMoreApps() {
        }

        @Override
        public void didShowMoreApps() {
        }

        CustomEventInterstitialListener getListener(String location) {
            CustomEventInterstitialListener listener = listenerForLocation.get(location);
            return listener != null ? listener : NULL_LISTENER;
        }
    }
}
