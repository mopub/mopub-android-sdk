package com.mopub.mobileads;

import android.os.AsyncTask;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HttpClientFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;

import static com.mopub.common.util.ResponseHeader.AD_TYPE;
import static com.mopub.common.util.ResponseHeader.USER_AGENT;
import static com.mopub.common.util.ResponseHeader.WARMUP;
import static com.mopub.mobileads.util.HttpResponses.extractHeader;

public class AdFetchTask extends AsyncTask<String, Void, AdLoadTask> {
    private TaskTracker mTaskTracker;
    private AdViewController mAdViewController;
    private Exception mException;
    private HttpClient mHttpClient;
    private long mTaskId;
    private String mUserAgent;

    private AdFetcher.FetchStatus mFetchStatus = AdFetcher.FetchStatus.NOT_SET;
    private static final int MAXIMUM_REFRESH_TIME_MILLISECONDS = 600000;
    private static final double EXPONENTIAL_BACKOFF_FACTOR = 1.5;

    public AdFetchTask(TaskTracker taskTracker, AdViewController adViewController, String userAgent, int timeoutMilliseconds) {
        mTaskTracker = taskTracker;

        mAdViewController = adViewController;
        mHttpClient = HttpClientFactory.create(timeoutMilliseconds);
        mTaskId = mTaskTracker.getCurrentTaskId();
        mUserAgent = userAgent;
    }

    @Override
    protected AdLoadTask doInBackground(String... urls) {
        AdLoadTask result = null;
        try {
            result = fetch(urls[0]);
        } catch (Exception exception) {
            mException = exception;
        } finally {
            shutdownHttpClient();
        }
        return result;
    }

    private AdLoadTask fetch(String url) throws Exception {
        HttpGet httpget = new HttpGet(url);
        httpget.addHeader(USER_AGENT.getKey(), mUserAgent);

        if (!isStateValid()) return null;

        HttpResponse response = mHttpClient.execute(httpget);

        if (!isResponseValid(response)) return null;

        mAdViewController.configureUsingHttpResponse(response);

        if (!responseContainsContent(response)) return null;

        return AdLoadTask.fromHttpResponse(response, mAdViewController);
    }

    private boolean responseContainsContent(HttpResponse response) {
        // Ensure that the ad is not warming up.
        if ("1".equals(extractHeader(response, WARMUP))) {
            MoPubLog.d("Ad Unit (" + mAdViewController.getAdUnitId() + ") is still warming up. " +
                    "Please try again in a few minutes.");
            mFetchStatus = AdFetcher.FetchStatus.AD_WARMING_UP;
            return false;
        }

        // Ensure that the ad type header is valid and not "clear".
        String adType = extractHeader(response, AD_TYPE);
        if ("clear".equals(adType)) {
            MoPubLog.d("No ads found for adunit (" + mAdViewController.getAdUnitId() + ").");
            mFetchStatus = AdFetcher.FetchStatus.CLEAR_AD_TYPE;
            return false;
        }

        return true;
    }

    private boolean isResponseValid(HttpResponse response) {
        if (response == null || response.getEntity() == null) {
            MoPubLog.d("MoPub server returned null response.");
            mFetchStatus = AdFetcher.FetchStatus.INVALID_SERVER_RESPONSE_NOBACKOFF;
            return false;
        }

        final int statusCode = response.getStatusLine().getStatusCode();

        // Client and Server HTTP errors should result in an exponential backoff
        if (statusCode >= 400) {
            MoPubLog.d("Server error: returned HTTP status code " + Integer.toString(statusCode) +
                    ". Please try again.");
            mFetchStatus = AdFetcher.FetchStatus.INVALID_SERVER_RESPONSE_BACKOFF;
            return false;
        }
        // Other non-200 HTTP status codes should still fail
        else if (statusCode != HttpStatus.SC_OK) {
            MoPubLog.d("MoPub server returned invalid response: HTTP status code " +
                    Integer.toString(statusCode) + ".");
            mFetchStatus = AdFetcher.FetchStatus.INVALID_SERVER_RESPONSE_NOBACKOFF;
            return false;
        }
        return true;
    }

