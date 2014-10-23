package com.mopub.mobileads;

import android.content.Context;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.mopub.common.util.Views;

import java.util.Map;

/**
 * Tested with Facebook SDK 3.18.1.
 */
public class FacebookBanner extends CustomEventBanner implements AdListener {
    private static final String PLACEMENT_ID_KEY = "placement_id";

    private AdView mFacebookBanner;
    private CustomEventBannerListener mBannerListener;

    /**
     * CustomEventBanner implementation
     */

    @Override
    protected void loadBanner(final Context context,
            final CustomEventBannerListener customEventBannerListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        final String placementId;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mFacebookBanner = new AdView(context, placementId, AdSize.BANNER_320_50);
        mFacebookBanner.setAdListener(this);
        mFacebookBanner.loadAd();
    }

    @Override
    protected void onInvalidate() {
        if (mFacebookBanner != null) {
            Views.removeFromParent(mFacebookBanner);
            mFacebookBanner.destroy();
            mFacebookBanner = null;
        }
    }

    /**
     * AdListener implementation
     */

    @Override
    public void onAdLoaded(Ad ad) {
        Log.d("MoPub", "Facebook banner ad loaded successfully. Showing ad...");
        mBannerListener.onBannerLoaded(mFacebookBanner);
    }

    @Override
    public void onError(final Ad ad, final AdError error) {
        Log.d("MoPub", "Facebook banner ad failed to load.");
        if (error == AdError.NO_FILL) {
            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
        } else if (error == AdError.INTERNAL_ERROR) {
            mBannerListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
        }
    }

    @Override
    public void onAdClicked(Ad ad) {
        Log.d("MoPub", "Facebook banner ad clicked.");
        mBannerListener.onBannerClicked();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    @Deprecated // for testing
    AdView getAdView() {
        return mFacebookBanner;
    }
}
