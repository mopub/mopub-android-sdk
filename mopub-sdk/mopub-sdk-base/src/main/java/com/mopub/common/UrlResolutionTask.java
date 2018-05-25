package com.mopub.common;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@VisibleForTesting
public class UrlResolutionTask extends AsyncTask<String, Void, String> {
    private static final int REDIRECT_LIMIT = 10;

    interface UrlResolutionListener {
        void onSuccess(@NonNull final String resolvedUrl);
        void onFailure(@NonNull final String message, @Nullable final Throwable throwable);
    }

    @NonNull private final UrlResolutionListener mListener;

    public static void getResolvedUrl(@NonNull final String urlString,
            @NonNull final UrlResolutionListener listener) {
        final UrlResolutionTask urlResolutionTask = new UrlResolutionTask(listener);

        try {
            AsyncTasks.safeExecuteOnExecutor(urlResolutionTask, urlString);
        } catch (Exception e) {
            listener.onFailure("Failed to resolve url", e);
        }
    }

    UrlResolutionTask(@NonNull UrlResolutionListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    protected String doInBackground(@Nullable String... urls) {
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
                if (!UrlAction.OPEN_IN_APP_BROWSER.shouldTryHandlingUrl(Uri.parse(locationUrl))) {
                    return locationUrl;
                }

                // Do not resolve redirects if native browser will handle the URL.
                if (UrlAction.OPEN_NATIVE_BROWSER.shouldTryHandlingUrl(Uri.parse(locationUrl))) {
                    return locationUrl;
                }

                previousUrl = locationUrl;
                locationUrl = getRedirectLocation(locationUrl);
                redirectCount++;
            }

        } catch (IOException e) {
            return null;
        } catch (URISyntaxException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }

        return previousUrl;
    }

    @Nullable
    private String getRedirectLocation(@NonNull final String urlString) throws IOException,
            URISyntaxException {
        final URL url = new URL(urlString);

        HttpURLConnection httpUrlConnection = null;
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setInstanceFollowRedirects(false);

            return resolveRedirectLocation(urlString, httpUrlConnection);
        } finally {
            if (httpUrlConnection != null) {
                final InputStream is = httpUrlConnection.getInputStream();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        MoPubLog.d("IOException when closing httpUrlConnection. Ignoring.");
                    }
                }
                httpUrlConnection.disconnect();
            }
        }
    }

    @VisibleForTesting
    @Nullable
    static String resolveRedirectLocation(@NonNull final String baseUrl,
            @NonNull final HttpURLConnection httpUrlConnection) throws IOException, URISyntaxException {
        final URI baseUri = new URI(baseUrl);
        final int responseCode = httpUrlConnection.getResponseCode();
        final String redirectUrl = httpUrlConnection.getHeaderField("Location");
        String result = null;

        if (responseCode >= 300 && responseCode < 400) {
            try {
                // If redirectUrl is a relative path, then resolve() will correctly complete the path;
                // otherwise, resolve() will return the redirectUrl
                result =  baseUri.resolve(redirectUrl).toString();
            } catch (IllegalArgumentException e) {
                // Ensure the request is cancelled instead of resolving an intermediary URL
                MoPubLog.e("Invalid URL redirection. baseUrl=" + baseUrl + "\n redirectUrl=" + redirectUrl);
                throw new URISyntaxException(redirectUrl, "Unable to parse invalid URL");
            } catch (NullPointerException e) {
                MoPubLog.e("Invalid URL redirection. baseUrl=" + baseUrl + "\n redirectUrl=" + redirectUrl);
                throw e;
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(@Nullable final String resolvedUrl) {
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

        mListener.onFailure("Task for resolving url was cancelled", null);
    }
}


