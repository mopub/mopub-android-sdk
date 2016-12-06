package com.mopub.mobileads;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.CacheService;
import com.mopub.common.MoPubHttpUrlConnection;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Streams;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.ArrayDeque;
import java.util.Deque;

public class VideoDownloader {
    private static final int MAX_VIDEO_SIZE = 25 * 1024 * 1024; // 25 MiB
    private static final Deque<WeakReference<VideoDownloaderTask>> sDownloaderTasks =
            new ArrayDeque<WeakReference<VideoDownloaderTask>>();

    interface VideoDownloaderListener {
        void onComplete(boolean success);
    }

    private VideoDownloader() {}

    public static void cache(@Nullable final String url,
            @NonNull final VideoDownloaderListener listener) {
        Preconditions.checkNotNull(listener);

        if (url == null) {
            MoPubLog.d("VideoDownloader attempted to cache video with null url.");
            listener.onComplete(false);
            return;
        }

        final VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(listener);
        try {
            AsyncTasks.safeExecuteOnExecutor(videoDownloaderTask, url);
        } catch (Exception e) {
            listener.onComplete(false);
        }
    }


    public static void cancelAllDownloaderTasks() {
        for (final WeakReference<VideoDownloaderTask> weakDownloaderTask : sDownloaderTasks) {
            cancelOneTask(weakDownloaderTask);
        }

        sDownloaderTasks.clear();
    }

    public static void cancelLastDownloadTask() {
        if (sDownloaderTasks.isEmpty()) {
            return;
        }

        cancelOneTask(sDownloaderTasks.peekLast());
        sDownloaderTasks.removeLast();
    }

    /**
     * @param weakDownloaderTask A weak reference to an in-flight VideoDownloaderTask
     * @return  <tt>false</tt> if weakDownloaderTask is null, has a null referent, or if the task has
     *          already been completed
     *          <tt>true</tt> otherwise
     */
    private static boolean cancelOneTask(
            @Nullable final WeakReference<VideoDownloaderTask> weakDownloaderTask) {
        if (weakDownloaderTask == null) {
            return false;
        }

        final VideoDownloaderTask downloaderTask = weakDownloaderTask.get();
        if (downloaderTask == null) {
            return false;
        }

        return downloaderTask.cancel(true);
    }

    @VisibleForTesting
    static class VideoDownloaderTask extends AsyncTask<String, Void, Boolean> {
        @NonNull private final VideoDownloaderListener mListener;
        @NonNull private final WeakReference<VideoDownloaderTask> mWeakSelf;

        @VisibleForTesting
        VideoDownloaderTask(@NonNull final VideoDownloaderListener listener) {
            mListener = listener;
            mWeakSelf = new WeakReference<VideoDownloaderTask>(this);
            sDownloaderTasks.add(mWeakSelf);
        }

        @Override
        protected Boolean doInBackground(final String... params) {
            if (params == null || params.length == 0 || params[0] == null) {
                MoPubLog.d("VideoDownloader task tried to execute null or empty url.");
                return false;
            }

            final String videoUrl = params[0];
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = MoPubHttpUrlConnection.getHttpUrlConnection(videoUrl);
                inputStream = new BufferedInputStream(urlConnection.getInputStream());

                // Check status code range
                int statusCode = urlConnection.getResponseCode();
                if (statusCode < HttpURLConnection.HTTP_OK
                        || statusCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    MoPubLog.d("VideoDownloader encountered unexpected statusCode: " +
                            statusCode);
                    return false;
                }

                // Check video size below maximum
                int contentLength = urlConnection.getContentLength();
                if (contentLength > MAX_VIDEO_SIZE) {
                    MoPubLog.d(String.format(
                            "VideoDownloader encountered video larger than disk cap. " +
                                    "(%d bytes / %d maximum).",
                            contentLength,
                            MAX_VIDEO_SIZE));
                    return false;
                }

                boolean diskPutResult = CacheService.putToDiskCache(videoUrl, inputStream);
                return diskPutResult;
            } catch (Exception e) {
                MoPubLog.d("VideoDownloader task threw an internal exception.", e);
                return false;
            } finally {
                Streams.closeStream(inputStream);
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (isCancelled()) {
                onCancelled();
                return;
            }

            sDownloaderTasks.remove(mWeakSelf);

            if (success == null) {
                mListener.onComplete(false);
                return;
            }

            mListener.onComplete(success);
        }

        @Override
        protected void onCancelled() {
            MoPubLog.d("VideoDownloader task was cancelled.");
            sDownloaderTasks.remove(mWeakSelf);
            mListener.onComplete(false);
        }
    }

    @Deprecated
    @VisibleForTesting
    public static Deque<WeakReference<VideoDownloaderTask>> getDownloaderTasks() {
        return sDownloaderTasks;
    }

    @Deprecated
    @VisibleForTesting
    public static void clearDownloaderTasks() {
        sDownloaderTasks.clear();
    }
}
