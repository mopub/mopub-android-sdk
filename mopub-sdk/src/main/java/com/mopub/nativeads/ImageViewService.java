package com.mopub.nativeads;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.nativeads.ImageService.ImageServiceListener;

class ImageViewService {
    // This is used instead of View.setTag, which causes a memory leak in 2.3
    // and earlier: https://code.google.com/p/android/issues/detail?id=18273
    private static final WeakHashMap<ImageView, Long> sImageViewRequestIds =
            new WeakHashMap<ImageView, Long>();

    private ImageViewService(){}

    static void loadImageView(final String url, final ImageView imageView) {
        if (imageView == null) {
            MoPubLog.d("Attempted to load an image into a null ImageView");
            return;
        }

        // Blank out previous image content while waiting for request to return
        imageView.setImageDrawable(null);

        if (url != null) {
            // Unique id to identify this async image request
            long uniqueId = Utils.generateUniqueId();
            sImageViewRequestIds.put(imageView, uniqueId);

            // Async call to get image from memory cache, disk and then network
            ImageService.get(
                    Arrays.asList(url),
                    new MyImageViewServiceListener(url, imageView, uniqueId)
            );
        }
    }

    private static class MyImageViewServiceListener implements ImageServiceListener {
        private final WeakReference<ImageView> mImageView;
        private final String mUrl;
        private final long mUniqueId;

        MyImageViewServiceListener(final String url, final ImageView imageView, final long uniqueId) {
            mUrl = url;
            mImageView = new WeakReference<ImageView>(imageView);
            mUniqueId = uniqueId;
        }

        @Override
        public void onSuccess(final Map<String, Bitmap> bitmaps) {
            final ImageView imageView = mImageView.get();
            if (imageView == null || bitmaps == null || !bitmaps.containsKey(mUrl)) {
                return;
            }
            final Long uniqueId = sImageViewRequestIds.get(imageView);
            if (uniqueId != null && mUniqueId == uniqueId) {
                imageView.setImageBitmap(bitmaps.get(mUrl));
            }
        }

        @Override
        public void onFail() {
            MoPubLog.d("Failed to load image for ImageView");
        }
    }

    @VisibleForTesting
    @Deprecated
    static Long getImageViewUniqueId(final ImageView imageView) {
        return sImageViewRequestIds.get(imageView);
    }

    @VisibleForTesting
    @Deprecated
    static void setImageViewUniqueId(final ImageView imageView, final long uniqueId) {
        sImageViewRequestIds.put(imageView, uniqueId);
    }
}
