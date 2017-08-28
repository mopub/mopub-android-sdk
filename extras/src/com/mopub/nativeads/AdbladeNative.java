package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.adiant.android.ads.NewsbulletFactory;
import com.adiant.android.ads.core.ErrorReason;
import com.adiant.android.ads.core.Newsbullet;
import com.adiant.android.ads.util.AdListener;

import java.util.Collections;
import java.util.Map;

/**
 * Tested with Adblade 1.1.1
 */
public class AdbladeNative extends CustomEventNative {
    public static final String EXTRA_DISPLAY_NAME = "display_name";

    private static final String LOG_TAG = "MoPub";
    private static final String CONTAINER_ID_KEY = "container_id";

    private static class AdbladeAdListener extends AdListener {
        private final AdbladeForwardingNativeAd ad;
        private final CustomEventNativeListener mopubListener;

        public AdbladeAdListener(AdbladeForwardingNativeAd ad, CustomEventNativeListener mopubListener) {
            this.ad = ad;
            this.mopubListener = mopubListener;
        }

        @Override
        public void onAdLoaded(Object result) {
            Log.d(LOG_TAG, "Adblade native ad loaded successfully. Caching images...");
            Newsbullet nb = (Newsbullet) result;
            ad.adLoaded(nb);
            BaseForwardingNativeAd.preCacheImages(ad.getContext(), Collections.singletonList(nb.getBannerUrl()), new ImageListener() {
                @Override
                public void onImagesCached() {
                    Log.d(LOG_TAG, "Adblade native ad images cached successfully.");
                    mopubListener.onNativeAdLoaded(ad);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    Log.d(LOG_TAG, "Adblade native ad images failed to cache.");
                    mopubListener.onNativeAdFailed(errorCode);
                }
            });
        }

        @Override
        public void onAdLoadFailed(final ErrorReason error) {
            Log.d(LOG_TAG, "Adblade native ad failed to load.");
            if (error == ErrorReason.NO_FILL) {
                mopubListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            } else if (error == ErrorReason.NETWORK) {
                mopubListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
            } else {
                mopubListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            }
        }
    }

    private static class AdbladeForwardingNativeAd extends BaseForwardingNativeAd {
        private final Context context;

        public AdbladeForwardingNativeAd(Context context) {
            this.context = context;
        }

        public void adLoaded(Newsbullet ad) {
            setClickDestinationUrl(ad.getClickUrl());
            setMainImageUrl(ad.getBannerUrl());
            setText(ad.getDescription());
            setTitle(ad.getTitle());
            addExtra(EXTRA_DISPLAY_NAME, ad.getDisplayName());
        }

        public Context getContext() {
            return context;
        }
    }

    private static boolean areServerExtrasValid(final Map<String, String> serverExtras) {
        final String containerId = serverExtras.get(CONTAINER_ID_KEY);
        return containerId != null && !containerId.isEmpty();
    }

    @Override
    protected void loadNativeAd(@NonNull Context context,
                                @NonNull CustomEventNativeListener mopubListener,
                                @NonNull Map<String, Object> localExtras,
                                @NonNull Map<String, String> serverExtras) {
        final String containerId;
        if (areServerExtrasValid(serverExtras)) {
            containerId = serverExtras.get(CONTAINER_ID_KEY);
        } else {
            mopubListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        new NewsbulletFactory(containerId, context).loadAd(new AdbladeAdListener(
                new AdbladeForwardingNativeAd(context), mopubListener));
    }
}
