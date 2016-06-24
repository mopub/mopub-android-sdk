package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

class FlurryCustomEventInterstitial extends com.mopub.mobileads.CustomEventInterstitial {
    private static final String LOG_TAG = FlurryCustomEventInterstitial.class.getSimpleName();

    private Context mContext;
    private CustomEventInterstitialListener mListener;

    private String mAdSpaceName;

    private FlurryAdInterstitial mInterstitial;

    // CustomEventInterstitial
    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener listener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        if (context == null) {
            Log.e(LOG_TAG, "Context cannot be null.");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (listener == null) {
            Log.e(LOG_TAG, "CustomEventInterstitialListener cannot be null.");
            return;
        }

        if (!(context instanceof Activity)) {
            Log.e(LOG_TAG, "Ad can be rendered only in Activity context.");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (!extrasAreValid(serverExtras)) {
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mContext = context;
        mListener = listener;

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        Log.d(LOG_TAG, "fetch Flurry ad (" + mAdSpaceName + ")");
        mInterstitial = new FlurryAdInterstitial(mContext, mAdSpaceName);
        mInterstitial.setListener(new FlurryMopubInterstitialListener());
        mInterstitial.fetchAd();
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        Log.d(LOG_TAG, "MoPub issued onInvalidate (" + mAdSpaceName + ")");

        if (mInterstitial != null) {
            mInterstitial.destroy();
            mInterstitial = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
        mListener = null;
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras != null && serverExtras.containsKey(FlurryAgentWrapper.PARAM_API_KEY) &&
                serverExtras.containsKey(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);
    }

    @Override
    protected void showInterstitial() {
        Log.d(LOG_TAG, "MoPub issued showInterstitial (" + mAdSpaceName + ")");

        if (mInterstitial != null) {
            mInterstitial.displayAd();
        }
    }

    // FlurryAdListener
    private class FlurryMopubInterstitialListener implements FlurryAdInterstitialListener {
        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        public void onFetched(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onFetched(" + adInterstitial.toString() + ")");

            if (mListener != null) {
                mListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onRendered(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onRendered(" + adInterstitial.toString() + ")");

            if (mListener != null) {
                mListener.onInterstitialShown();
            }
        }

        @Override
        public void onDisplay(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onDisplay(" + adInterstitial.toString() + ")");

            // no-op
        }

        @Override
        public void onClose(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onClose(" + adInterstitial.toString() + ")");

            if (mListener != null) {
                mListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAppExit(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onAppExit(" + adInterstitial.toString() + ")");

            if (mListener != null) {
                mListener.onLeaveApplication();
            }
        }

        @Override
        public void onClicked(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onClicked " + adInterstitial.toString());

            if (mListener != null) {
                mListener.onInterstitialClicked();
            }
        }

        @Override
        public void onVideoCompleted(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onVideoCompleted " + adInterstitial.toString());

            // no-op
        }

        @Override
        public void onError(FlurryAdInterstitial adBanner, FlurryAdErrorType adErrorType,
                            int errorCode) {
            Log.d(LOG_TAG, "onError(" + adBanner.toString() + adErrorType.toString() +
                    errorCode + ")");

            if (mListener != null) {
                if (FlurryAdErrorType.FETCH.equals(adErrorType)) {
                    mListener.onInterstitialFailed(NETWORK_NO_FILL);
                } else if (FlurryAdErrorType.RENDER.equals(adErrorType)) {
                    mListener.onInterstitialFailed(NETWORK_INVALID_STATE);
                }
            }
        }
    }
}
