package com.mopub.mobileads;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.network.Networking;
import com.mopub.volley.DefaultRetryPolicy;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.VolleyError;

/**
 * Handles the server-to-server rewarded video completion handshake.
 */
public class RewardedVideoCompletionRequestHandler implements
        RewardedVideoCompletionRequest.RewardedVideoCompletionRequestListener {

    /**
     * Request timeouts. Use the last value if the current retry is higher than the number of values
     * in this list.
     */
    static final int[] RETRY_TIMES = {5000, 10000, 20000, 40000, 60000};

    /**
     * The actual request should take a little shorter to have the runnable run at the set time and
     * have the previous request finish.
     */
    static final int REQUEST_TIMEOUT_DELAY = 1000;

    static final int MAX_RETRIES = 17;
    private static final String CUSTOMER_ID_KEY = "&customer_id=";
    private static final String SDK_VERSION_KEY = "&nv=";
    private static final String API_VERSION_KEY = "&v=";
    private static final String REWARD_NAME_KEY = "&rcn=";
    private static final String REWARD_AMOUNT_KEY = "&rca=";
    private static final String CUSTOM_EVENT_CLASS_NAME_KEY = "&cec=";
    private static final String CUSTOM_DATA_KEY = "&rcd=";

    @NonNull private final String mUrl;
    @NonNull private final Handler mHandler;
    @NonNull private final RequestQueue mRequestQueue;
    private int mRetryCount;
    private volatile boolean mShouldStop;

    RewardedVideoCompletionRequestHandler(@NonNull final Context context,
            @NonNull final String url,
            @Nullable final String customerId,
            @NonNull final String rewardName,
            @NonNull final String rewardAmount,
            @Nullable final String className,
            @Nullable final String customData) {
        this(context, url, customerId, rewardName, rewardAmount, className, customData,
                new Handler());
    }

    @VisibleForTesting
    RewardedVideoCompletionRequestHandler(@NonNull final Context context,
            @NonNull final String url,
            @Nullable final String customerId,
            @NonNull final String rewardName,
            @NonNull final String rewardAmount,
            @Nullable final String className,
            @Nullable final String customData,
            @NonNull final Handler handler) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(rewardName);
        Preconditions.checkNotNull(rewardAmount);
        Preconditions.checkNotNull(handler);

        mUrl = appendParameters(url, customerId, rewardName, rewardAmount, className, customData);
        mRetryCount = 0;
        mHandler = handler;
        mRequestQueue = Networking.getRequestQueue(context);
    }

    void makeRewardedVideoCompletionRequest() {
        if (mShouldStop) {
            // If we make a successful request, cancel all pending requests, and don't make more.
            mRequestQueue.cancelAll(mUrl);
            return;
        }

        final RewardedVideoCompletionRequest rewardedVideoCompletionRequest =
                new RewardedVideoCompletionRequest(mUrl,
                        new DefaultRetryPolicy(getTimeout(mRetryCount) - REQUEST_TIMEOUT_DELAY,
                                0, 0f), this);
        rewardedVideoCompletionRequest.setTag(mUrl);
        mRequestQueue.add(rewardedVideoCompletionRequest);

        if (mRetryCount >= MAX_RETRIES) {
            MoPubLog.d("Exceeded number of retries for rewarded video completion request.");
            return;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                makeRewardedVideoCompletionRequest();
            }
        }, getTimeout(mRetryCount));
        mRetryCount++;
    }

    @Override
    public void onResponse(final Integer response) {
        // Only consider it a failure if we get a 5xx status code.
        if (response != null && !(response >= 500 && response < 600)) {
            mShouldStop = true;
        }
    }

    @Override
    public void onErrorResponse(final VolleyError volleyError) {
        if (volleyError != null && volleyError.networkResponse != null &&
                !(volleyError.networkResponse.statusCode >= 500
                        && volleyError.networkResponse.statusCode < 600)) {
            mShouldStop = true;
        }
    }

    public static void makeRewardedVideoCompletionRequest(@Nullable final Context context,
            @Nullable final String url,
            @Nullable final String customerId,
            @NonNull final String rewardName,
            @NonNull final String rewardAmount,
            @Nullable final String rewardedAd,
            @Nullable final String customData) {
        if (context == null || TextUtils.isEmpty(url) || rewardName == null ||
                rewardAmount == null) {
            return;
        }

        new RewardedVideoCompletionRequestHandler(context, url, customerId, rewardName,
                rewardAmount, rewardedAd, customData)
                .makeRewardedVideoCompletionRequest();
    }

    static int getTimeout(int retryCount) {
        if (retryCount >= 0 && retryCount < RETRY_TIMES.length) {
            return RETRY_TIMES[retryCount];
        } else {
            return RETRY_TIMES[RETRY_TIMES.length - 1];
        }
    }

    private static String appendParameters(@NonNull final String url,
            @Nullable final String customerId,
            @NonNull final String rewardName,
            @NonNull final String rewardAmount,
            @Nullable final String className,
            @Nullable final String customData) {
        Preconditions.checkNotNull(url);
        Preconditions.checkNotNull(rewardName);
        Preconditions.checkNotNull(rewardAmount);

        final StringBuilder stringBuilder = new StringBuilder(url);
        stringBuilder
                .append(CUSTOMER_ID_KEY).append((customerId == null) ? "" : Uri.encode(customerId))
                .append(REWARD_NAME_KEY).append(Uri.encode(rewardName))
                .append(REWARD_AMOUNT_KEY).append(Uri.encode(rewardAmount))
                .append(SDK_VERSION_KEY).append(Uri.encode(MoPub.SDK_VERSION))
                // Does not need to be encoded as it's an int
                .append(API_VERSION_KEY).append(MoPubRewardedVideoManager.API_VERSION)
                .append(CUSTOM_EVENT_CLASS_NAME_KEY)
                .append((className == null) ? "" : Uri.encode(className));

        if (!TextUtils.isEmpty(customData)) {
            stringBuilder.append(CUSTOM_DATA_KEY).append(Uri.encode(customData));
        }

        return stringBuilder.toString();
    }

    @VisibleForTesting
    @Deprecated
    boolean getShouldStop() {
        return mShouldStop;
    }

    @VisibleForTesting
    @Deprecated
    int getRetryCount() {
        return mRetryCount;
    }

    @VisibleForTesting
    @Deprecated
    void setRetryCount(int retryCount) {
        mRetryCount = retryCount;
    }
}
