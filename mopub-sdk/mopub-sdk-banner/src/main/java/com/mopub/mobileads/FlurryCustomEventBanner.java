package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import com.flurry.android.ads.FlurryAdBanner;
import com.flurry.android.ads.FlurryAdBannerListener;
import com.flurry.android.ads.FlurryAdErrorType;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

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

        if (!extrasAreValid(serverExtras)) {
            listener.onBannerFailed(ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mContext = context;
        mListener = listener;
        mLayout = new FrameLayout(context);

        String apiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        mAdSpaceName = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

        FlurryAgentWrapper.getInstance().startSession(context, apiKey, null);

        Log.d(LOG_TAG, "fetch Flurry Ad (" + mAdSpaceName + ") -- " + mLayout.toString());
        mBanner = new FlurryAdBanner(mContext, mLayout, mAdSpaceName);
        mBanner.setListener(new FlurryMopubBannerListener());
        mBanner.fetchAd();
    }

    @Override
    protected void onInvalidate() {
        if (mContext == null) {
            return;
        }

        Log.d(LOG_TAG, "MoPub issued onInvalidate (" + mAdSpaceName + ")");

        if (mBanner != null) {
            mBanner.destroy();
            mBanner = null;
        }

        FlurryAgentWrapper.getInstance().endSession(mContext);

        mContext = null;
        mListener = null;
        mLayout = null;
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras != null && serverExtras.containsKey(FlurryAgentWrapper.PARAM_API_KEY) &&
                serverExtras.containsKey(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

    }

    // FlurryAdListener
    private class FlurryMopubBannerListener implements FlurryAdBannerListener {
        private final String LOG_TAG = getClass().getSimpleName();

        @Override
        public void onFetched(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onFetched(" + adBanner.toString() + ")");

            if (mBanner != null) {
                mBanner.displayAd();
            }
        }

        @Override
        public void onRendered(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onRendered(" + adBanner.toString() + ")");

            if (mListener != null) {
                mListener.onBannerLoaded(mLayout);
            }
        }

        @Override
        public void onShowFullscreen(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onShowFullscreen(" + adBanner.toString() + ")");

            if (mListener != null) {
                mListener.onBannerExpanded();
            }
        }

        @Override
        public void onCloseFullscreen(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onCloseFullscreen(" + adBanner.toString() + ")");

            if (mListener != null) {
                mListener.onBannerCollapsed();
            }
        }

        @Override
        public void onAppExit(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onAppExit(" + adBanner.toString() + ")");

            if (mListener != null) {
                mListener.onLeaveApplication();
            }
        }

        @Override
        public void onClicked(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onClicked " + adBanner.toString());

            if (mListener != null) {
                mListener.onBannerClicked();
            }
        }

        @Override
        public void onVideoCompleted(FlurryAdBanner adBanner) {
            Log.d(LOG_TAG, "onVideoCompleted " + adBanner.toString());
            
            // no-op
        }

        @Override
        public void onError(FlurryAdBanner adBanner, FlurryAdErrorType adErrorType,
                            int errorCode) {
            Log.d(LOG_TAG, "onError(" + adBanner.toString() + adErrorType.toString() +
                    errorCode + ")");

            if (mListener != null) {
                if (FlurryAdErrorType.FETCH.equals(adErrorType)) {
                    mListener.onBannerFailed(NETWORK_NO_FILL);
                } else if (FlurryAdErrorType.RENDER.equals(adErrorType)) {
                    mListener.onBannerFailed(NETWORK_INVALID_STATE);               
                }
            }
        }
    }
}
