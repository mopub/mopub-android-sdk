package com.mopub.mobileads;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import com.mopub.common.CacheService;
import com.mopub.common.HttpClient;
import com.mopub.common.logging.MoPubLog;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VastVideoDownloadTask extends AsyncTask<String, Void, Boolean> {
    private static final int MAX_VIDEO_SIZE = 25 * 1024 * 1024; // 25 MiB

    public interface VastVideoDownloadTaskListener {
        public void onComplete(boolean success);
    }

    private final VastVideoDownloadTaskListener mVastVideoDownloadTaskListener;

    public VastVideoDownloadTask(final VastVideoDownloadTaskListener listener) {
        mVastVideoDownloadTaskListener = listener;
    }

    @Override
    protected Boolean doInBackground(final String... params) {
        if (params == null || params.length == 0 || params[0] == null) {
            return false;
        }

        final String videoUrl = params[0];
        AndroidHttpClient httpClient = null;
        try {
            httpClient = HttpClient.getHttpClient();
            final HttpGet httpget = HttpClient.initializeHttpGet(videoUrl);
            final HttpResponse response = httpClient.execute(httpget);

            if (response == null || response.getEntity() == null) {
                throw new IOException("Obtained null response from video url: " + videoUrl);
            }

            if (response.getEntity().getContentLength() > MAX_VIDEO_SIZE) {
                throw new IOException("Video exceeded max download size");
            }

            final InputStream inputStream = new BufferedInputStream(response.getEntity().getContent());
            final boolean diskPutResult = CacheService.putToDiskCache(videoUrl, inputStream);
            inputStream.close();
            return diskPutResult;
        } catch (Exception e) {
            MoPubLog.d("Failed to download video: " + e.getMessage());
            return false;
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    @Override
    protected void onCancelled() {
        onPostExecute(false);
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (mVastVideoDownloadTaskListener != null) {
            mVastVideoDownloadTaskListener.onComplete(success);
        }
    }
}
