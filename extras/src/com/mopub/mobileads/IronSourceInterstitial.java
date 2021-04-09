package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;
import com.ironsource.mediationsdk.sdk.ISDemandOnlyInterstitialListener;

import java.util.Arrays;
import java.util.Map;

public class IronSourceInterstitial extends CustomEventInterstitial implements ISDemandOnlyInterstitialListener {

    /**
     * private vars
     */

    private static final String TAG = "MoPub";

    // Configuration keys
    private static final String APPLICATION_KEY = "applicationKey";
    private static final String APP_KEY = "appKey";
    private static final String TEST_ENABLED_KEY = "isTestEnabled";
    private static final String PLACEMENT_KEY = "placementName";
    private static final String INSTANCE_ID_KEY = "instanceId";

    private static final String MEDIATION_TYPE = "mopub" ;

    // This is the instance id used inside ironSource SDK
    private String mInstanceId = null;

    // This is the placement name used inside ironSource SDK
    private String mPlacementName = null;

    static CustomEventInterstitialListener mMoPubListener;

    private static Handler sHandler;

    private static boolean mInitInterstitialSuccessfully;
    private boolean mIsTestEnabled;

    /**
     * Activity Lifecycle Helper Methods
     **/

    public static void onActivityPaused(Activity activity) {

        IronSource.onPause(activity);
    }

    public static void onActivityResumed(Activity activity) {

        IronSource.onResume(activity);
    }

    /**
     *  Mopub API
     */

    @Override
    protected void loadInterstitial(Context context, CustomEventInterstitialListener customEventInterstitialListener, Map<String, Object> map0, Map<String, String> serverExtras) {

        try {
            mMoPubListener = customEventInterstitialListener;
            sHandler = new Handler(Looper.getMainLooper());

            if (!(context instanceof Activity)) {
                // Context not an Activity context, log the reason for failure and fail the
                // initialization.
                onLog("loadInterstitial must be called on an Activity context");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
                return;
            }

            //Set the Interstitial Listener
            String applicationKey = "";
            mInstanceId = "0";
            if (serverExtras != null) {

                if (serverExtras.get(APPLICATION_KEY) != null) {
                    applicationKey = serverExtras.get(APPLICATION_KEY);

                } else if (serverExtras.get(APP_KEY) != null) {
                    //try appKey if applicationKey doesn't exists (fallback)
                    applicationKey = serverExtras.get(APP_KEY);
                }

                if (serverExtras.get(TEST_ENABLED_KEY) != null) {
                    mIsTestEnabled = Boolean.valueOf(serverExtras.get(TEST_ENABLED_KEY));
                }

                if (serverExtras.get(PLACEMENT_KEY) != null) {
                    mPlacementName = serverExtras.get(PLACEMENT_KEY);
                }

                if (serverExtras.get(INSTANCE_ID_KEY) != null) {
                    mInstanceId = serverExtras.get(INSTANCE_ID_KEY);
                }

                onLog("server extras: " + Arrays.toString(serverExtras.entrySet().toArray()));
            }

            if (!TextUtils.isEmpty(applicationKey)) {
                initIronSourceSDK(((Activity) context), applicationKey);
                //Load ad unit
                if (mInitInterstitialSuccessfully) {
                    loadInterstitial();
                }
            } else {
                onLog("Initialization Failed, make sure that 'applicationKey' server parameter is added");
                sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
            }

        } catch (Exception e) {
            onLog(e.toString());
            sendMoPubInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    protected void showInterstitial() {
        onLog("showInterstitial " + mPlacementName);
        try {
            if (IronSource.isISDemandOnlyInterstitialReady(mInstanceId)) {
                if (TextUtils.isEmpty(mPlacementName)) {
                    IronSource.showISDemandOnlyInterstitial(mInstanceId);
                } else {
                    IronSource.showISDemandOnlyInterstitial(mInstanceId,mPlacementName);
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

    /**
     * Class Helper Methods
     **/

    private void initIronSourceSDK(Activity activity, String appKey) {
        if (!mInitInterstitialSuccessfully) {
            onLog("initInterstitial - IronSourceInterstitial");
            IronSource.setISDemandOnlyInterstitialListener(this);
            IronSource.setMediationType(MEDIATION_TYPE);
            IronSource.initISDemandOnly(activity, appKey, IronSource.AD_UNIT.INTERSTITIAL);
            mInitInterstitialSuccessfully = true;
        }
    }

    private void loadInterstitial() {
        if (IronSource.isISDemandOnlyInterstitialReady(mInstanceId)) {
            onInterstitialAdReady(mInstanceId);
        } else {
            onLog("loadInterstitial");
            IronSource.loadISDemandOnlyInterstitial(mInstanceId);
        }
    }

    private void onLog(String message) {
        if (mIsTestEnabled) {
            Log.d(TAG, message);
        }
    }

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

    /**
     * IronSource Interstitial Listener
     **/

    @Override
    public void onInterstitialAdReady(String instanceId) {
        onLog("onInterstitialAdReady");
        if (!mInstanceId.equals(instanceId))
            return;

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
    public void onInterstitialAdLoadFailed(String instanceId, IronSourceError ironSourceError) {
        onLog("onInterstitialAdLoadFailed:" + ironSourceError.getErrorMessage());
        if (!mInstanceId.equals(instanceId))
            return;

        sendMoPubInterstitialFailed(getMoPubErrorMessage(ironSourceError));
    }

    @Override
    public void onInterstitialAdOpened(String instanceId) {
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
    public void onInterstitialAdClosed(String instanceId) {
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
    public void onInterstitialAdShowSucceeded(String instanceId) {
        // not in use in Mopub mediation (we use the onInterstitialAdOpened for saying that the ad was shown)
    }

    @Override
    public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
        onLog("onInterstitialAdShowFailed:" + ironSourceError.getErrorMessage());
        // do nothing
    }

    @Override
    public void onInterstitialAdClicked(String instanceId) {
        sHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMoPubListener != null) {
                    mMoPubListener.onInterstitialClicked();
                }
            }
        });
    }

}
