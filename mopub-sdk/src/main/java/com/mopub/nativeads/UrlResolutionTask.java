package com.mopub.nativeads;

import android.os.AsyncTask;

import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.IntentUtils;
import com.mopub.common.logging.MoPubLog;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class UrlResolutionTask extends AsyncTask<String, Void, String> {
    private static final int REDIRECT_LIMIT = 10;

    interface UrlResolutionListener {
        void onSuccess(String resolvedUrl);
        void onFailure();
    }

    private final UrlResolutionListener mListener;

    public static void getResolvedUrl(final String urlString, final UrlResolutionListener listener) {
        final UrlResolutionTask urlResolutionTask = new UrlResolutionTask(listener);

        try {
            AsyncTasks.safeExecuteOnExecutor(urlResolutionTask, urlString);
        } catch (Exception e) {
            MoPubLog.d("Failed to resolve url", e);

            listener.onFailure();
        }
    }

    UrlResolutionTask(UrlResolutionListener listener) {
        mListener = listener;
    }

    @Override
    protected String doInBackground(String... urls) {
        if (urls == null || urls.length == 0) {
            return null;
        }

        String previousUrl = null;
        try {
            String locationUrl = urls[0];

            int redirectCount = 0;
            while (locationUrl != null && redirectCount < REDIRECT_LIMIT) {
                // if location url is not http(s), assume it's an Android deep link
                // this scheme will fail URL validation so we have to check early
                if (!IntentUtils.isHttpUrl(locationUrl)) {
                    return locationUrl;
                }

                previousUrl = locationUrl;
                locationUrl = getRedirectLocation(locationUrl);
                redirectCount++;
            }

        } catch (IOException e) {
            return null;
        }

        return previousUrl;
    }

    private String getRedirectLocation(final String urlString) throws IOException {
        final URL url = new URL(urlString);

        HttpURLConnection httpUrlConnection = null;
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setInstanceFollowRedirects(false);

            int responseCode = httpUrlConnection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                return httpUrlConnection.getHeaderField("Location");
            } else {
                return null;
            }
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    @Override
    protected void onPostExecute(final String resolvedUrl) {
        super.onPostExecute(resolvedUrl);

        if (isCancelled() || resolvedUrl == null) {
            onCancelled();
        } else {
            mListener.onSuccess(resolvedUrl);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        mListener.onFailure();
    }
}


