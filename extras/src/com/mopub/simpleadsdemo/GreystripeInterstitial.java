package com.mopub.simpleadsdemo;

import android.content.Context;
import android.util.Log;

import com.greystripe.sdk.AdPosition;
import com.greystripe.sdk.GSAd;
import com.greystripe.sdk.GSAdErrorCode;
import com.greystripe.sdk.GSAdListener;
import com.greystripe.sdk.GSFullscreenAd;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

import java.util.Map;

/*
 * Tested with Greystripe SDK 2.4.0.
 */
class GreystripeInterstitial extends CustomEventInterstitial implements GSAdListener {

    private static final String DEFAULT_GREYSTRIPE_APP_ID = "YOUR_GREYSTRIPE_APP_ID";

    /*
     * These keys are intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "GUID";

    private CustomEventInterstitialListener mInterstitialListener;
    private GSFullscreenAd mGreystripeAd;

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    protected void loadInterstitial(final Context context,
                                    final CustomEventInterstitialListener interstitialListener,
                                    final Map<String, Object> localExtras,
                                    final Map<String, String> serverExtras) {
        mInterstitialListener = interstitialListener;

        String greystripeAppId = DEFAULT_GREYSTRIPE_APP_ID;
        if (extrasAreValid(serverExtras)) {
            greystripeAppId = serverExtras.get(APP_ID_KEY);
        }

        mGreystripeAd = new GSFullscreenAd(context, greystripeAppId);
        mGreystripeAd.addListener(this);

        mGreystripeAd.fetch();
    }

    private static boolean extrasAreValid(Map<String, String> extras) {
        return extras.containsKey(APP_ID_KEY);
    }

    @Override
    protected void showInterstitial() {
        if (!mGreystripeAd.isAdReady()) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        Log.d("MoPub", "Showing Greystripe interstitial ad.");
        mGreystripeAd.display();
        mInterstitialListener.onInterstitialShown();
    }

    @Override
    protected void onInvalidate() {
        mGreystripeAd.removeListener(this);
    }

    /*
     * GSAdListener implementation
     */
    @Override
    public void onAdClickthrough(final GSAd greystripeAd) {
        Log.d("MoPub", "Greystripe interstitial ad clicked.");
        mInterstitialListener.onInterstitialClicked();

        /*
         * XXX: When a Greystripe interstitial is dismissed as a result of a user click, the
         * onAdDismissal callback does not get fired. This call ensures that the custom event
         * listener is informed of all dismissals.
         */
        mInterstitialListener.onInterstitialDismissed();
    }

    @Override
    public void onAdDismissal(final GSAd greystripeAd) {
        Log.d("MoPub", "Greystripe interstitial ad dismissed.");
        mInterstitialListener.onInterstitialDismissed();
    }

    @Override
    public void onFailedToFetchAd(final GSAd greystripeAd, final GSAdErrorCode errorCode) {
        Log.d("MoPub", "Greystripe interstitial ad failed to load.");
        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void onFetchedAd(final GSAd greystripeAd) {
        if (mGreystripeAd != null && mGreystripeAd.isAdReady()) {
            Log.d("MoPub", "Greysripe interstitial ad loaded successfully.");
            mInterstitialListener.onInterstitialLoaded();
        } else {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        }
    }

    @Override
    public void onAdCollapse(final GSAd greystripeAd) {
    }

    @Override
    public void onAdExpansion(final GSAd greystripeAd) {
    }

    @Override
    public void onAdResize(final GSAd gsAd, final AdPosition adPosition) {
    }
}
