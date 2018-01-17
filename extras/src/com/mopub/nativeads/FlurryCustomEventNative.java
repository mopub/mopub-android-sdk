package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.flurry.android.FlurryAgentListener;
import com.flurry.android.ads.FlurryAdErrorType;
import com.flurry.android.ads.FlurryAdNative;
import com.flurry.android.ads.FlurryAdNativeAsset;
import com.flurry.android.ads.FlurryAdNativeListener;
import com.flurry.android.ads.FlurryAdTargeting;
import com.mopub.mobileads.FlurryAgentWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Certified with Flurry 8.1.0
 */
public final class FlurryCustomEventNative extends CustomEventNative {

    /**
     * Extra image asset for the star-rating of app-install ads.
     */
    public static final String EXTRA_STAR_RATING_IMG = "flurry_starratingimage";
    /**
     * Extra text asset specifying the category for app-install ads.
     */
    public static final String EXTRA_APP_CATEGORY = "flurry_appcategorytext";
    /**
     * Extra image asset for the Flurry native ad branding logo.
     */
    public static final String EXTRA_SEC_BRANDING_LOGO = "flurry_brandingimage";
    /**
     * Key for enabling Flurry debug logging. After manually creating a {@link MoPubNative} object,
     * pass in <code>true</code> as a value for this key in the map passed to
     * {@link MoPubNative#setLocalExtras(Map)}.
     *
     * E.g.
     *
     * <blockquote><pre>
     * {@code
     * Map<String, Object> adapterExtras = new TreeMap<String, Object>();
     * adapterExtras.put(FlurryCustomEventNative.LOCAL_EXTRA_TEST_MODE, true);
     * mMoPubNative.setLocalExtras(adapterExtras);
     * }
     * </pre></blockquote>
     *
     * Debug logging is disabled on the Flurry SDK by default.
     */
    public static final String LOCAL_EXTRA_TEST_MODE = "enableTestMode";
    private static final String LOG_TAG = FlurryCustomEventNative.class.getSimpleName();
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
    private static final String ASSET_VIDEO = "videoUrl";
    private static final double MOPUB_STAR_RATING_SCALE = StaticNativeAd.MAX_STAR_RATING;

    private FlurryAgentListener mFlurryAgentListener;

    /* Static reference of the ads to ensure they don't get garbage collected */
    private final static List<FlurryAdNative> sFlurryNativeAds = new ArrayList<>();

