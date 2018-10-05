// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.concurrent.Executor;

public class AsyncTasks {
    private static Executor sExecutor;
    private static Handler sUiThreadHandler;

    static {
        init();
    }

    // This is in a separate method rather than a static block to pass lint.
    private static void init() {
        // Reuse the async task executor
        sExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
        sUiThreadHandler = new Handler(Looper.getMainLooper());
    }

    @VisibleForTesting
    public static void setExecutor(Executor executor) {
        sExecutor = executor;
    }

    /**
     * Starting with Honeycomb, default AsyncTask#execute behavior runs the tasks serially. This
     * method attempts to force these AsyncTasks to run in parallel with a ThreadPoolExecutor.
     */
    public static <P> void safeExecuteOnExecutor(final @NonNull AsyncTask<P, ?, ?> asyncTask, final @Nullable P... params) {
        Preconditions.checkNotNull(asyncTask, "Unable to execute null AsyncTask.");

        if (Looper.getMainLooper() == Looper.myLooper()) {
            asyncTask.executeOnExecutor(sExecutor, params);
        } else {
            MoPubLog.d("Posting AsyncTask to main thread for execution.");
            sUiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    asyncTask.executeOnExecutor(sExecutor, params);
                }
            });
        }
    }
}
