package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.chartboost.sdk.Chartboost;
import com.mopub.common.Preconditions;

import java.util.Map;

/**
 * A custom event for showing Chartboost interstitial ads.
 *
 * Certified with Chartboost 7.0.1
 */
class ChartboostInterstitial extends CustomEventInterstitial {

    @NonNull
    private String mLocation = ChartboostShared.LOCATION_DEFAULT;

    /*
     * Note: Chartboost recommends implementing their specific Activity lifecycle callbacks in your
     * Activity's onStart(), onStop(), onBackPressed() methods for proper results. Please see their
     * documentation for more information.
     */

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    protected void loadInterstitial(@NonNull Context context,
            @NonNull CustomEventInterstitialListener interstitialListener,
            @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(interstitialListener);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(serverExtras);

        if (!(context instanceof Activity)) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (serverExtras.containsKey(ChartboostShared.LOCATION_KEY)) {
            String location = serverExtras.get(ChartboostShared.LOCATION_KEY);
            mLocation = TextUtils.isEmpty(location) ? mLocation : location;
        }

        // If there's already a listener for this location, then another instance of
        // CustomEventInterstitial is still active and we should fail.
        if (ChartboostShared.getDelegate().hasInterstitialLocation(mLocation) &&
                ChartboostShared.getDelegate().getInterstitialListener(mLocation) != interstitialListener) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        Activity activity = (Activity) context;
        try {
            ChartboostShared.initializeSdk(activity, serverExtras);
            ChartboostShared.getDelegate().registerInterstitialListener(mLocation, interstitialListener);
        } catch (NullPointerException e) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        } catch (IllegalStateException e) {
            interstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        Chartboost.onCreate(activity);
        Chartboost.onStart(activity);
        if (Chartboost.hasInterstitial(mLocation)) {
            ChartboostShared.getDelegate().didCacheInterstitial(mLocation);
        } else {
            Chartboost.cacheInterstitial(mLocation);
        }
    }

    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing Chartboost interstitial ad.");
        Chartboost.showInterstitial(mLocation);
    }

    @Override
    protected void onInvalidate() {
        ChartboostShared.getDelegate().unregisterInterstitialListener(mLocation);
    }
}
