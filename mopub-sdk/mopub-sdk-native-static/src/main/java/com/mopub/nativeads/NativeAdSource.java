package com.mopub.nativeads;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import static com.mopub.common.Constants.AD_EXPIRATION_DELAY;
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
    /**
     * Number of ads to cache
     */
    private static final int CACHE_LIMIT = 1;

    private static final int EXPIRATION_TIME_MILLISECONDS = AD_EXPIRATION_DELAY;
    private static final int MAXIMUM_RETRY_TIME_MILLISECONDS = 5 * 60 * 1000; // 5 minutes.
    @VisibleForTesting static final int[] RETRY_TIME_ARRAY_MILLISECONDS = new int[]{1000, 3000, 5000, 25000, 60000, MAXIMUM_RETRY_TIME_MILLISECONDS};

    @NonNull private final List<TimestampWrapper<NativeAd>> mNativeAdCache;
    @NonNull private final Handler mReplenishCacheHandler;
    @NonNull private final Runnable mReplenishCacheRunnable;
    @NonNull private final MoPubNativeNetworkListener mMoPubNativeNetworkListener;

    @VisibleForTesting boolean mRequestInFlight;
    @VisibleForTesting boolean mRetryInFlight;
    @VisibleForTesting int mSequenceNumber;
    @VisibleForTesting int mCurrentRetries;

    @Nullable private AdSourceListener mAdSourceListener;

    // We will need collections of these when we support multiple ad units.
    @Nullable private RequestParameters mRequestParameters;
    @Nullable private MoPubNative mMoPubNative;

    @NonNull private final AdRendererRegistry mAdRendererRegistry;

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
        this(new ArrayList<TimestampWrapper<NativeAd>>(CACHE_LIMIT),
                new Handler(),
                new AdRendererRegistry());
    }

    @VisibleForTesting
    NativeAdSource(@NonNull final List<TimestampWrapper<NativeAd>> nativeAdCache,
            @NonNull final Handler replenishCacheHandler,
            @NonNull AdRendererRegistry adRendererRegistry) {
        mNativeAdCache = nativeAdCache;
        mReplenishCacheHandler = replenishCacheHandler;
        mReplenishCacheRunnable = new Runnable() {
            @Override
            public void run() {
                mRetryInFlight = false;
                replenishCache();
            }
        };

        mAdRendererRegistry = adRendererRegistry;

        // Construct native URL and start filling the cache
        mMoPubNativeNetworkListener = new MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(@NonNull final NativeAd nativeAd) {
                // This can be null if the ad source was cleared as the AsyncTask is posting
                // back to the UI handler. Drop this response.
                if (mMoPubNative == null) {
                    return;
                }

                mRequestInFlight = false;
                mSequenceNumber++;
                resetRetryTime();

                mNativeAdCache.add(new TimestampWrapper<NativeAd>(nativeAd));
                if (mNativeAdCache.size() == 1 && mAdSourceListener != null) {
                    mAdSourceListener.onAdsAvailable();
                }

                replenishCache();
            }

            @Override
            public void onNativeFail(final NativeErrorCode errorCode) {
                // Reset the retry time for the next time we dequeue.
                mRequestInFlight = false;

                // Stopping requests after the max retry count prevents us from using battery when
                // the user is not interacting with the stream, eg. the app is backgrounded.
                if (mCurrentRetries >= RETRY_TIME_ARRAY_MILLISECONDS.length - 1) {
                    resetRetryTime();
                    return;
                }

                updateRetryTime();
                mRetryInFlight = true;
                mReplenishCacheHandler.postDelayed(mReplenishCacheRunnable, getRetryTime());
            }
        };

        mSequenceNumber = 0;
        resetRetryTime();
    }

    int getAdRendererCount() {
        return mAdRendererRegistry.getAdRendererCount();
    }

    public int getViewTypeForAd(@NonNull final NativeAd nativeAd) {
        return mAdRendererRegistry.getViewTypeForAd(nativeAd);
    }

    /**
     * Registers an ad renderer for rendering a specific native ad format.
     * Note that if multiple ad renderers support a specific native ad format, the first
     * one registered will be used.
     */
    void registerAdRenderer(@NonNull final MoPubAdRenderer moPubNativeAdRenderer) {
        mAdRendererRegistry.registerAdRenderer(moPubNativeAdRenderer);
        if (mMoPubNative != null) {
            mMoPubNative.registerAdRenderer(moPubNativeAdRenderer);
        }
    }

    @Nullable
    public MoPubAdRenderer getAdRendererForViewType(final int viewType) {
        return mAdRendererRegistry.getRendererForViewType(viewType);
    }

    /**
     * Sets a adSourceListener for determining when ads are available.
     * @param adSourceListener An AdSourceListener.
     */
    void setAdSourceListener(@Nullable final AdSourceListener adSourceListener) {
        mAdSourceListener = adSourceListener;
    }

    void loadAds(@NonNull final Activity activity,
            @NonNull final String adUnitId,
            final RequestParameters requestParameters) {
        loadAds(requestParameters, new MoPubNative(activity, adUnitId, mMoPubNativeNetworkListener));
    }

    @VisibleForTesting
    void loadAds(final RequestParameters requestParameters,
             final MoPubNative moPubNative) {
        clear();

        for (MoPubAdRenderer renderer : mAdRendererRegistry.getRendererIterable()) {
            moPubNative.registerAdRenderer(renderer);
        }

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

        for (final TimestampWrapper<NativeAd> timestampWrapper : mNativeAdCache) {
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
    @Nullable
    NativeAd dequeueAd() {
        final long now = SystemClock.uptimeMillis();

        // Starting an ad request takes several millis. Post for performance reasons.
        if (!mRequestInFlight && !mRetryInFlight) {
            mReplenishCacheHandler.post(mReplenishCacheRunnable);
        }

        // Dequeue the first ad that hasn't expired.
        while (!mNativeAdCache.isEmpty()) {
            TimestampWrapper<NativeAd> responseWrapper = mNativeAdCache.remove(0);

            if (now - responseWrapper.mCreatedTimestamp < EXPIRATION_TIME_MILLISECONDS) {
                return responseWrapper.mInstance;
            }
        }
        return null;
    }

    @VisibleForTesting
    void updateRetryTime() {
        if (mCurrentRetries < RETRY_TIME_ARRAY_MILLISECONDS.length - 1) {
            mCurrentRetries++;
        }
    }

    @VisibleForTesting
    void resetRetryTime() {
        mCurrentRetries = 0;
    }

    @VisibleForTesting
    int getRetryTime() {
        if (mCurrentRetries >= RETRY_TIME_ARRAY_MILLISECONDS.length) {
            mCurrentRetries = RETRY_TIME_ARRAY_MILLISECONDS.length - 1;
        }
        return RETRY_TIME_ARRAY_MILLISECONDS[mCurrentRetries];
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
            mMoPubNative.makeRequest(mRequestParameters, mSequenceNumber);
        }
    }

    @Deprecated
    @VisibleForTesting
    void setMoPubNative(final MoPubNative moPubNative) {
        mMoPubNative = moPubNative;
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    MoPubNativeNetworkListener getMoPubNativeNetworkListener() {
        return mMoPubNativeNetworkListener;
    }
}
