package com.mopub.simpleadsdemo;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.inmobi.androidsdk.IMAdInterstitial;
import com.inmobi.androidsdk.IMAdInterstitialListener;
import com.inmobi.androidsdk.IMAdRequest;
import com.inmobi.androidsdk.IMAdRequest.ErrorCode;
import com.mopub.mobileads.CustomEventInterstitial;
import com.mopub.mobileads.MoPubErrorCode;

/*
 * Tested with InMobi SDK 3.6.2.
 */
class InMobiInterstitial extends CustomEventInterstitial implements IMAdInterstitialListener {
    private CustomEventInterstitialListener mInterstitialListener;
    private IMAdInterstitial mInMobiInterstitial;

    /*
     * Abstract methods from CustomEventInterstitial
     */
    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener interstitialListener,
                                    Map<String, Object> localExtras, Map<String, String> serverExtras) {
        mInterstitialListener = interstitialListener;

        Activity activity = null;
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else {
            // You may also pass in an Activity Context in the localExtras map and retrieve it here.
        }

        if (activity == null) {
            mInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }
        
        /*
         * You may also pass this String down in the serverExtras Map by specifying Custom Event Data
         * in MoPub's web interface.
         */
        String inMobiAppId = "YOUR_INMOBI_APP_ID";
        mInMobiInterstitial = new IMAdInterstitial(activity, inMobiAppId);
        IMAdRequest adRequest = new IMAdRequest();
        Map<String,String> requestParams = new HashMap<String,String>();
        /*
         * For InMobi's tracking, "tp" variable needs to be passed with value "c_mopub"
         */
        requestParams.put("tp", "c_mopub");
        adRequest.setRequestParams(requestParams);

        mInMobiInterstitial.setIMAdInterstitialListener(this);
        mInMobiInterstitial.loadNewAd(adRequest);
    }

    @Override
    protected void showInterstitial() {
        Log.d("MoPub", "Showing InMobi interstitial ad.");
        mInMobiInterstitial.show();
    }

    @Override
    protected void onInvalidate() {
        mInMobiInterstitial.setIMAdInterstitialListener(null);
    }

    /*
     * IMAdListener implementation
     */
    @Override
    public void onAdRequestLoaded(IMAdInterstitial adInterstitial) {
        Log.d("MoPub", "InMobi interstitial ad loaded successfully.");
        mInterstitialListener.onInterstitialLoaded();
    }

    @Override
    public void onAdRequestFailed(IMAdInterstitial adInterstitial, ErrorCode errorCode) {
        Log.d("MoPub", "InMobi interstitial ad failed to load.");
        mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void onShowAdScreen(IMAdInterstitial adInterstitial) {
        mInterstitialListener.onInterstitialShown();
    }

    @Override
    public void onLeaveApplication(IMAdInterstitial adInterstitial) {
        /*
         * Because InMobi does not have an onClick equivalent, we use onLeaveApplication
         * as a click notification.
         */
        Log.d("MoPub", "InMobi interstitial ad leaving application.");
        mInterstitialListener.onInterstitialClicked();
    }

    @Override
    public void onDismissAdScreen(IMAdInterstitial adInterstitial) {
        Log.d("MoPub", "InMobi interstitial ad dismissed.");
        mInterstitialListener.onInterstitialDismissed();
    }
}
