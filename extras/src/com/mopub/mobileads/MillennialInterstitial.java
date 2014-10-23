package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.MMInterstitial;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;
import com.millennialmedia.android.RequestListener;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

/**
 * Compatible with version 5.3.0 of the Millennial Media SDK.
 */

class MillennialInterstitial extends CustomEventInterstitial {
    private MMInterstitial mMillennialInterstitial;
    private CustomEventInterstitialListener mInterstitialListener;
    public static final String APID_KEY = "adUnitID";

    @Override
    protected void loadInterstitial(final Context context, final CustomEventInterstitialListener customEventInterstitialListener,
                                    final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        mInterstitialListener = customEventInterstitialListener;

        final String apid;
        if (extrasAreValid(serverExtras)) {
            apid = serverExtras.get(APID_KEY);
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        MMSDK.initialize(context);

        final Location location = (Location) localExtras.get("location");
        if (location != null) {
            MMRequest.setUserLocation(location);
        }

        mMillennialInterstitial = new MMInterstitial(context);
        mMillennialInterstitial.setListener(new MillennialInterstitialRequestListener());
        mMillennialInterstitial.setMMRequest(new MMRequest());
        mMillennialInterstitial.setApid(apid);
        mMillennialInterstitial.fetch();
    }

    @Override
    protected void showInterstitial() {
        if (mMillennialInterstitial.isAdAvailable()) {
            mMillennialInterstitial.display();
        } else {
            Log.d("MoPub", "Tried to show a Millennial interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected void onInvalidate() {
        // mMillennialInterstitial can be null if loadInterstitial terminated prematurely (i.e.
        // the associated serverExtras are invalid).
        if (mMillennialInterstitial != null) {
            mMillennialInterstitial.setListener(null);
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialInterstitialRequestListener implements RequestListener {
        @Override
        public void MMAdOverlayLaunched(final MMAd mmAd) {
            Log.d("MoPub", "Showing Millennial interstitial ad.");
            mInterstitialListener.onInterstitialShown();
        }

        @Override
        public void MMAdOverlayClosed(final MMAd mmAd) {
            Log.d("MoPub", "Millennial interstitial ad dismissed.");
            mInterstitialListener.onInterstitialDismissed();
        }

        @Override public void MMAdRequestIsCaching(final MMAd mmAd) {}

        @Override
        public void requestCompleted(final MMAd mmAd) {
            if (mMillennialInterstitial.isAdAvailable()) {
                Log.d("MoPub", "Millennial interstitial ad loaded successfully.");
                mInterstitialListener.onInterstitialLoaded();
            } else {
                Log.d("MoPub", "Millennial interstitial request completed, but no ad was available.");
                mInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            }
        }

        @Override
        public void requestFailed(final MMAd mmAd, final MMException e) {
            if (mMillennialInterstitial == null || e == null) {
                mInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            } else if (e.getCode() == MMException.CACHE_NOT_EMPTY && mMillennialInterstitial.isAdAvailable()) {
                // requestFailed can be due to an ad already loaded or an ad failed to load.
                Log.d("MoPub", "Millennial interstitial loaded successfully from cache.");
                mInterstitialListener.onInterstitialLoaded();
            } else {
                Log.d("MoPub", "Millennial interstitial ad failed to load.");
                mInterstitialListener.onInterstitialFailed(NETWORK_NO_FILL);
            }
        }

        @Override
        public void onSingleTap(final MMAd mmAd) {
            Log.d("MoPub", "Millennial interstitial clicked.");
            mInterstitialListener.onInterstitialClicked();
        }

    }
}
