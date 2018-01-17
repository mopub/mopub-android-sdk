package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.Preconditions.NoThrow;
import com.mopub.common.logging.MoPubLog;

import java.util.HashMap;
import java.util.Map;

/**
 * This the base class for implementations of the static native ad format.
 */
public abstract class StaticNativeAd extends BaseNativeAd implements ImpressionInterface, ClickInterface {
    private static final int DEFAULT_IMPRESSION_MIN_TIME_VIEWED_MS = 1000;
    private static final int DEFAULT_IMPRESSION_MIN_PERCENTAGE_VIEWED = 50;

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
    @Nullable private String mPrivacyInformationIconClickThroughUrl;
    @Nullable private String mPrivacyInformationIconImageUrl;

    // Impression logistics
    private boolean mImpressionRecorded;
    private int mImpressionMinTimeViewed;
    private int mImpressionMinPercentageViewed;
    private Integer mImpressionMinVisiblePx;

    // Extras
    @NonNull private final Map<String, Object> mExtras;

    public StaticNativeAd() {
        mImpressionMinTimeViewed = DEFAULT_IMPRESSION_MIN_TIME_VIEWED_MS;
        mImpressionMinPercentageViewed = DEFAULT_IMPRESSION_MIN_PERCENTAGE_VIEWED;
        mImpressionMinVisiblePx = null;

        mExtras = new HashMap<String, Object>();
    }

    // Getters
    /**
     * Returns the String corresponding to the ad's title.
     */
    @Nullable
    final public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the String corresponding to the ad's body text.
     */
    @Nullable
    final public String getText() {
        return mText;
    }
    /**
     * Returns the String url corresponding to the ad's main image.
     */
    @Nullable
    final public String getMainImageUrl() {
        return mMainImageUrl;
    }

    /**
     * Returns the String url corresponding to the ad's icon image.
     */
    @Nullable
    final public String getIconImageUrl() {
        return mIconImageUrl;
    }

    /**
     * Returns the Call To Action String (i.e. "Download" or "Learn More") associated with this ad.
     */
    @Nullable
    final public String getCallToAction() {
        return mCallToAction;
    }

    /**
     * For app install ads, this returns the associated star rating (on a 5 star scale) for the
     * advertised app. Note that this method may return null if the star rating was either never set
     * or invalid.
     */
    @Nullable
    final public Double getStarRating() {
        return mStarRating;
    }

    /**
     * Returns the Privacy Information click through url. No Privacy Information Icon will be shown
     * unless this is set to something non-null using {@link #setPrivacyInformationIconClickThroughUrl(String)}.
     *
     * @return String representing the Privacy Information Icon click through url, or {@code null}
     * if not set.
     */
    @Nullable
    final public String getPrivacyInformationIconClickThroughUrl() {
        return mPrivacyInformationIconClickThroughUrl;
    }

    /**
     * Returns the Privacy Information image url.
     *
     * @return String representing the Privacy Information Icon image url, or {@code null} if not
     * set.
     */
    @Nullable
    public String getPrivacyInformationIconImageUrl() {
        return mPrivacyInformationIconImageUrl;
    }

    // Extras Getters
    /**
     * Given a particular String key, return the associated Object value from the ad's extras map.
     * See {@link StaticNativeAd#getExtras()} for more information.
     */
    @Nullable
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
    final public Map<String, Object> getExtras() {
        return new HashMap<String, Object>(mExtras);
    }

    /**
     * Returns the String url that the device will attempt to resolve when the ad is clicked.
     */
    @Nullable
    final public String getClickDestinationUrl() {
        return mClickDestinationUrl;
    }

    final public void setMainImageUrl(@Nullable final String mainImageUrl) {
        mMainImageUrl = mainImageUrl;
    }

    final public void setIconImageUrl(@Nullable final String iconImageUrl) {
        mIconImageUrl = iconImageUrl;
    }

    final public void setClickDestinationUrl(@Nullable final String clickDestinationUrl) {
        mClickDestinationUrl = clickDestinationUrl;
    }

    final public void setCallToAction(@Nullable final String callToAction) {
        mCallToAction = callToAction;
    }

    final public void setTitle(@Nullable final String title) {
        mTitle = title;
    }

    final public void setText(@Nullable final String text) {
        mText = text;
    }

    final public void setStarRating(@Nullable final Double starRating) {
        if (starRating == null) {
            mStarRating = null;
        } else if (starRating >= MIN_STAR_RATING && starRating <= MAX_STAR_RATING) {
            mStarRating = starRating;
        } else {
            MoPubLog.d("Ignoring attempt to set invalid star rating (" + starRating + "). Must be "
                    + "between " + MIN_STAR_RATING + " and " + MAX_STAR_RATING + ".");
        }
    }

