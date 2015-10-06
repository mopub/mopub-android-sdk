package com.mopub.nativeads;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.Map;
import java.util.Set;

import static com.mopub.nativeads.BaseForwardingNativeAd.NativeEventListener;

interface NativeAdInterface {
    // Getters
    @Nullable
    String getMainImageUrl();

    @Nullable
    String getIconImageUrl();

    @Nullable
    String getClickDestinationUrl();

    @Nullable
    String getCallToAction();

    @Nullable
    String getTitle();

    @Nullable
    String getText();

    @Nullable
    Double getStarRating();

    @Nullable
    Drawable getDefaultIcon();

    String getDaaIconClickthroughUrl();

    @NonNull
    Set<String> getImpressionTrackers();

    int getImpressionMinPercentageViewed();

    int getImpressionMinTimeViewed();

    boolean isOverridingClickTracker();

    boolean isOverridingImpressionTracker();

    // Extras Getters
    @Nullable
    Object getExtra(final String key);

    @NonNull
    Map<String, Object> getExtras();

    // Setters
    void setNativeEventListener(@Nullable final NativeEventListener nativeEventListener);

    // Event Handlers
    void prepare(@NonNull final View view,@Nullable ViewBinder viewBinder);
    void recordImpression();
    void handleClick(@Nullable final View view);
    void clear(@NonNull final View view);
    void destroy();
}
