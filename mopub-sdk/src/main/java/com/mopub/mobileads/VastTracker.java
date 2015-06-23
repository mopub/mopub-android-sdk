package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.io.Serializable;

/**
 * State encapsulation for VAST tracking URLs that should only be called once. For example, progress
 * trackers are only called once.
 */
public class VastTracker implements Serializable {
    private static final long serialVersionUID = 0L;
    @NonNull protected final String mTrackingUrl;
    private boolean mCalled;

    public VastTracker(@NonNull String trackingUrl) {
        Preconditions.checkNotNull(trackingUrl);
        mTrackingUrl = trackingUrl;
    }

    @NonNull
    public String getTrackingUrl() {
        return mTrackingUrl;
    }

    public void setTracked() {
        mCalled = true;
    }

    public boolean isTracked() {
        return mCalled;
    }
}
