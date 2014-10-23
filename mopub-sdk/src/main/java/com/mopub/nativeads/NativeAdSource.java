package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener;

/**
 * An ad source responsible for requesting ads from the MoPub ad server.
 *
 * The ad source utilizes a cache to store ads, which allows ads to be immediately visible when
 * scrolling through a stream rather than "snapping" in when loaded. The cache is implemented as
 * a queue, so that the first ad loaded from the server will be the first ad available for dequeue.
 * To take an ad out of the cache, call {@link #dequeueAd}.
 *
 * The cache size may be automatically adjusted by the MoPub server based on an app's usage and
 * ad fill rate. Cached ads have a maximum TTL of 15 minutes before which they expire.
 *
 * The ad source also takes care of retrying failed ad requests, with a reasonable back-off to
 * avoid spamming the server.
 *
 * This class is not thread safe and should only be called from the UI thread.
 */
class NativeAdSource {
    private static final int CACHE_LIMIT = 3;
    private static final int EXPIRATION_TIME_MILLISECONDS = 15 * 60 * 1000; // 15 minutes
    private static final int DEFAULT_RETRY_TIME_MILLISECONDS = 1000; // 1 second
    private static final int MAXIMUM_RETRY_TIME_MILLISECONDS = 5 * 60 * 1000; // 5 minutes.
    private static final double EXPONENTIAL_BACKOFF_FACTOR = 2.0;

    private final List<TimestampWrapper<NativeResponse>> mNativeAdCache;
    private final Handler mReplenishCacheHandler;
    private final Runnable mReplenishCacheRunnable;
    private final MoPubNativeNetworkListener mMoPubNativeNetworkListener;

    @VisibleForTesting boolean mRequestInFlight;
    @VisibleForTesting boolean mRetryInFlight;
    @VisibleForTesting int mSequenceNumber;
    @VisibleForTesting int mRetryTimeMilliseconds;

    private AdSourceListener mAdSourceListener;

    // We will need collections of these when we support multiple ad units.
    private RequestParameters mRequestParameters;
    private MoPubNative mMoPubNative;

    /**
     * A listener for when ads are available for dequeueing.
     */
    interface AdSourceListener {
        /**
         * Called when the number of items available for goes from 0 to more than 0.
         */
        void onAdsAvailable();
    }

    NativeAdSource() {
        this(new ArrayList<TimestampWrapper<NativeResponse>>(CACHE_LIMIT), new Handler());
    }

    @VisibleForTesting
    NativeAdSource(final List<TimestampWrapper<NativeResponse>> nativeAdCache,
            final Handler replenishCacheHandler) {
        mNativeAdCache = nativeAdCache;
        mReplenishCacheHandler = replenishCacheHandler;
        mReplenishCacheRunnable = new Runnable() {
            @Override
            public void run() {
                mRetryInFlight = false;
                replenishCache();
            }
        };

        // Construct native URL and start filling the cache
        mMoPubNativeNetworkListener = new MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(final NativeResponse nativeResponse) {
                // This can be null if the ad source was cleared as the AsyncTask is posting
                // back to the UI handler. Drop this response.
                if (mMoPubNative == null) {
                    return;
                }

                mRequestInFlight = false;
                mSequenceNumber++;
                resetRetryTime();

                mNativeAdCache.add(new TimestampWrapper<NativeResponse>(nativeResponse));
                if (mNativeAdCache.size() == 1 && mAdSourceListener != null) {
                    mAdSourceListener.onAdsAvailable();
                }

                replenishCache();
            }

            @Override
            public void onNativeFail(final NativeErrorCode errorCode) {
                // Reset the retry time for the next time we dequeue.
                mRequestInFlight = false;

                // Stopping requests after the max retry time prevents us from using battery when
                // the user is not interacting with the stream, eg. the app is backgrounded.
                if (mRetryTimeMilliseconds >= MAXIMUM_RETRY_TIME_MILLISECONDS) {
                    resetRetryTime();
                    return;
                }

                updateRetryTime();
                mRetryInFlight = true;
                mReplenishCacheHandler.postDelayed(mReplenishCacheRunnable, mRetryTimeMilliseconds);
            }
        };

