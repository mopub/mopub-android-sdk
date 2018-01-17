package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdInterstitial;
import com.flurry.android.ads.FlurryAdInterstitialListener;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

/**
 * Certified with Flurry 8.1.0
 */
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

        if (!validateExtras(serverExtras)) {
            Log.e(LOG_TAG, "Failed interstitial ad fetch: Missing required server extras" +
                    " [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
            listener.onInterstitialFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mContext = context;
        mListener = listener;

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        Log.d(LOG_TAG, "Fetching Flurry ad, ad unit name:" + mAdSpaceName);
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

    @Override
    protected void showInterstitial() {
        Log.d(LOG_TAG, "MoPub issued showInterstitial (" + mAdSpaceName + ")");

        if (mInterstitial != null) {
            mInterstitial.displayAd();
        }
    }

    private boolean validateExtras(final Map<String, String> serverExtras) {
        if (serverExtras == null) {
            return false;
        }

        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);
        Log.i(LOG_TAG, "ServerInfo fetched from Mopub " + FlurryAgentWrapper.PARAM_API_KEY + " : "
                + flurryApiKey + " and " + FlurryAgentWrapper.PARAM_AD_SPACE_NAME + " :" +
                flurryAdSpace);

        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

    // FlurryAdListener
    private class FlurryMopubInterstitialListener implements FlurryAdInterstitialListener {
        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        public void onFetched(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onFetched: Flurry interstitial ad fetched successfully!");

            if (mListener != null) {
                mListener.onInterstitialLoaded();
            }
        }

        @Override
        public void onRendered(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onRendered: Flurry interstitial ad rendered");

            if (mListener != null) {
                mListener.onInterstitialShown();
            }
        }

        @Override
        public void onDisplay(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onDisplay: Flurry interstitial ad displayed");

            // no-op
        }

        @Override
        public void onClose(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onClose: Flurry interstitial ad closed");

            if (mListener != null) {
                mListener.onInterstitialDismissed();
            }
        }

        @Override
        public void onAppExit(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onAppExit: Flurry interstitial ad exited app");
        }

        @Override
        public void onClicked(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onClicked: Flurry interstitial ad clicked");

            if (mListener != null) {
                mListener.onInterstitialClicked();
            }
        }

        @Override
        public void onVideoCompleted(FlurryAdInterstitial adInterstitial) {
            Log.d(LOG_TAG, "onVideoCompleted: Flurry interstitial ad video completed");

            // no-op
        }

        @Override
        public void onError(FlurryAdInterstitial adInterstitial, FlurryAdErrorType adErrorType,
                int errorCode) {
            Log.d(LOG_TAG, String.format("onError: Flurry interstitial ad not available. " +
                    "Error type: %s. Error code: %s", adErrorType.toString(), errorCode));

            if (mListener != null) {
                switch (adErrorType) {
                    case FETCH:
                        mListener.onInterstitialFailed(NETWORK_NO_FILL);
                        return;
                    case RENDER:
                        mListener.onInterstitialFailed(NETWORK_INVALID_STATE);
                        return;
                    case CLICK:
                        // Don't call onInterstitialFailed in this case.
                        return;
                    default:
                        mListener.onInterstitialFailed(UNSPECIFIED);
                }
            }
        }
    }
}
