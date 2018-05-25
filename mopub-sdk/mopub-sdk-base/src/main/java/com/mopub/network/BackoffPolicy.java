package com.mopub.network;

import com.mopub.volley.VolleyError;

/**
 * The backoff policy for a request.
 */
public abstract class BackoffPolicy {
    protected int mBackoffMs;
    protected int mBackoffMultiplier;
    protected int mDefaultBackoffTimeMs;
    protected int mMaxBackoffTimeMs;
    protected int mRetryCount;
    protected int mMaxRetries;

    /**
     * Prepares for the next request attempt by updating the backoff time.
     *
     * @param volleyError The error code of the last request attempt.
     */
    public abstract void backoff(VolleyError volleyError) throws VolleyError;

    /**
     * Returns the current backoff time in ms.
     */
    public int getBackoffMs() {
        return mBackoffMs;
    }

    /**
     * Returns the current retry count.
     */
    public int getRetryCount() {
        return mRetryCount;
    }

    /**
     * Returns true if this policy has attempts remaining, false otherwise.
     */
    public boolean hasAttemptRemaining() {
        return mRetryCount < mMaxRetries;
    }
}
