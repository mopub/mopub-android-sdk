package com.mopub.common;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;


/**
 * Aggregates sdk initialization listeners so that the listener only fires once everything is done.
 */
class CompositeSdkInitializationListener implements SdkInitializationListener {

    @NonNull private final SdkInitializationListener mSdkInitializationListener;
    private int mTimes;

    /**
     * Don't fire onInitializationFinished until the requisite number of times of
     * onInitializationFinished has been called.
     *
     * @param sdkInitializationListener The original listener.
     * @param times                     Number of times to expect onInitializationFinished() to be called.
     */
    public CompositeSdkInitializationListener(
            @NonNull final SdkInitializationListener sdkInitializationListener, int times) {
        Preconditions.checkNotNull(sdkInitializationListener);

        mSdkInitializationListener = sdkInitializationListener;
        mTimes = times;
    }

    @Override
    public void onInitializationFinished() {
        mTimes--;
        if (mTimes <= 0) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mSdkInitializationListener.onInitializationFinished();
                }
            });
        }
    }
}
