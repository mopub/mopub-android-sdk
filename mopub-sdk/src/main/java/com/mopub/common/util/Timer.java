package com.mopub.common.util;

import java.util.concurrent.TimeUnit;

public class Timer {
    private enum State { STARTED, STOPPED }
    private long mStopTimeNanos;
    private long mStartTimeNanos;
    private State mState;

    public Timer() {
        mState = State.STOPPED;
    }

    public void start() {
        // System.nanoTime isn't affected by changing the system time
        mStartTimeNanos = System.nanoTime();
        mState = State.STARTED;
    }

    public void stop() {
        if (mState != State.STARTED) {
            throw new IllegalStateException("EventTimer was not started.");
        }
        mState = State.STOPPED;
        mStopTimeNanos = System.nanoTime();
    }

    public long getTime() {
        long endTime;
        if (mState == State.STARTED) {
            endTime = System.nanoTime();
        } else {
            endTime = mStopTimeNanos;
        }
        return TimeUnit.MILLISECONDS.convert(endTime - mStartTimeNanos, TimeUnit.NANOSECONDS);
    }
}
