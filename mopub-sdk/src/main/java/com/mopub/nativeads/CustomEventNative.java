package com.mopub.nativeads;

import android.content.Context;

import java.util.Map;

/**
 * CustomEventNative is a base class for custom events that support native ads. By implementing
 * subclasses of CustomEventNative, you can enable the MoPub SDK to support a wider
 * variety of third-party ad networks, or execute any of your application code on demand.
 *
 * At runtime, the MoPub SDK will find and instantiate a CustomEventNative subclass as needed
 * and invoke its loadNativeAd() method.
 */
public abstract class CustomEventNative {
    /**
     * When the MoPub SDK receives a response indicating it should load a custom event, it will send
     * this message to your custom event class. Your implementation of this method can either load
     * a native ad from a third-party ad network, or execute any application code. It must also
     * notify the provided CustomEventNativeListener Object of certain lifecycle events.
     *
     * The localExtras parameter is a Map containing additional custom data that is set within
     * your application by calling MoPubNative.setLocalExtras(Map<String, Object>). Note that the
     * localExtras Map is a copy of the Map supplied to setLocalExtras().
     *
     * The serverExtras parameter is a Map containing additional custom data configurable on the
     * MoPub website that you want to associate with a given custom event request. This data may be
     * used to pass dynamic information, such as publisher IDs, without changes in application code.
     */
    protected abstract void loadNativeAd(final Context context,
            final CustomEventNativeListener customEventNativeListener,
            final Map<String, Object> localExtras,
            final Map<String, String> serverExtras);

    public interface ImageListener {
        /**
         * Called when images are successfully cached. If you haven't already called
         * {@link CustomEventNativeListener#onNativeAdLoaded}, you should typically do so now.
         */
        void onImagesCached();

        /**
         * Called when images failed to cache. You should typically call
         * {@link CustomEventNativeListener#onNativeAdFailed} from this callback.
         */
        void onImagesFailedToCache(NativeErrorCode errorCode);
    }

    public interface CustomEventNativeListener {
        /**
         * Your custom event subclass must call this method when it successfully loads a native ad.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        void onNativeAdLoaded(NativeAdInterface nativeAd);

        /**
         * Your custom event subclass must call this method when it fails to load a native ad.
         * Failure to do so will disrupt the mediation waterfall and cause future ad requests to
         * stall.
         */
        void onNativeAdFailed(NativeErrorCode errorCode);
    }
}
