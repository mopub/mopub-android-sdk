package com.mopub.mobileads;

import android.content.Context;
import android.util.Log;

import com.adiant.android.ads.InterstitialAd;
import com.adiant.android.ads.core.ErrorReason;
import com.adiant.android.ads.util.AdListener;

import java.util.Map;

/**
 * Tested with Adblade 1.1.1
 */
public class AdbladeInterstitial extends CustomEventInterstitial {
    private static final String LOG_TAG = "MoPub";
    private static final String CONTAINER_ID_KEY = "container_id";

    private static class AdbladeAdListener extends AdListener {
        private final CustomEventInterstitialListener mopubListener;

        public AdbladeAdListener(CustomEventInterstitialListener mopubListener) {
            this.mopubListener = mopubListener;
        }

        @Override
        public void onAdLoaded(Object ad) {
            Log.d(LOG_TAG, "Adblade interstitial ad loaded successfully.");
            mopubListener.onInterstitialLoaded();
        }

        @Override
        public void onAdLeftApplication() {
            Log.d(LOG_TAG, "Adblade interstitial ad left application.");
            mopubListener.onLeaveApplication();
        }

        @Override
        public void onAdLoadFailed(final ErrorReason error) {
            Log.d(LOG_TAG, "Adblade interstitial ad failed to load.");
            if (error == ErrorReason.NO_FILL) {
                mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            } else if (error == ErrorReason.NETWORK) {
                mopubListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            } else {
                mopubListener.onInterstitialFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void onAdClicked() {
            Log.d(LOG_TAG, "Adblade interstitial ad clicked.");
            mopubListener.onInterstitialClicked();
        }

        @Override
        public void onAdClosed() {
            Log.d(LOG_TAG, "Adblade interstitial ad dismissed.");
            mopubListener.onInterstitialDismissed();
        }

        @Override
        public void onAdOpened() {
            Log.d(LOG_TAG, "Showing Adblade interstitial ad.");
            mopubListener.onInterstitialShown();
        }
    }

    private static boolean areServerExtrasValid(final Map<String, String> serverExtras) {
        final String containerId = serverExtras.get(CONTAINER_ID_KEY);
        return containerId != null && !containerId.isEmpty();
    }

    private InterstitialAd interstitial;

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener mopubListener,
                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
        final String containerId;
        if (areServerExtrasValid(serverExtras)) {
            containerId = serverExtras.get(CONTAINER_ID_KEY);
        } else {
            mopubListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        interstitial = new InterstitialAd(context);
        interstitial.setAdListener(new AdbladeAdListener(mopubListener));
        interstitial.setAdUnitId(containerId);
        interstitial.loadAd();
    }

    @Override
    protected void showInterstitial() {
        if (interstitial != null && interstitial.isLoaded()) {
            interstitial.show();
        } else {
            Log.d(LOG_TAG, "Tried to show an Adblade interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected void onInvalidate() {
        if (interstitial != null) {
            interstitial.destroy();
            interstitial = null;
        }
    }
}
