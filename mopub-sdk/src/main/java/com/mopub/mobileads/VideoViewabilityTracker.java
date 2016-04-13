package com.mopub.mobileads;

import android.support.annotation.NonNull;

public class VideoViewabilityTracker extends VastTracker {
    private final int mViewablePlaytimeMS;
    private final int mPercentViewable;

    public VideoViewabilityTracker(final int viewablePlaytimeMS, final int percentViewable,
            @NonNull final String trackerUrl) {
        super(trackerUrl);
        mViewablePlaytimeMS = viewablePlaytimeMS;
        mPercentViewable = percentViewable;
    }

    public int getViewablePlaytimeMS() {
        return mViewablePlaytimeMS;
    }

    public int getPercentViewable() {
        return mPercentViewable;
    }
}
