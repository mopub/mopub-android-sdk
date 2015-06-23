package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

import java.io.Serializable;
import java.util.Locale;

/**
 * A Vast tracking URL with a "fractional" tracking threshold on the interval [0.0, 1.0].
 * The tracker should be triggered after the given fraction of the video has been played.
 */
public class VastFractionalProgressTracker extends VastTracker implements Comparable<VastFractionalProgressTracker>, Serializable {
    private static final long serialVersionUID = 0L;
    private final float mFraction;

    public VastFractionalProgressTracker(@NonNull final String trackingUrl, float trackingFraction) {
        super(trackingUrl);
        Preconditions.checkArgument(trackingFraction >= 0);
        mFraction = trackingFraction;
    }

    public float trackingFraction() {
        return mFraction;
    }

    @Override
    public int compareTo(@NonNull final VastFractionalProgressTracker other) {
        float you = other.trackingFraction();
        float me = trackingFraction();

        return Double.compare(me, you);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%2f: %s", mFraction, mTrackingUrl);
    }
}
