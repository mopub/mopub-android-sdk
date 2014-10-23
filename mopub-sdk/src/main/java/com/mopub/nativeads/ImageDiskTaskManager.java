package com.mopub.nativeads;

import android.graphics.Bitmap;
import com.mopub.common.CacheService;

import java.util.*;

import static com.mopub.common.CacheService.DiskLruCacheGetListener;

class ImageDiskTaskManager extends TaskManager<Bitmap> {
    private final List<String> mUrls;
    private final int mMaxImageWidth;

    ImageDiskTaskManager(final List<String> urls,
            final TaskManagerListener<Bitmap> imageTaskManagerListener,
            final int maxImageWidth)
            throws IllegalArgumentException {
        super(urls, imageTaskManagerListener);
        mMaxImageWidth = maxImageWidth;
        mUrls = urls;
    }

    @Override
    void execute() {
        if (mUrls.isEmpty()) {
            mImageTaskManagerListener.onSuccess(mResults);
        }

        ImageDiskTaskListener imageDiskTaskListener = new ImageDiskTaskListener(mMaxImageWidth);
        for (final String url : mUrls) {
            CacheService.getFromDiskCacheAsync(url, imageDiskTaskListener);
        }
    }

    void failAllTasks() {
        if (mFailed.compareAndSet(false, true)) {
            mImageTaskManagerListener.onFail();
        }
    }

    private class ImageDiskTaskListener implements DiskLruCacheGetListener {

        private final int mTargetWidth;

        ImageDiskTaskListener(final int targetWidth) {
            mTargetWidth = targetWidth;
        }

        @Override
        public void onComplete(final String key, final byte[] content) {
            if (key == null) {
                failAllTasks();
                return;
            } else {
                Bitmap bitmap = null;
                if (content != null) {
                     bitmap = ImageService.byteArrayToBitmap(content, mTargetWidth);
                }
                mResults.put(key, bitmap);
            }

            if (mCompletedCount.incrementAndGet() == mSize) {
                mImageTaskManagerListener.onSuccess(mResults);
            }
        }
    }
}
