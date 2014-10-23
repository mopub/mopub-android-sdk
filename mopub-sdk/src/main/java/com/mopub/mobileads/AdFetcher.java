package com.mopub.mobileads;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.mobileads.factories.AdFetchTaskFactory;

/*
 * AdFetcher is a delegate of an AdViewController that handles loading ad data over a
 * network connection. The ad is fetched in a background thread by executing
 * AdFetchTask, which is an AsyncTask subclass. This class gracefully handles
 * the changes to AsyncTask in Android 4.0.1 (we continue to run parallel to
 * the app developer's background tasks). Further, AdFetcher keeps track of
 * the last completed task to prevent out-of-order execution.
 */
public class AdFetcher {
    public static final String HTML_RESPONSE_BODY_KEY = "Html-Response-Body";
    public static final String REDIRECT_URL_KEY = "Redirect-Url";
    public static final String CLICKTHROUGH_URL_KEY = "Clickthrough-Url";
    public static final String SCROLLABLE_KEY = "Scrollable";
    public static final String AD_CONFIGURATION_KEY = "Ad-Configuration";

    private int mTimeoutMilliseconds = 10000;
    private AdViewController mAdViewController;

    private AdFetchTask mCurrentTask;
    private String mUserAgent;
    private final TaskTracker mTaskTracker;

    enum FetchStatus {
        NOT_SET,
        FETCH_CANCELLED,
        INVALID_SERVER_RESPONSE_BACKOFF,
        INVALID_SERVER_RESPONSE_NOBACKOFF,
        CLEAR_AD_TYPE,
        AD_WARMING_UP;
    }

    public AdFetcher(AdViewController adview, String userAgent) {
        mAdViewController = adview;
        mUserAgent = userAgent;
        mTaskTracker = new TaskTracker();
    }

    public void fetchAdForUrl(String url) {
        mTaskTracker.newTaskStarted();
        MoPubLog.i("Fetching ad for task #" + getCurrentTaskId());

        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }

        mCurrentTask = AdFetchTaskFactory.create(mTaskTracker, mAdViewController, mUserAgent, mTimeoutMilliseconds);

        try {
            AsyncTasks.safeExecuteOnExecutor(mCurrentTask, url);
        } catch (Exception exception) {
            MoPubLog.d("Error executing AdFetchTask", exception);
        }
    }

    public void cancelFetch() {
        if (mCurrentTask != null) {
            MoPubLog.i("Canceling fetch ad for task #" + getCurrentTaskId());
            mCurrentTask.cancel(true);
        }
    }

    void cleanup() {
        cancelFetch();

        mAdViewController = null;
        mUserAgent = "";
    }

    protected void setTimeout(int milliseconds) {
        mTimeoutMilliseconds = milliseconds;
    }

    private long getCurrentTaskId() {
        return mTaskTracker.getCurrentTaskId();
    }
}
