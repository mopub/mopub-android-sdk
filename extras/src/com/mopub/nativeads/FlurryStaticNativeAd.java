package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeAsset;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.mopub.mobileads.FlurryAgentWrapper;

import java.util.ArrayList;
import java.util.List;

public class FlurryStaticNativeAd extends StaticNativeAd {

    public static final String EXTRA_STAR_RATING_IMG = "flurry_starratingimage";
    public static final String EXTRA_APP_CATEGORY = "flurry_appcategorytext";
    public static final String EXTRA_SEC_BRANDING_LOGO = "flurry_brandingimage";

    private static final String LOG_TAG = FlurryStaticNativeAd.class.getSimpleName();
    private static final String ASSET_SEC_HQ_IMAGE = "secHqImage";
    private static final String ASSET_SEC_IMAGE = "secImage";
    private static final String ASSET_SEC_HQ_RATING_IMG = "secHqRatingImg";
    private static final String ASSET_SEC_HQ_BRANDING_LOGO = "secHqBrandingLogo";
    private static final String ASSET_SEC_RATING_IMG = "secRatingImg";
    private static final String ASSET_APP_RATING = "appRating";
    private static final String ASSET_APP_CATEGORY = "appCategory";
    private static final String ASSET_HEADLINE = "headline";
    private static final String ASSET_SUMMARY = "summary";
    private static final String ASSET_CALL_TO_ACTION = "callToAction";
    private static final double MOPUB_STAR_RATING_SCALE = StaticNativeAd.MAX_STAR_RATING;

    private final Context mContext;
    private final CustomEventNative.CustomEventNativeListener mCustomEventNativeListener;
    private FlurryAdNative mFlurryAdNative;

    FlurryAdNativeListener listener = new FlurryAdNativeListener() {
        @Override
        public void onFetched(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onFetched(" +adNative.toString() + ") Successful.");
            FlurryStaticNativeAd.this.onFetched(adNative);
        }

        @Override
        public void onShowFullscreen(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onShowFullscreen(" + adNative.toString() + ")");
        }

        @Override
        public void onCloseFullscreen(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onCloseFullscreen(" + adNative.toString() + ")");
        }

        @Override
        public void onAppExit(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onAppExit(" + adNative.toString() + ")");
        }

        @Override
        public void onClicked(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onClicked(" +adNative.toString() + ")");
            notifyAdClicked();
        }

        @Override
        public void onImpressionLogged(FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onImpressionLogged(" +flurryAdNative.toString() + ")");
            notifyAdImpressed();
        }

        @Override
        public void onExpanded(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onExpanded(" + adNative.toString() + ")");
        }

        @Override
        public void onCollapsed(FlurryAdNative adNative) {
            Log.d(LOG_TAG, "onCollapsed(" + adNative.toString() + ")");
        }

        @Override
        public void onError(FlurryAdNative adNative, FlurryAdErrorType adErrorType, int errorCode) {
            if (adErrorType.equals(FlurryAdErrorType.FETCH)) {
                Log.d(LOG_TAG, "onError(" + adNative.toString() + ", " + adErrorType.toString() +
                        ","+ errorCode + ")");
                FlurryStaticNativeAd.this.onFetchFailed(adNative);
            }
        }
    };

    FlurryStaticNativeAd(Context context, FlurryAdNative adNative,
                         CustomEventNative.CustomEventNativeListener mCustomEventNativeListener) {
        this.mContext = context;
        this.mFlurryAdNative = adNative;
        this.mCustomEventNativeListener = mCustomEventNativeListener;
    }

    // region StaticNativeAd
    @Override
    public void prepare(@NonNull final View view) {
        mFlurryAdNative.setTrackingView(view);
        Log.d(LOG_TAG, "prepare(" + mFlurryAdNative.toString() + " " + view.toString() + ")");
    }

    @Override
    public void clear(@NonNull View view) {
        mFlurryAdNative.removeTrackingView();
        Log.d(LOG_TAG, "clear("+ mFlurryAdNative.toString() + ")");
    }

    @Override
    public void destroy() {
        Log.d(LOG_TAG, "destroy(" + mFlurryAdNative.toString() + ") started.");
        mFlurryAdNative.destroy();

        FlurryAgentWrapper.getInstance().endSession(mContext);
    }
    //endregion

    synchronized void fetchAd() {
        Context context = mContext;
        if (context != null) {
            Log.d(LOG_TAG, "Fetching Flurry Native Ad now.");
            mFlurryAdNative.setListener(listener);
            mFlurryAdNative.fetchAd();
        } else {
            Log.d(LOG_TAG, "Context is null, not fetching Flurry Native Ad.");
        }
    }

    private synchronized void onFetched(FlurryAdNative adNative) {
        if (adNative != null) {
            Log.d(LOG_TAG, "onFetched: Native Ad fetched successfully!"
                    + adNative.toString());
            setupNativeAd(adNative);
        }
    }

    private synchronized void onFetchFailed(FlurryAdNative adNative) {
        Log.d(LOG_TAG, "onFetchFailed: Native ad not available. "
                + adNative.toString());
        if (mCustomEventNativeListener != null) {
            mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
        }
    }

