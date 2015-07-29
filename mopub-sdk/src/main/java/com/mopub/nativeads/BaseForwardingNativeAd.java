package com.mopub.nativeads;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.mopub.common.Preconditions.NoThrow;
import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.ImageLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.CustomEventNative.ImageListener;

abstract class BaseForwardingNativeAd implements NativeAdInterface {
    private static final int IMPRESSION_MIN_PERCENTAGE_VIEWED = 50;

    static interface NativeEventListener {
        public void onAdImpressed();
        public void onAdClicked();
    }
    @Nullable private NativeEventListener mNativeEventListener;

    static final double MIN_STAR_RATING = 0;
    static final double MAX_STAR_RATING = 5;

    // Basic fields
    @Nullable private String mMainImageUrl;
    @Nullable private String mIconImageUrl;
    @Nullable private String mClickDestinationUrl;
    @Nullable private String mCallToAction;
    @Nullable private String mTitle;
    @Nullable private String mText;
    @Nullable private Double mStarRating;

    @Nullable private Drawable mDefaultIcon;

    // Impression logistics
    @NonNull private final Set<String> mImpressionTrackers;
    private int mImpressionMinTimeViewed;

    // Extras
    @NonNull private final Map<String, Object> mExtras;

    // Event Logistics
    private boolean mIsOverridingClickTracker;
    private boolean mIsOverridingImpressionTracker;

    BaseForwardingNativeAd() {
        mImpressionMinTimeViewed = 1000;

        mImpressionTrackers = new HashSet<String>();
        mExtras = new HashMap<String, Object>();
    }

    // Getters
    /**
     * Returns the String url corresponding to the ad's main image.
     */
    @Nullable
    @Override
    final public String getMainImageUrl() {
        return mMainImageUrl;
    }

    /**
     * Returns the String url corresponding to the ad's icon image.
     */
    @Nullable
    @Override
    final public String getIconImageUrl() {
        return mIconImageUrl;
    }

    /**
     * Returns a Set<String> of all impression trackers associated with this native ad. Note that
     * network requests will automatically be made to each of these impression trackers when the
     * native ad is display on screen. See {@link BaseForwardingNativeAd#getImpressionMinPercentageViewed}
     * and {@link BaseForwardingNativeAd#getImpressionMinTimeViewed()} for relevant
     * impression-tracking parameters.
     */
    @NonNull
    @Override
    final public Set<String> getImpressionTrackers() {
        return new HashSet<String>(mImpressionTrackers);
    }

    /**
     * Returns the String url that the device will attempt to resolve when the ad is clicked.
     */
    @Nullable
    @Override
    final public String getClickDestinationUrl() {
        return mClickDestinationUrl;
    }

    /**
     * Returns the Call To Action String (i.e. "Download" or "Learn More") associated with this ad.
     */
    @Nullable
    @Override
    final public String getCallToAction() {
        return mCallToAction;
    }

    /**
     * Returns the String corresponding to the ad's title.
     */
    @Nullable
    @Override
    final public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the String corresponding to the ad's body text.
     */
    @Nullable
    @Override
    final public String getText() {
        return mText;
    }

    /**
     * For app install ads, this returns the associated star rating (on a 5 star scale) for the
     * advertised app. Note that this method may return null if the star rating was either never set
     * or invalid.
     */
    @Nullable
    @Override
    final public Double getStarRating() {
        return mStarRating;
    }

    @Nullable
    @Override
    public Drawable getDefaultIcon() {
        return mDefaultIcon;
    }

    /**
     * Returns the minimum viewable percentage of the ad that must be onscreen for it to be
     * considered visible. See {@link BaseForwardingNativeAd#getImpressionMinTimeViewed()} for
     * additional impression tracking considerations.
     */
    @Override
    final public int getImpressionMinPercentageViewed() {
        return IMPRESSION_MIN_PERCENTAGE_VIEWED;
    }

    /**
     * Returns the minimum amount of time (in milliseconds) the ad that must be onscreen before an
     * impression is recorded. See {@link BaseForwardingNativeAd#getImpressionMinPercentageViewed()}
     * for additional impression tracking considerations.
     */
    @Override
    final public int getImpressionMinTimeViewed() {
        return mImpressionMinTimeViewed;
    }

