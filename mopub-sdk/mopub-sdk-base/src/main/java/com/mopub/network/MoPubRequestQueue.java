package com.mopub.network;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.volley.Cache;
import com.mopub.volley.Network;
import com.mopub.volley.Request;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.ResponseDelivery;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * MoPub's custom implementation of the Google Volley RequestQueue.
 * This subclass provides convenience methods for adding a delayed request to run at a time in
 * the future. This is useful for our backoff policy architecture.
 *
 * We've overridden certain implementation methods but have kept the contract of the
 * original method consistent.
 */
public class MoPubRequestQueue extends RequestQueue {

    private static final int CAPACITY = 10;

    @NonNull
    private final Map<Request<?>, DelayedRequestHelper> mDelayedRequests;

    MoPubRequestQueue(Cache cache, Network network, int threadPoolSize, ResponseDelivery delivery) {
        super(cache, network, threadPoolSize, delivery);
        mDelayedRequests = new HashMap<Request<?>, DelayedRequestHelper>(CAPACITY);
    }

    MoPubRequestQueue(Cache cache, Network network, int threadPoolSize) {
        super(cache, network, threadPoolSize);
        mDelayedRequests = new HashMap<Request<?>, DelayedRequestHelper>(CAPACITY);
    }

    MoPubRequestQueue(Cache cache, Network network) {
        super(cache, network);
        mDelayedRequests = new HashMap<Request<?>, DelayedRequestHelper>(CAPACITY);
    }

    /**
     * Convenience method for adding a request with a time delay to the request queue.
     *
     * @param request The request.
     * @param delayMs The delay in ms for adding the request to the request queue.
     */
    public void addDelayedRequest(@NonNull Request<?> request, int delayMs) {
        Preconditions.checkNotNull(request);
        addDelayedRequest(request, new DelayedRequestHelper(request, delayMs));
    }

    @VisibleForTesting
    void addDelayedRequest(@NonNull Request<?> request, @NonNull DelayedRequestHelper delayedRequestHelper) {
        Preconditions.checkNotNull(delayedRequestHelper);

        if (mDelayedRequests.containsKey(request)) {
            cancel(request);
        }

        delayedRequestHelper.start();
        mDelayedRequests.put(request, delayedRequestHelper);
    }

    /**
     * Override of cancelAll method to ensure delayed requests are cancelled as well.
     */
    @Override
    public void cancelAll(@NonNull RequestFilter filter) {
        Preconditions.checkNotNull(filter);

        super.cancelAll(filter);

        Iterator<Map.Entry<Request<?>, DelayedRequestHelper>> iterator = mDelayedRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Request<?>, DelayedRequestHelper> entry = iterator.next();
            if (filter.apply(entry.getKey())) {
                // Here we cancel both the request and the handler from posting the delayed runnable
                entry.getKey().cancel();
                entry.getValue().cancel();
                iterator.remove();
            }
        }
    }

    /**
     * Override of cancelAll method to ensure delayed requests are cancelled as well.
     */
    @Override
    public void cancelAll(@NonNull final Object tag) {
        Preconditions.checkNotNull(tag);

        super.cancelAll(tag);

        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return request.getTag() == tag;
            }
        });
    }

    /**
     * Convenience method to cancel a single request.
     *
     * @param request The request to cancel.
     */
    public void cancel(@NonNull final Request<?> request) {
        Preconditions.checkNotNull(request);

        cancelAll(new RequestFilter() {
            @Override
            public boolean apply(Request<?> _request) {
                return request == _request;
            }
        });
    }

    /**
     * This helper class is used to package the supporting objects a request needs to
     * run at a delayed time and cancel if needed.
     */
    class DelayedRequestHelper {
        final int mDelayMs;
        @NonNull final Handler mHandler;
        @NonNull final Runnable mDelayedRunnable;

        DelayedRequestHelper(@NonNull final Request<?> request, int delayMs) {
            this(request, delayMs, new Handler());
        }

        @VisibleForTesting
        DelayedRequestHelper(@NonNull final Request<?> request, int delayMs, @NonNull Handler handler) {
            mDelayMs = delayMs;
            mHandler = handler;
            mDelayedRunnable = new Runnable() {
                @Override
                public void run() {
                    mDelayedRequests.remove(request);
                    MoPubRequestQueue.this.add(request);
                }
            };
        }

        void start() {
            mHandler.postDelayed(mDelayedRunnable, mDelayMs);
        }

        void cancel() {
            mHandler.removeCallbacks(mDelayedRunnable);
        }
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    Map<Request<?>, DelayedRequestHelper> getDelayedRequests() {
        return mDelayedRequests;
    }
}
