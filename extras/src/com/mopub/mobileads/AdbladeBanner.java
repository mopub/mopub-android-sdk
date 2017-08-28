package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;

import com.adiant.android.ads.AdView;
import com.adiant.android.ads.core.AdSize;
import com.adiant.android.ads.core.ErrorReason;
import com.adiant.android.ads.util.AdListener;
import com.mopub.common.DataKeys;
import com.mopub.common.util.Views;

import java.util.Map;
import java.util.TreeMap;

/**
 * Tested with Adblade 1.1.1
 */
public class AdbladeBanner extends CustomEventBanner {
    private static final String LOG_TAG = "MoPub";
    private static final String CONTAINER_ID_KEY = "container_id";

    private static class AdbladeAdListener extends AdListener {
        private final AdView adView;
        private final CustomEventBannerListener mopubListener;

        public AdbladeAdListener(AdView adView, CustomEventBannerListener mopubListener) {
            this.adView = adView;
            this.mopubListener = mopubListener;
        }

        @Override
        public void onAdLoaded(Object ad) {
            Log.d(LOG_TAG, "Adblade banner ad loaded successfully. Showing ad...");
            mopubListener.onBannerLoaded(adView);
        }

        @Override
        public void onAdLeftApplication() {
            Log.d(LOG_TAG, "Adblade banner ad left application.");
            mopubListener.onLeaveApplication();
        }

        @Override
        public void onAdLoadFailed(final ErrorReason error) {
            Log.d(LOG_TAG, "Adblade banner ad failed to load.");
            if (error == ErrorReason.NO_FILL) {
                mopubListener.onBannerFailed(MoPubErrorCode.NETWORK_NO_FILL);
            } else if (error == ErrorReason.NETWORK) {
                mopubListener.onBannerFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            } else {
                mopubListener.onBannerFailed(MoPubErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void onAdClicked() {
            Log.d(LOG_TAG, "Adblade banner ad clicked.");
            mopubListener.onBannerClicked();
        }
    }

    private static boolean areServerExtrasValid(final Map<String, String> serverExtras) {
        final String containerId = serverExtras.get(CONTAINER_ID_KEY);
        return containerId != null && !containerId.isEmpty();
    }

    private static boolean areLocalExtrasValid(@NonNull final Map<String, Object> localExtras) {
        return localExtras.get(DataKeys.AD_WIDTH) instanceof Integer
                && localExtras.get(DataKeys.AD_HEIGHT) instanceof Integer;
    }

    @Nullable
    private static AdSize calculateAdSize(int width, int height) {
        // Use the smallest AdSize that will properly contain the adView
        for (Map.Entry<Integer, AdSize> size : sizes().entrySet()) {
            if (height <= size.getKey()) {
                return size.getValue();
            }
        }
        return null;
    }

    private static Map<Integer, AdSize> sizes() {
        // get all sizes, and sort by height ascending
        final Map<Integer, AdSize> sizesByHeight = new TreeMap<>();
        for (AdSize s : AdSize.values()) {
            sizesByHeight.put(s.types().iterator().next().height(), s);
        }
        return sizesByHeight;
    }

    private AdView adView;

    @Override
    protected void loadBanner(Context context, CustomEventBannerListener mopubListener,
                              Map<String, Object> localExtras, Map<String, String> serverExtras) {
        final String containerId;
        if (areServerExtrasValid(serverExtras)) {
            containerId = serverExtras.get(CONTAINER_ID_KEY);
        } else {
            mopubListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        int width;
        int height;
        if (areLocalExtrasValid(localExtras)) {
            width = (Integer) localExtras.get(DataKeys.AD_WIDTH);
            height = (Integer) localExtras.get(DataKeys.AD_HEIGHT);
        } else {
            mopubListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        final AdSize adSize = calculateAdSize(width, height);
        if (adSize == null) {
            mopubListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        adView = new AdView(context);
        adView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        adView.setAdListener(new AdbladeAdListener(adView, mopubListener));
        adView.setAdSize(adSize);
        adView.setAdUnitId(containerId);
        adView.loadAd();
    }

    @Override
    protected void onInvalidate() {
        if (adView != null) {
            Views.removeFromParent(adView);
            adView = null;
        }
    }
}