    // Extras Getters
    /**
     * Given a particular String key, return the associated Object value from the ad's extras map.
     * See {@link BaseForwardingNativeAd#getExtras()} for more information.
     */
    @Nullable
    @Override
    final public Object getExtra(@NonNull final String key) {
        if (!NoThrow.checkNotNull(key, "getExtra key is not allowed to be null")) {
            return null;
        }
        return mExtras.get(key);
    }

    /**
     * Returns a copy of the extras map, reflecting additional ad content not reflected in any
     * of the above hardcoded setters. This is particularly useful for passing down custom fields
     * with MoPub's direct-sold native ads or from mediated networks that pass back additional
     * fields.
     */
    @NonNull
    @Override
    final public Map<String, Object> getExtras() {
        return new HashMap<String, Object>(mExtras);
    }

    /**
     * Returns {@code true} if the native ad is using a network impression tracker. If set to
     * true, the network must expose a callback that calls into
     * {@link BaseForwardingNativeAd#notifyAdImpressed()} in order for MoPub to fire its impression
     * tracker at the appropriate time.
     */
    @Override
    final public boolean isOverridingImpressionTracker() {
        return mIsOverridingImpressionTracker;
    }

    /**
     * Returns {@code true} if the native ad is using a network click tracker. If set to true, the
     * network must expose a callback that calls into
     * {@link BaseForwardingNativeAd#notifyAdClicked()} in order for MoPub to fire its click tracker
     * at the appropriate time.
     */
    @Override
    final public boolean isOverridingClickTracker() {
        return mIsOverridingClickTracker;
    }

    // Setters
    @Override
    public final void setNativeEventListener(
            @Nullable final NativeEventListener nativeEventListener) {
        mNativeEventListener = nativeEventListener;
    }

    final void setMainImageUrl(@Nullable final String mainImageUrl) {
        mMainImageUrl = mainImageUrl;
    }

    final void setIconImageUrl(@Nullable final String iconImageUrl) {
        mIconImageUrl = iconImageUrl;
    }

    final void setClickDestinationUrl(@Nullable final String clickDestinationUrl) {
        mClickDestinationUrl = clickDestinationUrl;
    }

    final void setCallToAction(@Nullable final String callToAction) {
        mCallToAction = callToAction;
    }

    final void setTitle(@Nullable final String title) {
        mTitle = title;
    }

    final void setText(@Nullable final String text) {
        mText = text;
    }

    final void setDefaultIcon(@Nullable Drawable defaultIcon) {
        mDefaultIcon = defaultIcon;
    }

    final void setStarRating(@Nullable final Double starRating) {
        if (starRating == null) {
            mStarRating = null;
        } else if (starRating >= MIN_STAR_RATING && starRating <= MAX_STAR_RATING) {
            mStarRating = starRating;
        } else {
            MoPubLog.d("Ignoring attempt to set invalid star rating (" + starRating + "). Must be "
                    + "between " + MIN_STAR_RATING + " and " + MAX_STAR_RATING + ".");
        }
    }

    final void addExtra(@NonNull final String key, @Nullable final Object value) {
        if (!NoThrow.checkNotNull(key, "addExtra key is not allowed to be null")) {
            return;
        }
        mExtras.put(key, value);
    }

    final void addImpressionTracker(@NonNull final String url) {
        if (!NoThrow.checkNotNull(url, "impressionTracker url is not allowed to be null")) {
            return;
        }
        mImpressionTrackers.add(url);
    }

    final void setImpressionMinTimeViewed(final int impressionMinTimeViewed) {
        if (impressionMinTimeViewed >= 0) {
            mImpressionMinTimeViewed = impressionMinTimeViewed;
        }
    }

    final void setOverridingImpressionTracker(final boolean isOverridingImpressionTracker) {
        mIsOverridingImpressionTracker = isOverridingImpressionTracker;
    }

    final void setOverridingClickTracker(final boolean isOverridingClickTracker) {
        mIsOverridingClickTracker = isOverridingClickTracker;
    }