        mSequenceNumber = 0;
        mRetryTimeMilliseconds = DEFAULT_RETRY_TIME_MILLISECONDS;
    }

    /**
     * Sets a adSourceListener for determining when ads are available.
     * @param adSourceListener An AdSourceListener.
     */
    void setAdSourceListener(final AdSourceListener adSourceListener) {
        mAdSourceListener = adSourceListener;
    }

    void loadAds(final Context context,
            final String adUnitId,
            final RequestParameters requestParameters) {
        loadAds(requestParameters, new MoPubNative(context, adUnitId, mMoPubNativeNetworkListener));
    }

    @VisibleForTesting
    void loadAds(final RequestParameters requestParameters,
             final MoPubNative moPubNative) {
        clear();

        mRequestParameters = requestParameters;
        mMoPubNative = moPubNative;

        replenishCache();
    }

    /**
     * Clears the ad source, removing any currently queued ads.
     */
    void clear() {
        // This will cleanup listeners to stop callbacks from handling old ad units
        if (mMoPubNative != null) {
            mMoPubNative.destroy();
            mMoPubNative = null;
        }

        mRequestParameters = null;

        for (final TimestampWrapper<NativeResponse> timestampWrapper : mNativeAdCache) {
            timestampWrapper.mInstance.destroy();
        }
        mNativeAdCache.clear();

        mReplenishCacheHandler.removeMessages(0);
        mRequestInFlight = false;
        mSequenceNumber = 0;
        resetRetryTime();
    }

    /**
     * Removes an ad from the front of the ad source cache.
     *
     * Dequeueing will automatically attempt to replenish the cache. Callers should dequeue ads as
     * late as possible, typically immediately before rendering them into a view.
     *
     * Set the listener to {@code null} to remove the listener.
     *
     * @return Ad ad item that should be rendered into a view.
     */
    NativeResponse dequeueAd() {
        final long now = SystemClock.uptimeMillis();

        // Starting an ad request takes several millis. Post for performance reasons.
        if (!mRequestInFlight && !mRetryInFlight) {
            mReplenishCacheHandler.post(mReplenishCacheRunnable);
        }

        // Dequeue the first ad that hasn't expired.
        while (!mNativeAdCache.isEmpty()) {
            TimestampWrapper<NativeResponse> responseWrapper = mNativeAdCache.remove(0);

            if (now - responseWrapper.mCreatedTimestamp < EXPIRATION_TIME_MILLISECONDS) {
                return responseWrapper.mInstance;
            }
        }
        return null;
    }

    @VisibleForTesting
    void updateRetryTime() {
        // Backoff time calculations
        mRetryTimeMilliseconds = (int) (mRetryTimeMilliseconds * EXPONENTIAL_BACKOFF_FACTOR);
        if (mRetryTimeMilliseconds > MAXIMUM_RETRY_TIME_MILLISECONDS) {
            mRetryTimeMilliseconds = MAXIMUM_RETRY_TIME_MILLISECONDS;
        }
    }

    @VisibleForTesting
    void resetRetryTime() {
        mRetryTimeMilliseconds = DEFAULT_RETRY_TIME_MILLISECONDS;
    }

    /**
     * Replenish ads in the ad source cache.
     *
     * Calling this method is useful for warming the cache without dequeueing an ad.
     */
    @VisibleForTesting
    void replenishCache() {
        if (!mRequestInFlight && mMoPubNative != null && mNativeAdCache.size() < CACHE_LIMIT) {
            mRequestInFlight = true;
            mMoPubNative.loadNativeAd(mRequestParameters, mSequenceNumber);
        }
    }

    @Deprecated
    @VisibleForTesting
    void setMoPubNative(final MoPubNative moPubNative) {
        mMoPubNative = moPubNative;
    }

    @Deprecated
    @VisibleForTesting
    MoPubNativeNetworkListener getMoPubNativeNetworkListener() {
        return mMoPubNativeNetworkListener;
    }
}
