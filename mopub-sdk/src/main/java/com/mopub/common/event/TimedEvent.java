package com.mopub.common.event;

import com.mopub.common.ClientMetadata;

import java.util.concurrent.TimeUnit;

/**
 * Immutable data class with client event data, including a duration.
 */
public class TimedEvent extends BaseEvent {
    interface Listener {
        public void onStopped(TimedEvent event);

        public void onCancelled(TimedEvent event);
    }

    /**
     * Use this status code when stopping an event where an HTTP Status Code isn't relevant.
     */
    public static final int SC_NOT_RELEVANT = 0;

    /**
     * Use this status code when the timed event has no response.
     */
    public static final int SC_NO_RESPONSE = -1;

    private final long mStartTimeNanos;
    private enum State {
        WAITING, STOPPED, CANCELLED
    }
    private State mState;
    private final Listener mListener;


    private long mDurationMillis;
    private int mHttpStatusCode;

    TimedEvent(final Type eventType, final String requestUrl,
            final ClientMetadata clientMetadata, Listener listener) {
        super(eventType, requestUrl, clientMetadata);
        // System.nanoTime isn't affected by changing the system time, so we use it here.
        mState = State.WAITING;
        mStartTimeNanos = System.nanoTime();
        mListener = listener;
    }

    public synchronized void stop(int httpStatusCode) {
        if (mState == State.WAITING) {
            mState = State.STOPPED;
            mHttpStatusCode = httpStatusCode;
            long stopTimeNanos = System.nanoTime();
            mDurationMillis = TimeUnit.MILLISECONDS.convert(stopTimeNanos - mStartTimeNanos,
                    TimeUnit.NANOSECONDS);
            if (mListener != null) {
                mListener.onStopped(this);
            }
        }
    }

    public synchronized void cancel() {
        if (mState == State.WAITING) {
            mState = State.CANCELLED;
            if (mListener != null) {
                mListener.onCancelled(this);
            }
        }
    }

    public final synchronized long getDurationMillis() {
        return mDurationMillis;
    }

    public final synchronized int getHttpStatusCode() {
        return mHttpStatusCode;
    }
}
