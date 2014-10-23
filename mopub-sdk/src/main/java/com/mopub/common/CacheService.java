package com.mopub.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;

import com.mopub.common.util.DeviceUtils;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Streams;
import com.mopub.common.util.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.mopub.common.DiskLruCache.open;

public class CacheService {
    public static interface DiskLruCacheGetListener {
        void onComplete(final String key, final byte[] content);
    }

    static final String UNIQUE_CACHE_NAME = "mopub-cache";
    private static final int APP_VERSION = 1;
    // The number of values per cache entry. Must be positive.
    private static final int VALUE_COUNT = 1;
    private static final int DISK_CACHE_INDEX = 0;

    private static DiskLruCache sDiskLruCache;
    private static BitmapLruCache sBitmapLruCache;

    public static boolean initializeBitmapCache(final Context context) {
        if (context == null) {
            return false;
        }

        if (sBitmapLruCache == null) {
            final int memoryCacheSizeBytes = DeviceUtils.memoryCacheSizeBytes(context);
            sBitmapLruCache = new BitmapLruCache(memoryCacheSizeBytes);
        }
        return true;
    }

    public static boolean initializeDiskCache(final Context context) {
        if (context == null) {
            return false;
        }

        if (sDiskLruCache == null) {
            final File cacheDirectory = getDiskCacheDirectory(context);
            final long diskCacheSizeBytes = DeviceUtils.diskCacheSizeBytes(cacheDirectory);
            try {
                sDiskLruCache = open(
                        cacheDirectory,
                        APP_VERSION,
                        VALUE_COUNT,
                        diskCacheSizeBytes
                );
            } catch (IOException e) {
                MoPubLog.d("Unable to create DiskLruCache", e);
            }
        }
        return true;
    }

    public static void initialize(final Context context) {
        initializeBitmapCache(context);
        initializeDiskCache(context);
    }

    public static String createValidDiskCacheKey(final String key) {
        return Utils.sha1(key);
    }

    public static File getDiskCacheDirectory(final Context context) {
        final String cachePath = context.getCacheDir().getPath();
        return new File(cachePath + File.separator + UNIQUE_CACHE_NAME);
    }

    public static boolean containsKeyDiskCache(final String key) {
        if (sDiskLruCache == null) {
            return false;
        }

        try {
            final DiskLruCache.Snapshot snapshot = sDiskLruCache.get(createValidDiskCacheKey(key));
            return snapshot != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getFilePathDiskCache(final String key) {
        if (sDiskLruCache == null) {
            return null;
        }

        // This violates encapsulation but there is no convenience method to get a filename from
        // DiskLruCache. Filename was derived from private class method Entry#getCleanFile
        // in DiskLruCache.java
        return sDiskLruCache.getDirectory()
                + File.separator
                + createValidDiskCacheKey(key)
                + "."
                + DISK_CACHE_INDEX;
    }

    public static Bitmap getFromBitmapCache(final String key) {
        if (sBitmapLruCache == null) {
            return null;
        }

        return sBitmapLruCache.get(key);
    }

    public static byte[] getFromDiskCache(final String key) {
        if (sDiskLruCache == null) {
            return null;
        }

        byte[] bytes = null;
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = sDiskLruCache.get(createValidDiskCacheKey(key));
            if (snapshot == null) {
                return null;
            }

            final InputStream in = snapshot.getInputStream(DISK_CACHE_INDEX);
            if (in != null) {
                bytes = new byte[(int) snapshot.getLength(0)];
                final BufferedInputStream buffIn = new BufferedInputStream(in);
                try {
                    Streams.readStream(buffIn, bytes);
                } finally {
                    Streams.closeStream(buffIn);
                }
            }
        } catch (Exception e) {
            MoPubLog.d("Unable to get from DiskLruCache", e);
        } finally {
            if (snapshot != null) {
                snapshot.close();
            }
        }

        return bytes;
    }

    public static void getFromDiskCacheAsync(final String key, final DiskLruCacheGetListener diskLruCacheGetListener) {
        new DiskLruCacheGetTask(key, diskLruCacheGetListener).execute();
    }

    public static void putToBitmapCache(final String key, final Bitmap bitmap) {
        if (sBitmapLruCache == null) {
            return;
        }

        sBitmapLruCache.put(key, bitmap);
    }

    public static boolean putToDiskCache(final String key, final byte[] content) {
        return putToDiskCache(key, new ByteArrayInputStream(content));
    }

    public static boolean putToDiskCache(final String key, final InputStream content) {
        if (sDiskLruCache == null) {
            return false;
        }

        DiskLruCache.Editor editor = null;
        try {
            editor = sDiskLruCache.edit(createValidDiskCacheKey(key));

            if (editor == null) {
                // another edit is in progress
                return false;
            }

            final OutputStream outputStream =
                    new BufferedOutputStream(editor.newOutputStream(DISK_CACHE_INDEX));
            Streams.copyContent(content, outputStream);
            outputStream.flush();
            outputStream.close();

            sDiskLruCache.flush();
            editor.commit();
        } catch (Exception e) {
            MoPubLog.d("Unable to put to DiskLruCache", e);
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignore) {
                // ignore
            }
            return false;
        }
        return true;
    }