    final public void setPrivacyInformationIconClickThroughUrl(
            @Nullable final String privacyInformationIconClickThroughUrl) {
        mPrivacyInformationIconClickThroughUrl = privacyInformationIconClickThroughUrl;
    }

    final public void setPrivacyInformationIconImageUrl(
            @Nullable String privacyInformationIconImageUrl) {
        mPrivacyInformationIconImageUrl = privacyInformationIconImageUrl;
    }

    final public void addExtra(@NonNull final String key, @Nullable final Object value) {
        if (!NoThrow.checkNotNull(key, "addExtra key is not allowed to be null")) {
            return;
        }
        mExtras.put(key, value);
    }

    /**
     * Sets the minimum time for the ad to be on screen before impression trackers are fired.
     * This int must be greater than 0.
     *
     * @param impressionMinTimeViewed Time in milliseconds (ignored if negative or 0).
     */
    final public void setImpressionMinTimeViewed(final int impressionMinTimeViewed) {
        if (impressionMinTimeViewed > 0) {
            mImpressionMinTimeViewed = impressionMinTimeViewed;
        } else {
            MoPubLog.d("Ignoring non-positive impressionMinTimeViewed: " + impressionMinTimeViewed);
        }
    }

    /**
     * Sets the minimum percent of the ad to be on screen before impression trackers are fired.
     * This must be a percentage between 0 and 100, inclusive.
     *
     * @param impressionMinPercentageViewed Percent of ad (must be between 0 and 100 inclusive).
     */
    final public void setImpressionMinPercentageViewed(final int impressionMinPercentageViewed) {
        if (impressionMinPercentageViewed >= 0 && impressionMinPercentageViewed <= 100) {
            mImpressionMinPercentageViewed = impressionMinPercentageViewed;
        } else {
            MoPubLog.d("Ignoring impressionMinTimeViewed that's not a percent [0, 100]: " +
                    impressionMinPercentageViewed);
        }
    }

    /**
     * Sets the minimum number of pixels of the ad to be on screen before impression trackers are
     * fired. This must be an Integer greater than 0.
     *
     * @param impressionMinVisiblePx Number of pixels of an ad (ignored if negative or 0).
     */
    final public void setImpressionMinVisiblePx(@Nullable final Integer impressionMinVisiblePx) {
        if (impressionMinVisiblePx != null && impressionMinVisiblePx > 0) {
            mImpressionMinVisiblePx = impressionMinVisiblePx;
        } else {
            MoPubLog.d("Ignoring null or non-positive impressionMinVisiblePx: " +
                    impressionMinVisiblePx);
        }
    }

    // Lifecycle Handlers
    @Override
    public void prepare(@NonNull final View view) { }

    @Override
    public void clear(@NonNull final View view) { }

    @Override
    public void destroy() { }

    // Event Handlers
    /**
     * Your {@link StaticNativeAd} subclass should implement this method if the network requires the developer
     * to explicitly record an impression of a view rendered to screen.
     *
     * This method is optional.
     */
    @Override
    public void recordImpression(@NonNull final View view) { }

    /**
     * Returns the minimum viewable percentage of the ad that must be onscreen for it to be
     * considered visible. See {@link StaticNativeAd#getImpressionMinTimeViewed()} for
     * additional impression tracking considerations.
     */
    @Override
    final public int getImpressionMinPercentageViewed() {
        return mImpressionMinPercentageViewed;
    }

    /**
     * Returns the minimum amount of time (in milliseconds) the ad that must be onscreen before an
     * impression is recorded. See {@link StaticNativeAd#getImpressionMinPercentageViewed()}
     * for additional impression tracking considerations.
     */
    @Override
    final public int getImpressionMinTimeViewed() {
        return mImpressionMinTimeViewed;
    }

    /**
     * Returns the minimum viewable number of pixels of the ad that must be onscreen for it to be
     * considered visible. This value, if present and positive will override the min percentage.
     * See {@link StaticNativeAd#getImpressionMinTimeViewed()} for additional impression
     * tracking considerations.
     */
    @Override
    final public Integer getImpressionMinVisiblePx() {
        return mImpressionMinVisiblePx;
    }

    @Override
    final public boolean isImpressionRecorded() {
        return mImpressionRecorded;
    }

    @Override
    final public void setImpressionRecorded() {
        mImpressionRecorded = true;
    }

    /**
     * Your {@link StaticNativeAd} subclass should implement this method if the network requires the developer
     * to explicitly handle click events of views rendered to screen.
     *
     * This method is optional.
     */
    @Override
    public void handleClick(@NonNull final View view) { }
}
