package com.mopub.common;

import android.os.SystemClock;
import android.support.annotation.NonNull;

import com.mopub.common.logging.MoPubLog;

/**
 * This class uses system time to track durations. It can be started and paused, but not reset.
 * Create a new {@link DoubleTimeTracker} if you need to track a new duration.
 */
public class DoubleTimeTracker {
    /**
     * Wrapper for Android SystemClock used to allow dependency injection for tests.
     */
    public interface Clock {
        long elapsedRealTime();
    }


    private enum State { STARTED, PAUSED }
    @NonNull private volatile State state;
    private long startedTimestamp;
    private long interval;
    @NonNull private final Clock mClock;

    public DoubleTimeTracker() {
        this(new SystemClockClock());
    }

    @VisibleForTesting
    public DoubleTimeTracker(@NonNull Clock clock) {
        this.mClock = clock;
        this.state = State.PAUSED;
    }

    public synchronized void start() {
        if (state == State.STARTED) {
            MoPubLog.v("DoubleTimeTracker already started.");
            return;
        }

        state = State.STARTED;
        startedTimestamp = mClock.elapsedRealTime();
    }

    public synchronized void pause() {
        if (state == State.PAUSED) {
            MoPubLog.v("DoubleTimeTracker already paused.");
            return;
        }


        interval += computeIntervalDiff();
        startedTimestamp = 0;
        state = State.PAUSED;
    }


    public synchronized double getInterval() {
        return interval + computeIntervalDiff();
    }

    private synchronized long computeIntervalDiff() {
        if (state == State.PAUSED) {
            return 0;
        }

        return mClock.elapsedRealTime() - startedTimestamp;
    }

    private static class SystemClockClock implements Clock {
        public long elapsedRealTime() {
            return SystemClock.elapsedRealtime();
        }
    }
}
