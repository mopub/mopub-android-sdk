package com.mopub.mobileads;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

/**
 * Holds WebViews in memory until they are used.
 */
public class WebViewCacheService {
    public static class Config {
        @NonNull
        private final BaseWebView mWebView;
        @NonNull
        private final WeakReference<Interstitial> mWeakInterstitial;
        @NonNull
        private final ExternalViewabilitySessionManager mViewabilityManager;

        Config(@NonNull final BaseWebView baseWebView,
                @NonNull final Interstitial baseInterstitial,
                @NonNull final ExternalViewabilitySessionManager viewabilityManager) {
            mWebView = baseWebView;
            mWeakInterstitial = new WeakReference<Interstitial>(baseInterstitial);
            mViewabilityManager = viewabilityManager;
        }

        @NonNull
        public BaseWebView getWebView() {
            return mWebView;
        }

        @NonNull
        public WeakReference<Interstitial> getWeakInterstitial() {
            return mWeakInterstitial;
        }

        @NonNull
        public ExternalViewabilitySessionManager getViewabilityManager() {
            return mViewabilityManager;
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
     * trimmed every time {@link #storeWebViewConfig(Long, Interstitial, BaseWebView, ExternalViewabilitySessionManager)} is called.
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
     * @param baseInterstitial    The interstitial managing this WebView
     * @param baseWebView         The BaseWebView to be stored
     * @param viewabilityManager  The associated viewability manager, which needs to be created
     *                            during Interstitial load and reutilized on show
     */
    @VisibleForTesting
    public static void storeWebViewConfig(@NonNull final Long broadcastIdentifier,
            @NonNull final Interstitial baseInterstitial,
            @NonNull final BaseWebView baseWebView,
            @NonNull final ExternalViewabilitySessionManager viewabilityManager) {
        Preconditions.checkNotNull(broadcastIdentifier);
        Preconditions.checkNotNull(baseInterstitial);
        Preconditions.checkNotNull(baseWebView);

        trimCache();
        // Ignore request when max size is reached.
        if (sWebViewConfigs.size() >= MAX_SIZE) {
            MoPubLog.w(
                    "Unable to cache web view. Please destroy some via MoPubInterstitial#destroy() and try again.");
            return;
        }

        sWebViewConfigs.put(broadcastIdentifier,
                new Config(baseWebView, baseInterstitial, viewabilityManager));
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

            // If the Interstitial was removed from memory, end viewability manager tracking and
            // discard the entire associated Config.
            if (entry.getValue().getWeakInterstitial().get() == null) {
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
