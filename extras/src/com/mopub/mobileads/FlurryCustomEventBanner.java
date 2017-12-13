package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;

import com.flurry.android.ads.FlurryAdBanner;
import com.flurry.android.ads.FlurryAdBannerListener;
import com.flurry.android.ads.FlurryAdErrorType;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

/**
 * Certified with Flurry 8.1.0
 */
class FlurryCustomEventBanner extends com.mopub.mobileads.CustomEventBanner {
    private static final String LOG_TAG = FlurryCustomEventBanner.class.getSimpleName();

    private Context mContext;
    private CustomEventBannerListener mListener;
    private FrameLayout mLayout;

    private String mAdSpaceName;

    private FlurryAdBanner mBanner;

    // CustomEventBanner
    @Override
    protected void loadBanner(Context context,
            CustomEventBannerListener listener,
            Map<String, Object> localExtras, Map<String, String> serverExtras) {
        if (context == null) {
            Log.e(LOG_TAG, "Context cannot be null.");
            listener.onBannerFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (listener == null) {
            Log.e(LOG_TAG, "CustomEventBannerListener cannot be null.");
            return;
        }

        if (!(context instanceof Activity)) {
            Log.e(LOG_TAG, "Ad can be rendered only in Activity context.");
            listener.onBannerFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        if (!validateExtras(serverExtras)) {
            Log.e(LOG_TAG, "Failed banner ad fetch: Missing required server extras" +
                    " [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
            listener.onBannerFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mContext = context;
        mListener = listener;
        mLayout = new FrameLayout(context);

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        Log.d(LOG_TAG, "Fetching Flurry ad, ad unit name:" + mAdSpaceName);
        mBanner = new FlurryAdBanner(mContext, mLayout, mAdSpaceName);
        mBanner.setListener(new FlurryMopubBannerListener());
        mBanner.fetchAd();
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        Log.d(LOG_TAG, "MoPub issued onInvalidate. Flurry ad unit: " + mAdSpaceName);

        if (mBanner != null) {
            mBanner.destroy();
            mBanner = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
        mListener = null;
        mLayout = null;
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
    private class FlurryMopubBannerListener implements FlurryAdBannerListener {
        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        public void onFetched(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onFetched: Flurry banner ad fetched successfully!");

            if (mBanner != null) {
                mBanner.displayAd();
            }
        }

        @Override
        public void onRendered(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onRendered: Flurry banner ad rendered");

            if (mListener != null) {
                mListener.onBannerLoaded(mLayout);
            }
        }

        @Override
        public void onShowFullscreen(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onFetched: Flurry banner ad in full-screen");

            if (mListener != null) {
                mListener.onBannerExpanded();
            }
        }

        @Override
        public void onCloseFullscreen(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onCloseFullscreen: Flurry banner ad full-screen closed");

            if (mListener != null) {
                mListener.onBannerCollapsed();
            }
        }

        @Override
        public void onAppExit(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onAppExit: Flurry banner ad exited app");
        }

        @Override
        public void onClicked(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onClicked: Flurry banner ad clicked");

            if (mListener != null) {
                mListener.onBannerClicked();
            }
        }

        @Override
        public void onVideoCompleted(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onVideoCompleted: Flurry banner ad video completed");

            // no-op
        }

        @Override
        public void onError(FlurryAdBanner adBanner, FlurryAdErrorType adErrorType,
                int errorCode) {
            Log.d(LOG_TAG, String.format("onError: Flurry banner ad not available. " +
                    "Error type: %s. Error code: %s", adErrorType.toString(), errorCode));

            if (mListener != null) {
                switch (adErrorType) {
                    case FETCH:
                        mListener.onBannerFailed(NETWORK_NO_FILL);
                        return;
                    case RENDER:
                        mListener.onBannerFailed(NETWORK_INVALID_STATE);
                        return;
                    case CLICK:
                        // Don't call onBannerFailed in this case.
                        return;
                    default:
                        mListener.onBannerFailed(UNSPECIFIED);
                }
            }
        }
    }
}
