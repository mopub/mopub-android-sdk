package com.mopub.common.event;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Samples events based on rules defined in the sample method.
 */
public class EventSampler {

    @VisibleForTesting static final int MAX_SIZE = 100;
    private static final float LOAD_FACTOR = 0.75f;
    /**
     * The capacity is just large enough to hold the max size without rehashing.
     */
    private static final int CAPACITY = (int) (MAX_SIZE / LOAD_FACTOR + 2);

    @NonNull private Random mRandom;
    @NonNull private LinkedHashMap<String, Boolean> mSampleDecisionsCache;

    public EventSampler() {
        this(new Random());
    }

    @VisibleForTesting
    public EventSampler(@NonNull Random random) {
        mRandom = random;
        mSampleDecisionsCache = new LinkedHashMap<String, Boolean>(CAPACITY, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_SIZE;
            }
        };
    }

    /**
     * Samples events based on custom rules. Events with the same request ID will either all pass or
     * be discarded together.
     *
     * @param baseEvent The event to be sampled.
     * @return Will return {@code true} if the event passed sampling and {@code false} if it is to
     * be discarded.
     */
    boolean sample(@NonNull BaseEvent baseEvent) {
        Preconditions.checkNotNull(baseEvent);

        final String requestId = baseEvent.getRequestId();
        if (requestId == null) {
            return mRandom.nextDouble() < baseEvent.getSamplingRate();
        }

        final Boolean existingSample = mSampleDecisionsCache.get(requestId);
        if (existingSample != null) {
            return existingSample;
        }
        final boolean newSample = mRandom.nextDouble() < baseEvent.getSamplingRate();
        mSampleDecisionsCache.put(requestId, newSample);
        return newSample;
    }

    @VisibleForTesting
    int getCacheSize() {
        return mSampleDecisionsCache.size();
    }
}
