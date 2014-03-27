/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import com.mopub.mobileads.factories.AdFetchTaskFactory;
import com.mopub.mobileads.util.AsyncTasks;

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
        Log.i("MoPub", "Fetching ad for task #" + getCurrentTaskId());

        if (mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }

        mCurrentTask = AdFetchTaskFactory.create(mTaskTracker, mAdViewController, mUserAgent, mTimeoutMilliseconds);

        try {
            AsyncTasks.safeExecuteOnExecutor(mCurrentTask, url);
        } catch (Exception exception) {
            Log.d("MoPub", "Error executing AdFetchTask", exception);
        }
    }

    public void cancelFetch() {
        if (mCurrentTask != null) {
            Log.i("MoPub", "Canceling fetch ad for task #" + getCurrentTaskId());
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