    // Event Handlers
    /**
     * Your base native ad subclass should implement this method if the network requires the developer
     * to prepare state for recording an impression or click before a view is rendered to screen.
     *
     * This method is optional.
     */
    @Override
    public void prepare(@Nullable final View view) { }

    /**
     * Your base native ad subclass should implement this method if the network requires the developer
     * to explicitly record an impression of a view rendered to screen.
     *
     * This method is optional.
     */
    @Override
    public void recordImpression() { }

    /**
     * Your base native ad subclass should implement this method if the network requires the developer
     * to explicitly handle click events of views rendered to screen.
     *
     * This method is optional.
     */
    @Override
    public void handleClick(@Nullable final View view) { }

    /**
     * Your base native ad subclass should implement this method if the network requires the developer
     * to reset or clear state of the native ad after it goes off screen and before it is rendered
     * again.
     *
     * This method is optional.
     */
    @Override
    public void clear(@Nullable final View view) { }

    /**
     * Your base native ad subclass should implement this method if the network requires the developer
     * to destroy or cleanup their native ad when they are finished with it.
     *
     * This method is optional.
     */
    @Override
    public void destroy() { }

    // Event Notifiers
    /**
     * Notifies the SDK that the ad has been shown. This will cause the SDK to record an impression
     * for the ad. This is meant for network SDKs that expose their own impression tracking
     * callbacks, and requires that you call
     * {@link BaseForwardingNativeAd#setOverridingImpressionTracker} from your implementation of
     * {@link BaseForwardingNativeAd#prepare}.
     */
    protected final void notifyAdImpressed() {
        if (mNativeEventListener != null) {
            mNativeEventListener.onAdImpressed();
        }
    }

    /**
     * Notifies the SDK that the user has clicked the ad. This will cause the SDK to record an
     * click for the ad. This is meant for network SDKs that expose their own click
     * tracking callbacks, and requires that you call
     * {@link BaseForwardingNativeAd#setOverridingClickTracker} from your implementation of
     * {@link BaseForwardingNativeAd#prepare}.
     */
    protected final void notifyAdClicked() {
        if (mNativeEventListener != null) {
            mNativeEventListener.onAdClicked();
        }
    }

    /**
     * Pre caches the given set of image urls. We recommend using this method to warm the image
     * cache before calling {@link CustomEventNativeListener#onNativeAdLoaded}. Doing so will
     * force images to cache before displaying the ad.
     */
    static void preCacheImages(@NonNull final Context context,
            @NonNull final List<String> imageUrls,
            @NonNull final ImageListener imageListener) {
        final ImageLoader imageLoader = Networking.getImageLoader(context);
        // These Atomics are only accessed on the main thread.
        // We use Atomics here so we can change their values while keeping a reference for the inner class.
        final AtomicInteger imageCounter = new AtomicInteger(imageUrls.size());
        final AtomicBoolean anyFailures = new AtomicBoolean(false);
        ImageLoader.ImageListener volleyImageListener = new ImageLoader.ImageListener() {
            @Override
            public void onResponse(final ImageLoader.ImageContainer imageContainer, final boolean isImmediate) {
                // Image Loader returns a "default" response immediately. We want to ignore this
                // unless the image is already cached.
                if (imageContainer.getBitmap() != null) {
                    final int count = imageCounter.decrementAndGet();
                    if (count == 0 && !anyFailures.get()) {
                        imageListener.onImagesCached();
                    }
                }
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                MoPubLog.d("Failed to download a native ads image:", volleyError);
                boolean anyPreviousErrors = anyFailures.getAndSet(true);
                imageCounter.decrementAndGet();
                if (!anyPreviousErrors) {
                    imageListener.onImagesFailedToCache(NativeErrorCode.IMAGE_DOWNLOAD_FAILURE);
                }
            }
        };

        for (String url : imageUrls) {
            if (TextUtils.isEmpty(url)) {
                anyFailures.set(true);
                imageListener.onImagesFailedToCache(NativeErrorCode.IMAGE_DOWNLOAD_FAILURE);
                return;
            }
            imageLoader.get(url, volleyImageListener);
        }
    }
}
