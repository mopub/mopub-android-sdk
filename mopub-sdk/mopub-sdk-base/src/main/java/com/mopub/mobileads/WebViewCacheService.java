// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.mopub.common.Constants.FIFTEEN_MINUTES_MILLIS;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * Holds WebViews in memory until they are used.
 */
public class WebViewCacheService {
    public static class Config {
        @NonNull
        private final BaseWebView mWebView;
        @NonNull
        private final WeakReference<BaseAd> mWeakBaseAd;
        @NonNull
        private final ExternalViewabilitySessionManager mViewabilityManager;
        @Nullable
        private final MoPubWebViewController mController;

        Config(@NonNull final BaseWebView baseWebView,
               @NonNull final BaseAd baseAd,
               @NonNull final ExternalViewabilitySessionManager viewabilityManager,
               @Nullable final MoPubWebViewController controller) {
            Preconditions.checkNotNull(baseWebView);
            Preconditions.checkNotNull(baseAd);
            Preconditions.checkNotNull(viewabilityManager);

            mWebView = baseWebView;
            mWeakBaseAd = new WeakReference<>(baseAd);
            mViewabilityManager = viewabilityManager;
            mController = controller;
        }

        @NonNull
        public BaseWebView getWebView() {
            return mWebView;
        }

        @NonNull
        public WeakReference<BaseAd> getWeakBaseAd() {
            return mWeakBaseAd;
        }

        @NonNull
        public ExternalViewabilitySessionManager getViewabilityManager() {
            return mViewabilityManager;
        }

        @Nullable
        public MoPubWebViewController getController() {
            return mController;
        }
    }

    /**
     * Maximum number of {@link BaseWebView}s that are cached. This limit is intended to be very
     * conservative; it is not recommended to cache more than a few BaseWebViews.
     */
    @VisibleForTesting
    static final int MAX_SIZE = 50;

    /**
     * Trim the cache at least this frequently. Trimming only removes a {@link Config}s when its
     * associated {@link Interstitial} is no longer in memory. The cache is also
     * trimmed every time {@link #storeWebViewConfig(Long, BaseAd, BaseWebView, ExternalViewabilitySessionManager, MoPubWebViewController)} is called.
     */
    @VisibleForTesting
    static final long TRIM_CACHE_FREQUENCY_MILLIS = FIFTEEN_MINUTES_MILLIS;

    @SuppressLint("UseSparseArrays")
    @NonNull
    private static final Map<Long, Config> sWebViewConfigs =
            Collections.synchronizedMap(new HashMap<Long, Config>());

    @VisibleForTesting
    @NonNull
    static final TrimCacheRunnable sTrimCacheRunnable = new TrimCacheRunnable();
    @NonNull
    private static Handler sHandler = new Handler();

    private WebViewCacheService() {
    }

    /**
     * Stores the {@link BaseWebView} in the cache. This WebView will live until it is retrieved via
     * {@link #popWebViewConfig(Long)} or when the base interstitial object is removed from memory.
     *
     * @param broadcastIdentifier The unique identifier associated with both the interstitial and the WebView
     * @param baseWebView         The BaseWebView to be stored
     * @param viewabilityManager  The associated viewability manager, which needs to be created
     *                            during Interstitial load and reutilized on show
     */
    @VisibleForTesting
    public static void storeWebViewConfig(@NonNull final Long broadcastIdentifier,
                                          @NonNull final BaseWebView baseWebView,
                                          @NonNull final BaseAd baseAd,
                                          @NonNull final ExternalViewabilitySessionManager viewabilityManager,
                                          @Nullable final MoPubWebViewController controller) {
        Preconditions.checkNotNull(broadcastIdentifier);
        Preconditions.checkNotNull(baseWebView);
        Preconditions.checkNotNull(baseAd);
        Preconditions.checkNotNull(viewabilityManager);

        trimCache();
        // Ignore request when max size is reached.
        if (sWebViewConfigs.size() >= MAX_SIZE) {
            MoPubLog.log(CUSTOM,
                    "Unable to cache web view. Please destroy some via MoPubInterstitial#destroy() and try again.");
            return;
        }

        sWebViewConfigs.put(broadcastIdentifier,
                new Config(baseWebView, baseAd, viewabilityManager, controller));
    }

    @Nullable
    public static Config popWebViewConfig(@NonNull final Long broadcastIdentifier) {
        Preconditions.checkNotNull(broadcastIdentifier);

        return sWebViewConfigs.remove(broadcastIdentifier);
    }

    @VisibleForTesting
    static synchronized void trimCache() {
        final Iterator<Map.Entry<Long, Config>> iterator = sWebViewConfigs.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Long, Config> entry = iterator.next();

            // If the BaseAd was removed from memory, end viewability manager tracking and
            // discard the entire associated Config.
            if (entry.getValue().getWeakBaseAd().get() == null) {
                entry.getValue().getViewabilityManager().endDisplaySession();
                iterator.remove();
            }
        }

        if (!sWebViewConfigs.isEmpty()) {
            sHandler.removeCallbacks(sTrimCacheRunnable);
            sHandler.postDelayed(sTrimCacheRunnable, TRIM_CACHE_FREQUENCY_MILLIS);
        }
    }

    private static class TrimCacheRunnable implements Runnable {
        @Override
        public void run() {
            trimCache();
        }
    }

    @Deprecated
    @VisibleForTesting
    public static void clearAll() {
        sWebViewConfigs.clear();
        sHandler.removeCallbacks(sTrimCacheRunnable);
    }

    @Deprecated
    @VisibleForTesting
    @NonNull
    static Map<Long, Config> getWebViewConfigs() {
        return sWebViewConfigs;
    }

    @Deprecated
    @VisibleForTesting
    static void setHandler(@NonNull final Handler handler) {
        sHandler = handler;
    }
}
