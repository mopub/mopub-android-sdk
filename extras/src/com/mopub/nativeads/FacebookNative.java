package com.mopub.nativeads;

import android.content.Context;
import android.view.View;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdListener;
import com.facebook.ads.ImpressionListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAd.Rating;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Tested with Facebook SDK 3.23.1
 */
public class FacebookNative extends CustomEventNative {
    private static final String PLACEMENT_ID_KEY = "placement_id";

    // CustomEventNative implementation
    @Override
    protected void loadNativeAd(final Context context,
            final CustomEventNativeListener customEventNativeListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras) {

        final String placementId;
        if (extrasAreValid(serverExtras)) {
            placementId = serverExtras.get(PLACEMENT_ID_KEY);
        } else {
            customEventNativeListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        final FacebookForwardingNativeAd facebookForwardingNativeAd =
                new FacebookForwardingNativeAd(context,
                        new NativeAd(context, placementId), customEventNativeListener);
        facebookForwardingNativeAd.loadAd();
    }

    private boolean extrasAreValid(final Map<String, String> serverExtras) {
        final String placementId = serverExtras.get(PLACEMENT_ID_KEY);
        return (placementId != null && placementId.length() > 0);
    }

    static class FacebookForwardingNativeAd extends BaseForwardingNativeAd implements AdListener, ImpressionListener {
        private static final String SOCIAL_CONTEXT_FOR_AD = "socialContextForAd";

        private final Context mContext;
        private final NativeAd mNativeAd;
        private final CustomEventNativeListener mCustomEventNativeListener;

        FacebookForwardingNativeAd(final Context context,
                final NativeAd nativeAd,
                final CustomEventNativeListener customEventNativeListener) {
            mContext = context.getApplicationContext();
            mNativeAd = nativeAd;
            mCustomEventNativeListener = customEventNativeListener;
        }

        void loadAd() {
            mNativeAd.setAdListener(this);
            mNativeAd.setImpressionListener(this);
            mNativeAd.loadAd();
        }

        // AdListener
        @Override
        public void onAdLoaded(final Ad ad) {
            // This identity check is from Facebook's Native API sample code:
            // https://developers.facebook.com/docs/audience-network/android/native-api
            if (!mNativeAd.equals(ad) || !mNativeAd.isAdLoaded()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
                return;
            }

            setTitle(mNativeAd.getAdTitle());
            setText(mNativeAd.getAdBody());

            NativeAd.Image coverImage = mNativeAd.getAdCoverImage();
            setMainImageUrl(coverImage == null ? null : coverImage.getUrl());

            NativeAd.Image icon = mNativeAd.getAdIcon();
            setIconImageUrl(icon == null ? null : icon.getUrl());

            setCallToAction(mNativeAd.getAdCallToAction());
            setStarRating(getDoubleRating(mNativeAd.getAdStarRating()));

            addExtra(SOCIAL_CONTEXT_FOR_AD, mNativeAd.getAdSocialContext());

            final List<String> imageUrls = new ArrayList<String>();
            final String mainImageUrl = getMainImageUrl();
            if (mainImageUrl != null) {
                imageUrls.add(getMainImageUrl());
            }
            final String iconUrl = getIconImageUrl();
            if (iconUrl != null) {
                imageUrls.add(getIconImageUrl());
            }

            preCacheImages(mContext, imageUrls, new ImageListener() {
                @Override
                public void onImagesCached() {
                    mCustomEventNativeListener.onNativeAdLoaded(FacebookForwardingNativeAd.this);
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    mCustomEventNativeListener.onNativeAdFailed(errorCode);
                }
            });
        }

        @Override
        public void onError(final Ad ad, final AdError adError) {
            if (adError == null) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            } else if (adError.getErrorCode() == AdError.NO_FILL.getErrorCode()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            } else if (adError.getErrorCode() == AdError.INTERNAL_ERROR.getErrorCode()) {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_INVALID_STATE);
            } else {
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.UNSPECIFIED);
            }
        }

        @Override
        public void onAdClicked(final Ad ad) {
            notifyAdClicked();
        }

        // ImpressionListener
        @Override
        public void onLoggingImpression(final Ad ad) {
            notifyAdImpressed();
        }

        // BaseForwardingNativeAd
        @Override
        public void prepare(final View view) {
            mNativeAd.registerViewForInteraction(view);
            setOverridingClickTracker(true);
            setOverridingImpressionTracker(true);
        }

        @Override
        public void clear(final View view) {
            mNativeAd.unregisterView();
        }

        @Override
        public void destroy() {
            mNativeAd.destroy();
        }

        private Double getDoubleRating(final Rating rating) {
            if (rating == null) {
                return null;
            }

            return MAX_STAR_RATING * rating.getValue() / rating.getScale();
        }
    }
}
