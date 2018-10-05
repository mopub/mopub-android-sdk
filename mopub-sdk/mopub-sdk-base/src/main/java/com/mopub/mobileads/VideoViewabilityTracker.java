// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

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