    public static void putToDiskCacheAsync(final String key, final byte[] content) {
        new DiskLruCachePutTask(key, content).execute();
    }

    private static class BitmapLruCache extends LruCache<String, Bitmap> {
        public BitmapLruCache(final int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(final String key, final Bitmap bitmap) {
            if (bitmap != null) {
                return bitmap.getRowBytes() * bitmap.getHeight();
            }

            return super.sizeOf(key, bitmap);
        }
    }

    private static class DiskLruCacheGetTask extends AsyncTask<Void, Void, byte[]> {
        private final DiskLruCacheGetListener mDiskLruCacheGetListener;
        private final String mKey;

        DiskLruCacheGetTask(final String key, final DiskLruCacheGetListener diskLruCacheGetListener) {
            mDiskLruCacheGetListener = diskLruCacheGetListener;
            mKey = key;
        }

        @Override
        protected byte[] doInBackground(Void... voids) {
            return CacheService.getFromDiskCache(mKey);
        }

        @Override
        protected void onPostExecute(final byte[] bytes) {
            if (isCancelled()) {
                onCancelled();
                return;
            }

            if (mDiskLruCacheGetListener != null) {
                mDiskLruCacheGetListener.onComplete(mKey, bytes);
            }
        }

        @Override
        protected void onCancelled() {
            if (mDiskLruCacheGetListener != null) {
                mDiskLruCacheGetListener.onComplete(mKey, null);
            }
        }
    }

    private static class DiskLruCachePutTask extends AsyncTask<Void, Void, Void> {
        private final String mKey;
        private final byte[] mContent;

        DiskLruCachePutTask(final String key, final byte[] content) {
            mKey = key;
            mContent = content;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            CacheService.putToDiskCache(mKey, mContent);
            return null;
        }
    }

    // Testing
    @Deprecated
    @VisibleForTesting
    public static void clearAndNullCaches() {
        if (sDiskLruCache != null) {
            try {
                sDiskLruCache.delete();
                sDiskLruCache = null;
            } catch (IOException ignore) {
                sDiskLruCache = null;
            }
        }
        if (sBitmapLruCache != null) {
            sBitmapLruCache.evictAll();
            sBitmapLruCache = null;
        }
    }

    // Testing
    @Deprecated
    @VisibleForTesting
    public static LruCache<String, Bitmap> getBitmapLruCache() {
        return sBitmapLruCache;
    }

    // Testing
    @Deprecated
    @VisibleForTesting
    public static DiskLruCache getDiskLruCache() {
        return sDiskLruCache;
    }
}
