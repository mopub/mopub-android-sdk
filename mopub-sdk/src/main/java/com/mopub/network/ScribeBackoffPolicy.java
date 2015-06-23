package com.mopub.network;

import com.mopub.common.VisibleForTesting;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.VolleyError;

/**
 * The backoff policy for making requests to the Scribe service.
 */
public class ScribeBackoffPolicy extends BackoffPolicy {
    private static final int DEFAULT_BACKOFF_TIME_MS = 60 * 1000;
    private static final int MAX_RETRIES = 5;
    private static final int BACKOFF_MULTIPLIER = 2;

    public ScribeBackoffPolicy() {
        this(DEFAULT_BACKOFF_TIME_MS, MAX_RETRIES, BACKOFF_MULTIPLIER);
    }

    @VisibleForTesting
    ScribeBackoffPolicy(int defaultBackoffTimeMs, int maxRetries, int backoffMultiplier) {
        mDefaultBackoffTimeMs = defaultBackoffTimeMs;
        mMaxRetries = maxRetries;
        mBackoffMultiplier = backoffMultiplier;
    }

    @Override
    public void backoff(VolleyError volleyError) throws VolleyError {
        if (!hasAttemptRemaining()) {
            throw volleyError;
        }

        if (volleyError instanceof NoConnectionError) {
            updateBackoffTime();
            return;
        }

        NetworkResponse networkResponse = volleyError.networkResponse;
        if (networkResponse != null &&
                (networkResponse.statusCode == 503  || networkResponse.statusCode == 504)) {
            updateBackoffTime();
            return;
        }

        throw volleyError;
    }

    private void updateBackoffTime() {
        double multiplier = Math.pow(mBackoffMultiplier, mRetryCount);
        mBackoffMs = (int) (mDefaultBackoffTimeMs * multiplier);
        mRetryCount++;
    }
}

