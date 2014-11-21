package com.mopub.nativeads;

import android.view.View;

import java.util.Map;
import java.util.Set;

import static com.mopub.nativeads.BaseForwardingNativeAd.NativeEventListener;

interface NativeAdInterface {
    // Getters
    String getMainImageUrl();
    String getIconImageUrl();
    String getClickDestinationUrl();
    String getCallToAction();
    String getTitle();
    String getText();
    Double getStarRating();

    Set<String> getImpressionTrackers();
    int getImpressionMinPercentageViewed();
    int getImpressionMinTimeViewed();

    boolean isOverridingClickTracker();
    boolean isOverridingImpressionTracker();

    // Extras Getters
    Object getExtra(final String key);
    Map<String, Object> getExtras();

    // Setters
    void setNativeEventListener(final NativeEventListener nativeEventListener);

    // Event Handlers
    void prepare(final View view);
    void recordImpression();
    void handleClick(final View view);
    void clear(final View view);
    void destroy();
}
