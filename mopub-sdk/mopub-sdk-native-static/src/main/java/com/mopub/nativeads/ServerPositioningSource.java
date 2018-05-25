package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Constants;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.Networking;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.Response;
import com.mopub.volley.VolleyError;

/**
 * Requests positioning information from the MoPub ad server.
 *
 * The expected JSON format contains a set of rules for fixed and repeating positions. For example:
 * {
 *   fixed: [{
 *     position: 7
 *   }, {
 *     section : 1
 *     position: 6
 *   }],
 *   repeating:  {
 *     interval: 12
 *   }
 * }
 *
 * Both fixed and repeating rules are optional. If they exist they must follow the following
 * guidelines:
 *
 * fixed - contains a set of positioning objects, each with an optional section and a required
 * position. Section is used for iOS clients only, and non-zero sections are ignored on Android.
 *
 * repeating - contains a required interval, which must be 2 or greater.
 *
 * The JSON parsing logic treats any violations to the above spec as invalid,
 * rather than trying to continue with a partially valid response.
 */
class ServerPositioningSource implements PositioningSource {

    private static final double DEFAULT_RETRY_TIME_MILLISECONDS = 1000; // 1 second
    private static final double EXPONENTIAL_BACKOFF_FACTOR = 2;

    // We allow the retry limit to be set per-instance for testing, but it is always initialized
    // to this default.
    private static final int MAXIMUM_RETRY_TIME_MILLISECONDS = 5 * 60 * 1000; // 5 minutes.
    private int mMaximumRetryTimeMillis = MAXIMUM_RETRY_TIME_MILLISECONDS;

    @NonNull private final Context mContext;

    // Handler and runnable for retrying after a failed response.
    @NonNull private final Handler mRetryHandler;
    @NonNull private final Runnable mRetryRunnable;
    private final Response.Listener<MoPubClientPositioning> mPositioningListener;
    private final Response.ErrorListener mErrorListener;

    @Nullable private PositioningListener mListener;
    private int mRetryCount;
    @Nullable private String mRetryUrl;
    @Nullable private PositioningRequest mRequest;

    ServerPositioningSource(@NonNull final Context context) {
        mContext = context.getApplicationContext();

        mRetryHandler = new Handler();
        mRetryRunnable = new Runnable() {
            @Override
            public void run() {
                requestPositioningInternal();
            }
        };

        mPositioningListener = new Response.Listener<MoPubClientPositioning>() {
            @Override
            public void onResponse(final MoPubClientPositioning clientPositioning) {
                handleSuccess(clientPositioning);
            }
        };

        mErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                // Don't log a stack trace when we're just warming up.
                if (!(error instanceof MoPubNetworkError) ||
                        ((MoPubNetworkError) error).getReason().equals(MoPubNetworkError.Reason.WARMING_UP)) {
                    MoPubLog.e("Failed to load positioning data", error);
                    if (error.networkResponse == null && !DeviceUtils.isNetworkAvailable(mContext)) {
                        MoPubLog.c(String.valueOf(MoPubErrorCode.NO_CONNECTION.toString()));
                    }
                }

                handleFailure();
            }
        };
    }

    @Override
    public void loadPositions(@NonNull String adUnitId, @NonNull PositioningListener listener) {
        // If a request is in flight, remove it.
        if (mRequest != null) {
            mRequest.cancel();
            mRequest = null;
        }

        // If a retry is pending remove it.
        if (mRetryCount > 0) {
            mRetryHandler.removeCallbacks(mRetryRunnable);
            mRetryCount = 0;
        }

        mListener = listener;
        mRetryUrl = new PositioningUrlGenerator(mContext)
                .withAdUnitId(adUnitId)
                .generateUrlString(Constants.HOST);
        requestPositioningInternal();
    }

    private void requestPositioningInternal() {
        MoPubLog.d("Loading positioning from: " + mRetryUrl);

        mRequest = new PositioningRequest(mContext, mRetryUrl, mPositioningListener, mErrorListener);
        final RequestQueue requestQueue = Networking.getRequestQueue(mContext);
        requestQueue.add(mRequest);
    }

    private void handleSuccess(@NonNull MoPubClientPositioning positioning) {
        if (mListener != null) {
            mListener.onLoad(positioning);
        }
        mListener = null;
        mRetryCount = 0;
    }

    private void handleFailure() {
        double multiplier = Math.pow(EXPONENTIAL_BACKOFF_FACTOR, mRetryCount + 1);
        int delay = (int) (DEFAULT_RETRY_TIME_MILLISECONDS * multiplier);
        if (delay >= mMaximumRetryTimeMillis) {
            MoPubLog.d("Error downloading positioning information");
            if (mListener != null) {
                mListener.onFailed();
            }
            mListener = null;
            return;
        }

        mRetryCount++;
        mRetryHandler.postDelayed(mRetryRunnable, delay);
    }

    @Deprecated
    @VisibleForTesting
    void setMaximumRetryTimeMilliseconds(int millis) {
        mMaximumRetryTimeMillis = millis;
    }
}