    private synchronized void setupNativeAd(@NonNull FlurryAdNative adNative) {
        FlurryAdNativeAsset coverImageAsset = adNative.getAsset(ASSET_SEC_HQ_IMAGE);
        FlurryAdNativeAsset iconImageAsset = adNative.getAsset(ASSET_SEC_IMAGE);

        if (coverImageAsset != null && !TextUtils.isEmpty(coverImageAsset.getValue())) {
            setMainImageUrl(coverImageAsset.getValue());
        }
        if (iconImageAsset != null && !TextUtils.isEmpty(iconImageAsset.getValue())) {
            setIconImageUrl(iconImageAsset.getValue());
        }

        setTitle(adNative.getAsset(ASSET_HEADLINE).getValue());
        setText(adNative.getAsset(ASSET_SUMMARY).getValue());
        addExtra(EXTRA_SEC_BRANDING_LOGO,
                adNative.getAsset(ASSET_SEC_HQ_BRANDING_LOGO).getValue());

        if(isAppInstallAd()) {
            // App rating image URL may be null
            FlurryAdNativeAsset ratingHqImageAsset = adNative
                    .getAsset(ASSET_SEC_HQ_RATING_IMG);
            if (ratingHqImageAsset != null && !TextUtils.isEmpty(ratingHqImageAsset.getValue())) {
                addExtra(EXTRA_STAR_RATING_IMG, ratingHqImageAsset.getValue());
            } else {
                FlurryAdNativeAsset ratingImageAsset = adNative.getAsset(ASSET_SEC_RATING_IMG);
                if (ratingImageAsset != null && !TextUtils.isEmpty(ratingImageAsset.getValue())) {
                    addExtra(EXTRA_STAR_RATING_IMG, ratingImageAsset.getValue());
                }
            }

            FlurryAdNativeAsset appCategoryAsset = adNative.getAsset(ASSET_APP_CATEGORY);
            if (appCategoryAsset != null) {
                addExtra(EXTRA_APP_CATEGORY, appCategoryAsset.getValue());
            }
            FlurryAdNativeAsset appRatingAsset = adNative.getAsset(ASSET_APP_RATING);
            if(appRatingAsset != null) {
                setStarRating(getStarRatingValue(appRatingAsset.getValue()));
            }
        }

        FlurryAdNativeAsset ctaAsset = adNative.getAsset(ASSET_CALL_TO_ACTION);
        if(ctaAsset != null){
            setCallToAction(ctaAsset.getValue());
        }

        if (getImageUrls() == null || getImageUrls().isEmpty()) {
            Log.d(LOG_TAG, "preCacheImages: No images to cache for Flurry Native Ad: " +
                    adNative.toString());
            mCustomEventNativeListener.onNativeAdLoaded(this);
        } else {
            NativeImageHelper.preCacheImages(mContext, getImageUrls(),
                    new NativeImageHelper.ImageListener() {
                @Override
                public void onImagesCached() {
                    if (mCustomEventNativeListener != null) {
                        Log.d(LOG_TAG, "preCacheImages: Ad image cached.");
                        mCustomEventNativeListener.onNativeAdLoaded(FlurryStaticNativeAd.this);
                    } else {
                        Log.d(LOG_TAG, "Unable to notify cache failure: " +
                                "CustomEventNativeListener is null.");
                    }
                }

                @Override
                public void onImagesFailedToCache(NativeErrorCode errorCode) {
                    if (mCustomEventNativeListener != null) {
                        Log.d(LOG_TAG, "preCacheImages: Unable to cache Ad image. Error["
                                + errorCode.toString() + "]");
                        mCustomEventNativeListener.onNativeAdFailed(errorCode);
                    } else {
                        Log.d(LOG_TAG, "Unable to notify cache failure: " +
                                "CustomEventNativeListener is null.");
                    }
                }
            });
        }
    }

    private List<String> getImageUrls() {
        final List<String> imageUrls = new ArrayList<>(2);
        final String mainImageUrl = getMainImageUrl();

        if (mainImageUrl != null) {
            imageUrls.add(getMainImageUrl());
            Log.d(LOG_TAG, "Flurry Native Ad main image found.");
        }

        final String iconUrl = getIconImageUrl();
        if (iconUrl != null) {
            imageUrls.add(this.getIconImageUrl());
            Log.d(LOG_TAG, "Flurry Native Ad icon image found.");
        }
        return imageUrls;
    }

    @Nullable private Double getStarRatingValue(@Nullable String appRatingString) {
        // App rating String should be of the form X/Y. E.g. 80/100
        Double rating = null;
        if (appRatingString != null) {
            String[] ratingParts = appRatingString.split("/");
            if (ratingParts.length == 2) {
                try {
                    float numer = Integer.valueOf(ratingParts[0]);
                    float denom = Integer.valueOf(ratingParts[1]);
                    rating = (numer / denom) * MOPUB_STAR_RATING_SCALE;
                } catch (NumberFormatException e) { /*Ignore and return null*/ }
            }
        }
        return rating;
    }

    private boolean isAppInstallAd() {
        return mFlurryAdNative.getAsset(ASSET_SEC_RATING_IMG) != null ||
                mFlurryAdNative.getAsset(ASSET_SEC_HQ_RATING_IMG) != null ||
                mFlurryAdNative.getAsset(ASSET_APP_CATEGORY) != null;
    }
}