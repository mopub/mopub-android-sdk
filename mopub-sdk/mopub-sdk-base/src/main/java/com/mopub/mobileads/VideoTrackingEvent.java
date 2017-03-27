package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Internal Video Tracking events, defined in ad server
 */
enum VideoTrackingEvent {
    START("start"),
    FIRST_QUARTILE("firstQuartile"),
    MIDPOINT("midpoint"),
    THIRD_QUARTILE("thirdQuartile"),
    COMPLETE("complete"),
    COMPANION_AD_VIEW("companionAdView"),
    COMPANION_AD_CLICK("companionAdClick"),
    UNKNOWN("");

    private final String name;

    VideoTrackingEvent(@NonNull final String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public static VideoTrackingEvent fromString(@Nullable final String name) {
        if (name == null) {
            return UNKNOWN;
        }

        for (VideoTrackingEvent event : VideoTrackingEvent.values()) {
            if (name.equals(event.getName())) {
                return event;
            }
        }

        return UNKNOWN;
    }
}
