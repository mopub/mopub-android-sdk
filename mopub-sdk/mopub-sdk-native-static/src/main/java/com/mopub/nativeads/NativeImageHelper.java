package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
import com.mopub.volley.toolbox.ImageLoader;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Collection of helper methods to assist with image downloading and displaying
 */
public class NativeImageHelper {

    public interface ImageListener {
        /**
         * Called when images are successfully cached. If you haven't already called {@link
         * CustomEventNativeListener#onNativeAdLoaded}, you should typically do so now.
         */
        void onImagesCached();

        /**
         * Called when images failed to cache. You should typically call {@link
         * CustomEventNativeListener#onNativeAdFailed} from this callback.
         *
         * @param errorCode An enum value with the relevant error message.
         */
        void onImagesFailedToCache(NativeErrorCode errorCode);
    }

    /**
     * Pre caches the given set of image urls. We recommend using this method to warm the image
     * cache before calling {@link CustomEventNativeListener#onNativeAdLoaded}. Doing so will
     * force images to cache before displaying the ad.
     */
    public static void preCacheImages(@NonNull final Context context,
            @NonNull final List<String> imageUrls,
            @NonNull final ImageListener imageListener) {
        final ImageLoader imageLoader = Networking.getImageLoader(context);
        // These Atomics are only accessed on the main thread.
        // We use Atomics here so we can change their values while keeping a reference for the inner class.
        final AtomicInteger imageCounter = new AtomicInteger(imageUrls.size());
        final AtomicBoolean anyFailures = new AtomicBoolean(false);
        ImageLoader.ImageListener volleyImageListener = new ImageLoader.ImageListener() {
            @Override
            public void onResponse(final ImageLoader.ImageContainer imageContainer, final boolean isImmediate) {
                // Image Loader returns a "default" response immediately. We want to ignore this
                // unless the image is already cached.
                if (imageContainer.getBitmap() != null) {
                    final int count = imageCounter.decrementAndGet();
                    if (count == 0 && !anyFailures.get()) {
                        imageListener.onImagesCached();
                    }
                }
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                MoPubLog.d("Failed to download a native ads image:", volleyError);
                boolean anyPreviousErrors = anyFailures.getAndSet(true);
                imageCounter.decrementAndGet();
                if (!anyPreviousErrors) {
                    imageListener.onImagesFailedToCache(NativeErrorCode.IMAGE_DOWNLOAD_FAILURE);
                }
            }
        };

        for (String url : imageUrls) {
            if (TextUtils.isEmpty(url)) {
                anyFailures.set(true);
                imageListener.onImagesFailedToCache(NativeErrorCode.IMAGE_DOWNLOAD_FAILURE);
                return;
            }
            imageLoader.get(url, volleyImageListener);
        }
    }

    /**
     * Helper method that takes an image url and loads the image into an image view.
     *
     * @param url The image url
     * @param imageView The image view into which to load the image
     */
    public static void loadImageView(@Nullable final String url, @Nullable final ImageView imageView) {
        if (!Preconditions.NoThrow.checkNotNull(imageView, "Cannot load image into null ImageView")) {
            return;
        }

        if (!Preconditions.NoThrow.checkNotNull(url, "Cannot load image with null url")) {
            imageView.setImageDrawable(null);
            return;
        }

        final ImageLoader mImageLoader = Networking.getImageLoader(imageView.getContext());
        mImageLoader.get(url, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(final ImageLoader.ImageContainer imageContainer,
                    final boolean isImmediate) {
                if (!isImmediate) {
                    MoPubLog.d("Image was not loaded immediately into your ad view. You should call preCacheImages as part of your custom event loading process.");
                }
                imageView.setImageBitmap(imageContainer.getBitmap());
            }

            @Override
            public void onErrorResponse(final VolleyError volleyError) {
                MoPubLog.d("Failed to load image.", volleyError);
                imageView.setImageDrawable(null);
            }
        });
    }
}
