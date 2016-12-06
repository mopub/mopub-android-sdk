package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.io.Serializable;

/**
 * State encapsulation for VAST tracking URLs that may or may not only be called once. For example,
 * progress trackers are only called once, but error trackers are repeatable.
 */
public class VastTracker implements Serializable {
    private static final long serialVersionUID = 0L;
    @NonNull protected final String mTrackingUrl;
    private boolean mCalled;
    private boolean mIsRepeatable;

    public VastTracker(@NonNull String trackingUrl) {
        Preconditions.checkNotNull(trackingUrl);
        mTrackingUrl = trackingUrl;
    }

    public VastTracker(@NonNull String trackingUrl, boolean isRepeatable) {
        this(trackingUrl);
        mIsRepeatable = isRepeatable;
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

    public boolean isRepeatable() {
        return mIsRepeatable;
    }
}
