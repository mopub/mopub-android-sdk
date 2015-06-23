package com.mopub.common.event;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.Random;

/**
 * Samples events based on rules defined in the sample method.
 */
public class EventSampler {

    @NonNull private Random mRandom;

    public EventSampler() {
        this(new Random());
    }

    @VisibleForTesting
    public EventSampler(@NonNull Random random) {
        mRandom = random;
    }

    /**
     * Samples events based on custom rules.
     *
     * @param baseEvent The event to be sampled.
     *
     * @return Will return {@code true} if the event passed sampling and {@code false}
     * if it is to be discarded.
     */
    boolean sample(@NonNull BaseEvent baseEvent) {
        Preconditions.checkNotNull(baseEvent);

        return mRandom.nextDouble() < baseEvent.getSamplingRate();
    }
}
