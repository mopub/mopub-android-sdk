package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.vungle.publisher.EventListener;
import com.vungle.publisher.VunglePub;

import java.util.Map;

/*
 * Tested with Vungle SDK 4.0.2
 */
public class VungleInterstitial extends CustomEventInterstitial implements EventListener {

    private static final String DEFAULT_VUNGLE_APP_ID = "YOUR_DEFAULT_VUNGLE_APP_ID";

    /*
     * APP_ID_KEY is intended for MoPub internal use. Do not modify.
     */
    public static final String APP_ID_KEY = "appId";

    private final VunglePub mVunglePub;
    private final Handler mHandler;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;

    public VungleInterstitial() {
        mHandler = new Handler(Looper.getMainLooper());
        mVunglePub = VunglePub.getInstance();
    }

    @Override
    protected void loadInterstitial(Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (context == null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        /*
         * You may pass the Vungle App Id in the serverExtras Map by specifying Custom Event Data
         * in MoPub's web interface.
         */
        final String appId;
        if (extrasAreValid(serverExtras)) {
            appId = serverExtras.get(APP_ID_KEY);
        } else {
            appId = DEFAULT_VUNGLE_APP_ID;
        }

        // init clears the event listener.
        mVunglePub.init(context, appId);
        mVunglePub.setEventListeners(this);
        if (mVunglePub.isAdPlayable()) {
            Log.d("MoPub", "Vungle interstitial ad successfully loaded.");
            mCustomEventInterstitialListener.onInterstitialLoaded();
        } else {
            Log.d("MoPub", "Vungle interstitial ad is not loaded.");
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
        }
    }

    @Override
    protected void showInterstitial() {
        if (mVunglePub.isAdPlayable()) {
            mVunglePub.playAd();
        } else {
            Log.d("MoPub", "Tried to show a Vungle interstitial ad before it finished loading. Please try again.");
        }
    }

    @Override
    protected void onInvalidate() {
        mVunglePub.clearEventListeners();
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(APP_ID_KEY);
    }

    /*
     * EventListener implementation
     */

    @Override
    public void onVideoView(final boolean isCompletedView, final int watchedMillis, final int videoDurationMillis) {
        final double watchedPercent = (double) watchedMillis / videoDurationMillis * 100;
        Log.d("MoPub", String.format("%.1f%% of Vungle video watched.", watchedPercent));
    }

    @Override
    public void onAdStart() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MoPub", "Showing Vungle interstitial ad.");
                mCustomEventInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onAdEnd(final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d("MoPub", "Vungle interstitial ad dismissed.");
                mCustomEventInterstitialListener.onInterstitialDismissed();
                if (wasCallToActionClicked) {
                    mCustomEventInterstitialListener.onInterstitialClicked();
                }
            }
        });
    }

    @Override
    public void onAdUnavailable(final String s) {
        mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void onAdPlayableChanged(final boolean playable) {
        Log.d("MoPub", String.format("Vungle interstitial ad is %s.",
                playable ? "playable" : "not playable"));
    }
}