    @Override
    protected void loadNativeAd(@NonNull final Context context,
            @NonNull final CustomEventNativeListener customEventNativeListener,
            @NonNull final Map<String, Object> localExtras,
            @NonNull final Map<String, String> serverExtras) {

        final String flurryApiKey;
        final String flurryAdSpace;

        //Get the FLURRY_APIKEY and FLURRY_ADSPACE from the server.
        if (validateExtras(serverExtras)) {
            flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
            flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

            if (FlurryAgentWrapper.getInstance().isSessionActive() || mFlurryAgentListener != null) {
                fetchFlurryAd(context, flurryAdSpace, localExtras, customEventNativeListener);
            } else {
                mFlurryAgentListener = new FlurryAgentListener() {
                    @Override
                    public void onSessionStarted() {
                        fetchFlurryAd(context, flurryAdSpace, localExtras,
                                customEventNativeListener);
                    }
                };

                FlurryAgentWrapper.getInstance().startSession(context, flurryApiKey,
                        mFlurryAgentListener);
            }
        } else {
            customEventNativeListener.onNativeAdFailed(
                    NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            Log.i(LOG_TAG, "Failed Native AdFetch: Missing required server extras" +
                    " [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
        }
    }

    private static synchronized void mapNativeAd(@NonNull final FlurryBaseNativeAd mopubSupportedAd,
            @NonNull final FlurryAdNative flurryAdNative) {
        FlurryAdNativeAsset coverImageAsset = flurryAdNative.getAsset(ASSET_SEC_HQ_IMAGE);
        FlurryAdNativeAsset iconImageAsset = flurryAdNative.getAsset(ASSET_SEC_IMAGE);

        if (coverImageAsset != null && !TextUtils.isEmpty(coverImageAsset.getValue())) {
            mopubSupportedAd.setMainImageUrl(coverImageAsset.getValue());
        }
        if (iconImageAsset != null && !TextUtils.isEmpty(iconImageAsset.getValue())) {
            mopubSupportedAd.setIconImageUrl(iconImageAsset.getValue());
        }

        mopubSupportedAd.setTitle(flurryAdNative.getAsset(ASSET_HEADLINE).getValue());
        mopubSupportedAd.setText(flurryAdNative.getAsset(ASSET_SUMMARY).getValue());
        mopubSupportedAd.addExtra(EXTRA_SEC_BRANDING_LOGO,
                flurryAdNative.getAsset(ASSET_SEC_HQ_BRANDING_LOGO).getValue());

        if (mopubSupportedAd.isAppInstallAd()) {
            // App rating image URL may be null
            FlurryAdNativeAsset ratingHqImageAsset = flurryAdNative
                    .getAsset(ASSET_SEC_HQ_RATING_IMG);
            if (ratingHqImageAsset != null && !TextUtils.isEmpty(ratingHqImageAsset.getValue())) {
                mopubSupportedAd.addExtra(EXTRA_STAR_RATING_IMG, ratingHqImageAsset.getValue());
            } else {
                FlurryAdNativeAsset ratingImageAsset = flurryAdNative
                        .getAsset(ASSET_SEC_RATING_IMG);
                if (ratingImageAsset != null && !TextUtils.isEmpty(ratingImageAsset.getValue())) {
                    mopubSupportedAd.addExtra(EXTRA_STAR_RATING_IMG, ratingImageAsset.getValue());
                }
            }

            FlurryAdNativeAsset appCategoryAsset = flurryAdNative.getAsset(ASSET_APP_CATEGORY);
            if (appCategoryAsset != null) {
                mopubSupportedAd.addExtra(EXTRA_APP_CATEGORY, appCategoryAsset.getValue());
            }
            FlurryAdNativeAsset appRatingAsset = flurryAdNative.getAsset(ASSET_APP_RATING);
            if (appRatingAsset != null) {
                mopubSupportedAd.setStarRating(getStarRatingValue(appRatingAsset.getValue()));
            }
        }

        FlurryAdNativeAsset ctaAsset = flurryAdNative.getAsset(ASSET_CALL_TO_ACTION);
        if (ctaAsset != null) {
            mopubSupportedAd.setCallToAction(ctaAsset.getValue());
        }

        if (mopubSupportedAd.getImageUrls().isEmpty()) {
            Log.d(LOG_TAG, "preCacheImages: No images to cache for Flurry Native Ad: " +
                    flurryAdNative.toString());
            mopubSupportedAd.onNativeAdLoaded();
        } else {
            mopubSupportedAd.precacheImages();
        }
    }

    @Nullable
    private static Double getStarRatingValue(@Nullable final String appRatingString) {
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

    private boolean validateExtras(final Map<String, String> serverExtras) {
        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);
        Log.i(LOG_TAG, "ServerInfo fetched from Mopub " + FlurryAgentWrapper.PARAM_API_KEY + " : "
                + flurryApiKey + " and " + FlurryAgentWrapper.PARAM_AD_SPACE_NAME + " :" +
                flurryAdSpace);
        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

    private void fetchFlurryAd(@NonNull Context context, String flurryAdSpace,
            @NonNull Map<String, Object> localExtras,
            @NonNull CustomEventNativeListener customEventNativeListener) {
        final FlurryAdNative flurryAdNative = new FlurryAdNative(context, flurryAdSpace);

        if (localExtras.containsKey(LOCAL_EXTRA_TEST_MODE) &&
                localExtras.get(LOCAL_EXTRA_TEST_MODE) instanceof Boolean) {
            final FlurryAdTargeting targeting = new FlurryAdTargeting();
            targeting.setEnableTestAds((Boolean) localExtras.get(LOCAL_EXTRA_TEST_MODE));
        }

        final FlurryBaseNativeAd flurryNativeAd;
        if (shouldAllowVideoNativeAds()) {
            flurryNativeAd = new FlurryVideoEnabledNativeAd(context, flurryAdNative,
                    customEventNativeListener);
        } else {
            flurryNativeAd = new FlurryStaticNativeAd(context, flurryAdNative,
                    customEventNativeListener);
        }

        // Add to requested ad list
        sFlurryNativeAds.add(flurryAdNative);

        flurryNativeAd.fetchAd();
    }

    private boolean shouldAllowVideoNativeAds() {
        try {
            Class.forName("com.mopub.nativeads.FlurryNativeAdRenderer");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }

    /**
     * Class that supports Flurry static native ads with the help of the
     * {@link MoPubStaticNativeAdRenderer}.
     *
     * @see FlurryVideoEnabledNativeAd
     */
    private static class FlurryStaticNativeAd extends StaticNativeAd implements FlurryBaseNativeAd {

        @NonNull private final Context mContext;
        @NonNull private final CustomEventNativeListener mCustomEventNativeListener;
        @NonNull private final FlurryAdNative mFlurryAdNative;
        private final FlurryAdNativeListener mFlurryNativelistener = new FlurryBaseAdListener(this) {
            @Override
            public void onClicked(final FlurryAdNative flurryAdNative) {
                super.onClicked(flurryAdNative);
                notifyAdClicked();
            }

            @Override
            public void onImpressionLogged(final FlurryAdNative flurryAdNative) {
                super.onImpressionLogged(flurryAdNative);
                notifyAdImpressed();
            }

            @Override
            public void onError(final FlurryAdNative adNative,
                    final FlurryAdErrorType adErrorType,
                    final int errorCode) {
                super.onError(adNative, adErrorType, errorCode);
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            }
        };

        FlurryStaticNativeAd(@NonNull Context context, @NonNull FlurryAdNative adNative,
                @NonNull CustomEventNativeListener customEventNativeListener) {
            mContext = context;
            mFlurryAdNative = adNative;
            mCustomEventNativeListener = customEventNativeListener;
        }

        // region StaticNativeAd

        @Override
        public void prepare(@NonNull final View view) {
            mFlurryAdNative.setTrackingView(view);
            Log.d(LOG_TAG, "prepare(" + mFlurryAdNative.toString() + " " + view.toString() + ")");
        }

        @Override
        public void clear(@NonNull final View view) {
            mFlurryAdNative.removeTrackingView();
            Log.d(LOG_TAG, "clear(" + mFlurryAdNative.toString() + ")");
        }

        @Override
        public void destroy() {
            Log.d(LOG_TAG, "destroy(" + mFlurryAdNative.toString() + ") started.");
            mFlurryAdNative.destroy();

            FlurryAgentWrapper.getInstance().endSession(mContext);
        }

        //endregion

        // region FlurryBaseNativeAd

        @Override
        public synchronized void fetchAd() {
            Log.d(LOG_TAG, "Fetching Flurry Native Ad now.");
            mFlurryAdNative.setListener(mFlurryNativelistener);
            mFlurryAdNative.fetchAd();
        }

        @NonNull
        public List<String> getImageUrls() {
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

        public boolean isAppInstallAd() {
            return mFlurryAdNative.getAsset(ASSET_SEC_RATING_IMG) != null ||
                    mFlurryAdNative.getAsset(ASSET_SEC_HQ_RATING_IMG) != null ||
                    mFlurryAdNative.getAsset(ASSET_APP_CATEGORY) != null;
        }

        @Override
        public void onNativeAdLoaded() {
            mCustomEventNativeListener.onNativeAdLoaded(this);
        }

        @Override
        public void precacheImages() {
            NativeImageHelper.preCacheImages(mContext, getImageUrls(),
                    new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            Log.d(LOG_TAG, "preCacheImages: Ad image cached.");
                            mCustomEventNativeListener.onNativeAdLoaded(FlurryStaticNativeAd.this);
                        }

                        @Override
                        public void onImagesFailedToCache(final NativeErrorCode errorCode) {
                            mCustomEventNativeListener.onNativeAdFailed(errorCode);
                            Log.d(LOG_TAG, "preCacheImages: Unable to cache Ad image. Error["
                                    + errorCode.toString() + "]");
                        }
                    });
        }

        //endregion
    }

    /**
     * Class that supports both Flurry static and video native ads with the help of
     * {@link FlurryNativeAdRenderer}. This class does not need to be included if you
     * are fetching only static ads. Use {@link FlurryStaticNativeAd} instead.
     */
    static class FlurryVideoEnabledNativeAd extends BaseNativeAd implements FlurryBaseNativeAd {
        private @NonNull final Context mContext;
        private @NonNull final CustomEventNativeListener mCustomEventNativeListener;
        private @NonNull final FlurryAdNative mFlurryAdNative;
        private final FlurryAdNativeListener mFlurryNativelistener = new FlurryBaseAdListener(this) {
            @Override
            public void onClicked(final FlurryAdNative flurryAdNative) {
                super.onClicked(flurryAdNative);
                notifyAdClicked();
            }

            @Override
            public void onImpressionLogged(final FlurryAdNative flurryAdNative) {
                super.onImpressionLogged(flurryAdNative);
                notifyAdImpressed();
            }

            @Override
            public void onError(final FlurryAdNative adNative,
                    final FlurryAdErrorType adErrorType,
                    final int errorCode) {
                super.onError(adNative, adErrorType, errorCode);
                mCustomEventNativeListener.onNativeAdFailed(NativeErrorCode.NETWORK_NO_FILL);
            }
        };

        // Basic fields
        @Nullable private String mTitle;
        @Nullable private String mText;
        @Nullable private String mCallToAction;
        @Nullable private String mMainImageUrl;
        @Nullable private String mIconImageUrl;
        @Nullable private Double mStarRating;

        // Extras
        @NonNull private final Map<String, Object> mExtras;


        FlurryVideoEnabledNativeAd(@NonNull Context context, @NonNull FlurryAdNative adNative,
                @NonNull CustomEventNativeListener customEventNativeListener) {
            mContext = context;
            mFlurryAdNative = adNative;
            mCustomEventNativeListener = customEventNativeListener;
            mExtras = new HashMap<>();
        }

        // region StaticNativeAd

        @Override
        public void prepare(@NonNull View view) {
            mFlurryAdNative.setTrackingView(view);
            Log.d(LOG_TAG, "prepare(" + mFlurryAdNative.toString() + " " + view.toString() + ")");
        }

        @Override
        public void clear(@NonNull View view) {
            mFlurryAdNative.removeTrackingView();
            Log.d(LOG_TAG, "clear(" + mFlurryAdNative.toString() + ")");
        }

        @Override
        public void destroy() {
            Log.d(LOG_TAG, "destroy(" + mFlurryAdNative.toString() + ") started.");
            mFlurryAdNative.destroy();

            FlurryAgentWrapper.getInstance().endSession(mContext);
        }

        // endregion

        // region FlurryBaseNativeAd

        @Override
        public synchronized void fetchAd() {
            Log.d(LOG_TAG, "Fetching Flurry Native Ad now.");
            mFlurryAdNative.setListener(mFlurryNativelistener);
            mFlurryAdNative.fetchAd();
        }

        @Override
        public boolean isAppInstallAd() {
            return mFlurryAdNative.getAsset(ASSET_SEC_RATING_IMG) != null ||
                    mFlurryAdNative.getAsset(ASSET_SEC_HQ_RATING_IMG) != null ||
                    mFlurryAdNative.getAsset(ASSET_APP_CATEGORY) != null;
        }

        @Override
        public void precacheImages() {
            NativeImageHelper.preCacheImages(mContext, getImageUrls(),
                    new NativeImageHelper.ImageListener() {
                        @Override
                        public void onImagesCached() {
                            Log.d(LOG_TAG, "preCacheImages: Ad image cached.");
                            mCustomEventNativeListener.onNativeAdLoaded(
                                    FlurryVideoEnabledNativeAd.this);
                        }

                        @Override
                        public void onImagesFailedToCache(final NativeErrorCode errorCode) {
                            mCustomEventNativeListener.onNativeAdFailed(errorCode);
                            Log.d(LOG_TAG, "preCacheImages: Unable to cache Ad image. Error["
                                    + errorCode.toString() + "]");
                        }
                    });
        }

        @NonNull
        @Override
        public List<String> getImageUrls() {
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

        @Nullable
        @Override
        public String getTitle() {
            return mTitle;
        }

        @Nullable
        @Override
        public String getText() {
            return mText;
        }

        @Nullable
        @Override
        public String getCallToAction() {
            return mCallToAction;
        }

        @Nullable
        @Override
        public String getMainImageUrl() {
            return mMainImageUrl;
        }

        @Nullable
        @Override
        public String getIconImageUrl() {
            return mIconImageUrl;
        }

        @Nullable
        @Override
        public Double getStarRating() {
            return mStarRating;
        }

        @NonNull
        @Override
        public Map<String, Object> getExtras() {
            return mExtras;
        }

        @Override
        public void setTitle(@Nullable String title) {
            mTitle = title;
        }

        @Override
        public void setText(@Nullable String text) {
            mText = text;
        }

        @Override
        public void setCallToAction(@Nullable String callToAction) {
            mCallToAction = callToAction;
        }

        @Override
        public void setMainImageUrl(@Nullable String mainImageUrl) {
            mMainImageUrl = mainImageUrl;
        }

        @Override
        public void setIconImageUrl(@Nullable String iconImageUrl) {
            mIconImageUrl = iconImageUrl;
        }

        @Override
        public void setStarRating(@Nullable Double starRating) {
            mStarRating = starRating;
        }

        @Override
        public void addExtra(@NonNull String key, @Nullable Object value) {
            mExtras.put(key, value);
        }

        @Override
        public void onNativeAdLoaded() {
            mCustomEventNativeListener.onNativeAdLoaded(this);
        }

        // endregion

        boolean isVideoAd() {
            return mFlurryAdNative.isVideoAd();
        }

        void loadVideoIntoView(@NonNull ViewGroup videoView) {
            mFlurryAdNative.getAsset(ASSET_VIDEO).loadAssetIntoView(videoView);
        }
    }

    static abstract class FlurryBaseAdListener implements FlurryAdNativeListener {
        private final @NonNull FlurryBaseNativeAd mBaseNativeAd;

        FlurryBaseAdListener(@NonNull FlurryBaseNativeAd baseNativeAd) {
            mBaseNativeAd = baseNativeAd;
        }

        @Override
        public void onFetched(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onFetched: Flurry native ad fetched successfully!");
            mapNativeAd(mBaseNativeAd, flurryAdNative);
            sFlurryNativeAds.remove(flurryAdNative);
        }

        @Override
        public void onShowFullscreen(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onShowFullscreen: Flurry native ad in full-screen");
        }

        @Override
        public void onCloseFullscreen(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onCloseFullscreen: Flurry native ad full-screen closed");
        }

        @Override
        public void onAppExit(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onAppExit: Flurry native ad exited app");
        }

        @Override
        public void onClicked(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onClicked: Flurry native ad clicked");
        }

        @Override
        public void onImpressionLogged(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onImpressionLogged: Flurry native ad impression logged");
        }

        @Override
        public void onExpanded(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onExpanded: Flurry native ad expanded");
        }

        @Override
        public void onCollapsed(final FlurryAdNative flurryAdNative) {
            Log.d(LOG_TAG, "onCollapsed: Flurry native ad collapsed");
        }

        @Override
        public void onError(final FlurryAdNative flurryAdNative,
                final FlurryAdErrorType adErrorType,
                final int errorCode) {
            Log.d(LOG_TAG, String.format("onError: Flurry native ad not available. " +
                    "Error type: %s. Error code: %s", adErrorType.toString(), errorCode));
            sFlurryNativeAds.remove(flurryAdNative);
        }
    }
}
