package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.config.ConfigFile;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.InterstitialListener;
import com.mopub.common.MoPub;

import java.util.Arrays;
import java.util.Map;

public class IronSourceInterstitial extends CustomEventInterstitial implements InterstitialListener {

    private static final String TAG = "MoPub";

    private static final String ADAPTER_VERSION = "2.5.3";
    private static final String ADAPTER_NAME = "Mopub";

    private static CustomEventInterstitialListener mMoPubListener;
    private static int initState;
    private static boolean isTestEnabled;

    /**
     * This is the placement name used inside ironSource SDK
     */
    private String placementName = null;

    private static final int INIT_NOT_STARTED = 0;
    private static final int INIT_PENDING = 1;
    private static final int INIT_SUCCEEDED = 2;

    private static Handler sHandler;

    // Mopub API Start

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> map0, Map<String, String> serverExtras) {

        try {
            mMoPubListener = customEventInterstitialListener;
            sHandler = new Handler(Looper.getMainLooper());

            if (context instanceof Activity) {
                //Set the Interstitial Listener
                String applicationKey = "";
                if (serverExtras != null) {
                    if (serverExtras.get("applicationKey") != null) {
                        applicationKey = serverExtras.get("applicationKey");
                    } else if (serverExtras.get("appKey") != null) {
                        //try appKey if applicationKey doesn't exists (fallback)
                        applicationKey = serverExtras.get("appKey");
                    }

                    if (serverExtras.get("isTestEnabled") != null) {
                        isTestEnabled = Boolean.valueOf(serverExtras.get("isTestEnabled"));
                    }

                    if (serverExtras.get("placementName") != null) {
                        placementName = serverExtras.get("placementName");
                    }

                    onLog("server extras: " + Arrays.toString(serverExtras.entrySet().toArray()));

                }
                if (!TextUtils.isEmpty(applicationKey)) {
                    initISIronSourceSDK(((Activity) context), applicationKey);
                    //Load ad unit
                    if (initState == INIT_SUCCEEDED) {
                        loadISIronSourceSDK();
                    }
                }
            } else {
                onLog("loadInterstitial must be called on an Activity context");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            }
        } catch (Exception e) {
            onLog(e.toString());
            sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected void showInterstitial() {
        onLog("showInterstitial " + placementName);
        try {
            if (IronSource.isInterstitialReady()) {
                if (TextUtils.isEmpty(placementName)) {
                    IronSource.showInterstitial();
                } else {
                    IronSource.showInterstitial(placementName);
                }
            } else {
                sendMoPubInterstitialFailed(MoPubErrorCode.NO_FILL);
            }
        } catch (Exception e) {
            onLog(e.toString());
            sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected void onInvalidate() {
        onLog("onInvalidate");
        mMoPubListener = null;
    }

    // Mopub API Finish //

    private void sendMoPubInterstitialFailed(final MoPubErrorCode errorCode) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialFailed(errorCode);
                }
            }
        });
    }

    private synchronized void initISIronSourceSDK(Activity activity, String appKey) {
        if (initState == INIT_NOT_STARTED) {
            onLog("initInterstitial - IronSourceInterstitial");
            initState = INIT_PENDING;
            IronSource.setInterstitialListener(this);
            ConfigFile.getConfigFile()
                    .setPluginData(ADAPTER_NAME, ADAPTER_VERSION, MoPub.SDK_VERSION);
            IronSource.setMediationType("mopub");
            IronSource.init(activity, appKey, IronSource.AD_UNIT.INTERSTITIAL, IronSource.AD_UNIT.REWARDED_VIDEO);
            initState = INIT_SUCCEEDED;
        }
    }

    private void loadISIronSourceSDK() {
        if (IronSource.isInterstitialReady()) {
            onInterstitialAdReady();
        } else {
            onLog("loadInterstitial");
            IronSource.loadInterstitial();
        }
    }

    /**
     * Activity Lifecycle Helper Methods
     **/
    public static void onActivityPaused(Activity activity) {
        IronSource.onPause(activity);
    }

    public static void onActivityResumed(Activity activity) {
        IronSource.onResume(activity);
    }

    @Override
    public void onInterstitialAdReady() {
        onLog("onInterstitialAdReady");
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialLoaded();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdLoadFailed(IronSourceError ironSourceError) {
        onLog("onInterstitialAdLoadFailed:" + ironSourceError.getErrorMessage());
        sendMoPubInterstitialFailed(getMoPubErrorMessage(ironSourceError));
    }

    private MoPubErrorCode getMoPubErrorMessage(IronSourceError ironSourceError) {
        if (ironSourceError == null) {
            return MoPubErrorCode.INTERNAL_ERROR;
        }
        switch (ironSourceError.getErrorCode()) {
            case IronSourceError.ERROR_CODE_NO_CONFIGURATION_AVAILABLE:
            case IronSourceError.ERROR_CODE_KEY_NOT_SET:
            case IronSourceError.ERROR_CODE_INVALID_KEY_VALUE:
            case IronSourceError.ERROR_CODE_INIT_FAILED:
                return MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
            case IronSourceError.ERROR_CODE_USING_CACHED_CONFIGURATION:
                return MoPubErrorCode.VIDEO_CACHE_ERROR;
            case IronSourceError.ERROR_CODE_NO_ADS_TO_SHOW:
                return MoPubErrorCode.NETWORK_NO_FILL;
            case IronSourceError.ERROR_CODE_GENERIC:
                return MoPubErrorCode.INTERNAL_ERROR;
            case IronSourceError.ERROR_NO_INTERNET_CONNECTION:
                return MoPubErrorCode.NO_CONNECTION;
            default:
                return MoPubErrorCode.NETWORK_TIMEOUT;
        }
    }

    @Override
    public void onInterstitialAdOpened() {
        onLog("onInterstitialAdOpened");
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialShown();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdClosed() {
        onLog("onInterstitialAdClosed");
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialDismissed();
                }
            }
        });
    }

    @Override
    public void onInterstitialAdShowSucceeded() {
        // not in use in Mopub mediation (we use the onInterstitialAdOpened for saying that the ad was shown)
    }

    @Override
    public void onInterstitialAdShowFailed(IronSourceError ironSourceError) {
        onLog("onInterstitialAdShowFailed:" + ironSourceError.getErrorMessage());
        // do nothing
    }

    @Override
    public void onInterstitialAdClicked() {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialClicked();
                }
            }
        });
    }

    private static void onLog(String message) {
        if (isTestEnabled) {
            Log.d(TAG, message);
        }
    }
}