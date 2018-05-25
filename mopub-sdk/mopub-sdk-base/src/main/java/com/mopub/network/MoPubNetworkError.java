package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.volley.NetworkResponse;
import com.mopub.volley.VolleyError;

public class MoPubNetworkError extends VolleyError {
    public enum Reason {
        WARMING_UP,
        NO_FILL,
        BAD_HEADER_DATA,
        BAD_BODY,
        TRACKING_FAILURE,
        UNSPECIFIED
    }

    @NonNull private final Reason mReason;
    @Nullable private final Integer mRefreshTimeMillis;

    public MoPubNetworkError(@NonNull Reason reason) {
        super();
        mReason = reason;
        mRefreshTimeMillis = null;
    }

    public MoPubNetworkError(@NonNull NetworkResponse networkResponse, @NonNull Reason reason) {
        super(networkResponse);
        mReason = reason;
        mRefreshTimeMillis = null;
    }

    public MoPubNetworkError(@NonNull Throwable cause, @NonNull Reason reason) {
        super(cause);
        mReason = reason;
        mRefreshTimeMillis = null;
    }

    public MoPubNetworkError(@NonNull String message, @NonNull Reason reason) {
        this(message, reason, null);
    }

    public MoPubNetworkError(@NonNull String message, @NonNull Throwable cause, @NonNull Reason reason) {
        super(message, cause);
        mReason = reason;
        mRefreshTimeMillis = null;
    }

    public MoPubNetworkError(@NonNull String message, @NonNull Reason reason,
            @Nullable Integer refreshTimeMillis) {
        super(message);
        mReason = reason;
        mRefreshTimeMillis = refreshTimeMillis;
    }

    @NonNull
    public Reason getReason() {
        return mReason;
    }

    @Nullable
    public Integer getRefreshTimeMillis() {
        return mRefreshTimeMillis;
    }
}
