package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Certified with Flurry 8.1.0
 */
public interface FlurryBaseNativeAd {

    void fetchAd();
    @NonNull List<String> getImageUrls();
    boolean isAppInstallAd();
    void precacheImages();

    @Nullable String getTitle();
    @Nullable String getText();
    @Nullable String getCallToAction();
    @Nullable String getMainImageUrl();
    @Nullable String getIconImageUrl();
    @Nullable Double getStarRating();
    @Nullable Map<String, Object> getExtras();


    void setTitle(@Nullable final String title);
    void setText(@Nullable final String text);
    void setCallToAction(@Nullable final String callToAction);
    void setMainImageUrl(@Nullable final String mainImageUrl);
    void setIconImageUrl(@Nullable final String iconImageUrl);
    void setStarRating(@Nullable final Double starRating);
    void addExtra(@NonNull final String key, @Nullable final Object value);

    void onNativeAdLoaded();
}
