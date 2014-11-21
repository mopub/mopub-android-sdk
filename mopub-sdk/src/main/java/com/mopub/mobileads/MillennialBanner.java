package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.millennialmedia.android.MMAd;
import com.millennialmedia.android.MMAdView;
import com.millennialmedia.android.MMException;
import com.millennialmedia.android.MMRequest;
import com.millennialmedia.android.MMSDK;
import com.millennialmedia.android.RequestListener;

import java.util.Map;

import static com.mopub.mobileads.MoPubErrorCode.NETWORK_NO_FILL;

/**
 * Compatible with version 5.3.0 of the Millennial Media SDK.
 */

class MillennialBanner extends CustomEventBanner {
    private MMAdView mMillennialAdView;
    private CustomEventBannerListener mBannerListener;
    public static final String APID_KEY = "adUnitID";
    public static final String AD_WIDTH_KEY = "adWidth";
    public static final String AD_HEIGHT_KEY = "adHeight";

    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;


        final String apid;
        final int width;
        final int height;
        if (extrasAreValid(serverExtras)) {
            apid = serverExtras.get(APID_KEY);
            width = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            height = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        MMSDK.initialize(context);

        mMillennialAdView = new MMAdView(context);
        mMillennialAdView.setListener(new MillennialBannerRequestListener());

        mMillennialAdView.setApid(apid);
        mMillennialAdView.setWidth(width);
        mMillennialAdView.setHeight(height);

        final Location location = (Location) localExtras.get("location");
        if (location != null) {
            MMRequest.setUserLocation(location);
        }

        mMillennialAdView.setMMRequest(new MMRequest());
        mMillennialAdView.setId(MMSDK.getDefaultAdId());
        AdViewController.setShouldHonorServerDimensions(mMillennialAdView);
        mMillennialAdView.getAd();
    }

    @Override
    protected void onInvalidate() {
        // mMillennialAdView can be null if loadBanner terminated prematurely (i.e. the associated
        // serverExtras are invalid).
        if (mMillennialAdView != null) {
            mMillennialAdView.setListener(null);
        }
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        try {
            Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
            Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));
        } catch (NumberFormatException e) {
            return false;
        }

        return serverExtras.containsKey(APID_KEY);
    }

    class MillennialBannerRequestListener implements RequestListener {
        @Override
        public void MMAdOverlayLaunched(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad Launched.");
            mBannerListener.onBannerExpanded();
        }

        @Override
        public void MMAdOverlayClosed(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad closed.");
            mBannerListener.onBannerCollapsed();
        }

        @Override
        public void MMAdRequestIsCaching(final MMAd mmAd) {}

        @Override
        public void requestCompleted(final MMAd mmAd) {
            Log.d("MoPub", "Millennial banner ad loaded successfully. Showing ad...");
            mBannerListener.onBannerLoaded(mMillennialAdView);
        }

        @Override
        public void requestFailed(final MMAd mmAd, final MMException e) {
            Log.d("MoPub", "Millennial banner ad failed to load.");
            mBannerListener.onBannerFailed(NETWORK_NO_FILL);
        }

        @Override
        public void onSingleTap(final MMAd mmAd) {
            mBannerListener.onBannerClicked();
        }
    }

    @Deprecated
    MMAdView getMMAdView() {
        return mMillennialAdView;
    }
}