    private boolean isStateValid() {
        // We check to see if this AsyncTask was cancelled, as per
        // http://developer.android.com/reference/android/os/AsyncTask.html
        if (isCancelled()) {
            mFetchStatus = AdFetcher.FetchStatus.FETCH_CANCELLED;
            return false;
        }

        if (mAdViewController == null || mAdViewController.isDestroyed()) {
            MoPubLog.d("Error loading ad: AdViewController has already been GCed or destroyed.");
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(AdLoadTask adLoadTask) {
        if (!isMostCurrentTask()) {
            MoPubLog.d("Ad response is stale.");
            cleanup();
            return;
        }

        // If cleanup() has already been called on the AdViewController, don't proceed.
        if (mAdViewController == null || mAdViewController.isDestroyed()) {
            if (adLoadTask != null) {
                adLoadTask.cleanup();
            }
            mTaskTracker.markTaskCompleted(mTaskId);
            cleanup();
            return;
        }

        if (adLoadTask == null) {
            if (mException != null) {
                MoPubLog.d("Exception caught while loading ad: " + mException);
            }

            MoPubErrorCode errorCode;
            switch (mFetchStatus) {
                case NOT_SET:
                    errorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case FETCH_CANCELLED:
                    errorCode = MoPubErrorCode.CANCELLED;
                    break;
                case INVALID_SERVER_RESPONSE_BACKOFF:
                case INVALID_SERVER_RESPONSE_NOBACKOFF:
                    errorCode = MoPubErrorCode.SERVER_ERROR;
                    break;
                case CLEAR_AD_TYPE:
                case AD_WARMING_UP:
                    errorCode = MoPubErrorCode.NO_FILL;
                    break;
                default:
                    errorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
            }

            mAdViewController.adDidFail(errorCode);

            /*
             * There are numerous reasons for the ad fetch to fail, but only in the specific
             * case of actual server failure should we exponentially back off.
             *
             * Note: We place the exponential backoff after AdViewController's adDidFail because we only
             * want to increase refresh times after the first failure refresh timer is
             * scheduled, and not before.
             */
            if (mFetchStatus == AdFetcher.FetchStatus.INVALID_SERVER_RESPONSE_BACKOFF) {
                exponentialBackoff();
                mFetchStatus = AdFetcher.FetchStatus.NOT_SET;
            }
        } else {
            adLoadTask.execute();
            adLoadTask.cleanup();
        }

        mTaskTracker.markTaskCompleted(mTaskId);
        cleanup();
    }

    @Override
    protected void onCancelled() {
        if (!isMostCurrentTask()) {
            MoPubLog.d("Ad response is stale.");
            cleanup();
            return;
        }

        MoPubLog.d("Ad loading was cancelled.");
        if (mException != null) {
            MoPubLog.d("Exception caught while loading ad: " + mException);
        }
        mTaskTracker.markTaskCompleted(mTaskId);
        cleanup();
    }

    /* This helper function is called when a 4XX or 5XX error is received during an ad fetch.
     * It exponentially increases the parent AdViewController's refreshTime up to a specified cap.
     */
    private void exponentialBackoff() {
        if (mAdViewController == null) {
            return;
        }

        int refreshTimeMilliseconds = mAdViewController.getRefreshTimeMilliseconds();

        refreshTimeMilliseconds = (int) (refreshTimeMilliseconds * EXPONENTIAL_BACKOFF_FACTOR);
        if (refreshTimeMilliseconds > MAXIMUM_REFRESH_TIME_MILLISECONDS) {
            refreshTimeMilliseconds = MAXIMUM_REFRESH_TIME_MILLISECONDS;
        }

        mAdViewController.setRefreshTimeMilliseconds(refreshTimeMilliseconds);
    }

    private void cleanup() {
        mTaskTracker = null;
        mException = null;
        mFetchStatus = AdFetcher.FetchStatus.NOT_SET;
    }

    private void shutdownHttpClient() {
        if (mHttpClient != null) {
            ClientConnectionManager manager = mHttpClient.getConnectionManager();
            if (manager != null) {
                manager.shutdown();
            }
            mHttpClient = null;
        }
    }

    private boolean isMostCurrentTask() {
        // if we've been cleaned up already, then we're definitely not the current task
        return (mTaskTracker == null) ? false : mTaskTracker.isMostCurrentTask(mTaskId);
    }
}
