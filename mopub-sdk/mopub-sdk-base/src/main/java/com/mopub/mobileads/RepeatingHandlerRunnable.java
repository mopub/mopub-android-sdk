package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

/**
 * A generic runnable that handles scheduling itself periodically on a Handler and stops when
 * requested.
 */
public abstract class RepeatingHandlerRunnable implements Runnable {
    @NonNull protected final Handler mHandler;
    private volatile boolean mIsRunning;
    protected volatile long mUpdateIntervalMillis;

    public RepeatingHandlerRunnable(@NonNull final Handler handler) {
        Preconditions.checkNotNull(handler);
        mHandler = handler;
    }

    @Override
    public void run() {
        if (mIsRunning) {
            doWork();
            mHandler.postDelayed(this, mUpdateIntervalMillis);
        }
    }

    public abstract void doWork();

    /**
     * Start this runnable immediately, repeating at the provided interval.
     */
    public void startRepeating(long intervalMillis) {
        Preconditions.checkArgument(intervalMillis > 0, "intervalMillis must be greater than 0. " +
                "Saw: %d", intervalMillis);
        mUpdateIntervalMillis = intervalMillis;
        if (!mIsRunning) {
            mIsRunning = true;
            mHandler.post(this);
        }
    }

    /**
     * Stop this repeating runnable.
     */
    public void stop() {
        mIsRunning = false;
    }

    @Deprecated
    @VisibleForTesting
    public boolean isRunning() {
        return mIsRunning;
    }
}
