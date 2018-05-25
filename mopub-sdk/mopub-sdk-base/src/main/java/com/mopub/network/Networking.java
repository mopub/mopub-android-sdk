package com.mopub.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.DeviceUtils;
import com.mopub.volley.Cache;
import com.mopub.volley.Network;
import com.mopub.volley.RequestQueue;
import com.mopub.volley.toolbox.BaseHttpStack;
import com.mopub.volley.toolbox.BasicNetwork;
import com.mopub.volley.toolbox.DiskBasedCache;
import com.mopub.volley.toolbox.HurlStack;
import com.mopub.volley.toolbox.ImageLoader;

import java.io.File;

import javax.net.ssl.SSLSocketFactory;

public class Networking {
    @VisibleForTesting
    static final String CACHE_DIRECTORY_NAME = "mopub-volley-cache";
    private static final String DEFAULT_USER_AGENT = System.getProperty("http.agent");

    // These are volatile so that double-checked locking works.
    // See https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
    // for more information.
    private volatile static MoPubRequestQueue sRequestQueue;
    private volatile static String sUserAgent;
    private volatile static MaxWidthImageLoader sMaxWidthImageLoader;
    private static boolean sUseHttps = false;
    private static HurlStack.UrlRewriter sUrlRewriter;

    @Nullable
    public static MoPubRequestQueue getRequestQueue() {
        return sRequestQueue;
    }

    @NonNull
    public static HurlStack.UrlRewriter getUrlRewriter(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        // No synchronization done here since it's fine to create the same rewriter more than once.
        if (sUrlRewriter == null) {
            sUrlRewriter = new PlayServicesUrlRewriter();
        }
        return sUrlRewriter;
    }

    @NonNull
    public static MoPubRequestQueue getRequestQueue(@NonNull final Context context) {
        MoPubRequestQueue requestQueue = sRequestQueue;
        // Double-check locking to initialize.
        if (requestQueue == null) {
            synchronized (Networking.class) {
                requestQueue = sRequestQueue;
                if (requestQueue == null) {

                    final SSLSocketFactory socketFactory = CustomSSLSocketFactory.getDefault(Constants.TEN_SECONDS_MILLIS);

                    final String userAgent = Networking.getUserAgent(
                            context.getApplicationContext());
                    final BaseHttpStack httpStack = new RequestQueueHttpStack(userAgent,
                            getUrlRewriter(context), socketFactory);

                    final Network network = new BasicNetwork(httpStack);
                    final File volleyCacheDir = new File(context.getCacheDir().getPath() +
                            File.separator + CACHE_DIRECTORY_NAME);
                    final Cache cache = new DiskBasedCache(volleyCacheDir,
                            (int) DeviceUtils.diskCacheSizeBytes(volleyCacheDir, Constants.TEN_MB));
                    requestQueue = new MoPubRequestQueue(cache, network);
                    sRequestQueue = requestQueue;
                    requestQueue.start();
                }
            }
        }

        return requestQueue;
    }

    @NonNull
    public static ImageLoader getImageLoader(@NonNull Context context) {
        MaxWidthImageLoader imageLoader = sMaxWidthImageLoader;
        // Double-check locking to initialize.
        if (imageLoader == null) {
            synchronized (Networking.class) {
                imageLoader = sMaxWidthImageLoader;
                if (imageLoader == null) {
                    RequestQueue queue = getRequestQueue(context);
                    int cacheSize = DeviceUtils.memoryCacheSizeBytes(context);
                    final LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, Bitmap value) {
                            if (value != null) {
                                return value.getRowBytes() * value.getHeight();
                            }

                            return super.sizeOf(key, value);
                        }
                    };
                    imageLoader = new MaxWidthImageLoader(queue, context, new MaxWidthImageLoader.ImageCache() {
                        @Override
                        public Bitmap getBitmap(final String key) {
                            return imageCache.get(key);
                        }

                        @Override
                        public void putBitmap(final String key, final Bitmap bitmap) {
                            imageCache.put(key, bitmap);
                        }
                    });
                    sMaxWidthImageLoader = imageLoader;
                }
            }
        }
        return imageLoader;
    }

    /**
     * Caches and returns the WebView user agent to be used across all SDK requests. This is
     * important because advertisers expect the same user agent across all request, impression, and
     * click events.
     */
    @NonNull
    public static String getUserAgent(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        String userAgent = sUserAgent;
        if (userAgent == null) {
            synchronized (Networking.class) {
                userAgent = sUserAgent;
                if (userAgent == null) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            userAgent = WebSettings.getDefaultUserAgent(context);
                        } else if (Looper.myLooper() == Looper.getMainLooper()) {
                            // WebViews may only be instantiated on the UI thread. If anything goes
                            // wrong with getting a user agent, use the system-specific user agent.
                            userAgent = new WebView(context).getSettings().getUserAgentString();
                        } else {
                            userAgent = DEFAULT_USER_AGENT;
                        }
                    } catch (Exception e) {
                        // Some custom ROMs may fail to get a user agent. If that happens, return
                        // the Android system user agent.
                        userAgent = DEFAULT_USER_AGENT;
                    }
                    sUserAgent = userAgent;
                }
            }
        }

        return userAgent;
    }

    /**
     * Gets the previously cached WebView user agent. This returns the default userAgent if the
     * WebView user agent has not been initialized yet.
     *
     * @return Best-effort String WebView user agent.
     */
    @NonNull
    public static String getCachedUserAgent() {
        final String userAgent = sUserAgent;
        if (userAgent == null) {
            return DEFAULT_USER_AGENT;
        }
        return userAgent;
    }

    @VisibleForTesting
    public static synchronized void clearForTesting() {
        sRequestQueue = null;
        sMaxWidthImageLoader = null;
        sUserAgent = null;
    }

    @VisibleForTesting
    public static synchronized void setRequestQueueForTesting(MoPubRequestQueue queue) {
        sRequestQueue = queue;
    }

    @VisibleForTesting
    public static synchronized void setImageLoaderForTesting(MaxWidthImageLoader imageLoader) {
        sMaxWidthImageLoader = imageLoader;
    }

    @VisibleForTesting
    public static synchronized void setUserAgentForTesting(String userAgent) {
        sUserAgent = userAgent;
    }

    /**
     * Set whether to use HTTP or HTTPS for WebView base urls.
     */
    public static void useHttps(boolean useHttps) {
        sUseHttps = useHttps;
    }

    public static boolean shouldUseHttps() {
        return sUseHttps;
    }

    /**
     * Retrieve the scheme that should be used to communicate to the ad server. This should always
     * return https.
     *
     * @return "https"
     */
    public static String getScheme() {
        return Constants.HTTPS;
    }

    /**
     * DSPs are currently not ready for full https creatives. When we flip the switch to go full
     * https, this should just return https. However, for now, we allow the publisher to use
     * either http or https. This only affects WebView base urls.
     *
     * @return "https" if {@link #shouldUseHttps()} is true; "http" otherwise.
     */
    public static String getBaseUrlScheme() {
        return shouldUseHttps() ? Constants.HTTPS : Constants.HTTP;
    }
}
