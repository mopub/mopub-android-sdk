package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.AdSize;
import com.facebook.ads.AdView;
import com.mopub.common.DataKeys;
import com.mopub.common.util.Views;

import java.util.Map;

/**
 * Certified with Facebook Audience Network 4.26.1
 */
public class FacebookBanner extends CustomEventBanner implements AdListener {
    public static final String PLACEMENT_ID_KEY = "placement_id";

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
        if (serverExtrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        int width;
        int height;
        if (localExtrasAreValid(localExtras)) {
            width = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            height = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
        } else {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        AdSize adSize = calculateAdSize(width, height);
        if (adSize == null) {
            mBannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mFacebookBanner = new AdView(context, placementId, adSize);
        mFacebookBanner.setAdListener(this);
        mFacebookBanner.disableAutoRefresh();
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

    @Override
    public void onLoggingImpression(Ad ad) {
        Log.d("MoPub", "Facebook banner ad logged impression.");
    }

    private boolean serverExtrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    private boolean localExtrasAreValid(@NonNull final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Nullable
    private AdSize calculateAdSize(int width, int height) {
        // Use the smallest AdSize that will properly contain the adView
        if (height <= AdSize.BANNER_320_50.getHeight()) {
            return AdSize.BANNER_320_50;
        } else if (height <= AdSize.BANNER_HEIGHT_90.getHeight()) {
            return AdSize.BANNER_HEIGHT_90;
        } else if (height <= AdSize.RECTANGLE_HEIGHT_250.getHeight()) {
            return AdSize.RECTANGLE_HEIGHT_250;
        } else {
            return null;
        }
    }

    @Deprecated // for testing
    AdView getAdView() {
        return mFacebookBanner;
    }
}
