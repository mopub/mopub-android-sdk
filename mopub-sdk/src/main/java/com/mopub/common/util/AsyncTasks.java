package com.mopub.common.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncTasks {
    private static Executor sExecutor;

    static {
        init();
    }

    // This is in a separate method rather than a static block to pass lint.
    @TargetApi(VERSION_CODES.HONEYCOMB)
    private static void init() {
        // Reuse the async task executor if possible
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            sExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
        } else {
            sExecutor = Executors.newSingleThreadExecutor();
        }
    }

    @VisibleForTesting
    public static void setExecutor(Executor executor) {
        sExecutor = executor;
    }

    /**
     * Starting with Honeycomb, default AsyncTask#execute behavior runs the tasks serially. This
     * method attempts to force these AsyncTasks to run in parallel with a ThreadPoolExecutor, if
     * possible.
     */
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static <P> void safeExecuteOnExecutor(AsyncTask<P, ?, ?> asyncTask, P... params) {
        Preconditions.checkNotNull(asyncTask, "Unable to execute null AsyncTask.");
        Preconditions.checkUiThread("AsyncTask must be executed on the main thread");

        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            asyncTask.executeOnExecutor(sExecutor, params);
        } else {
            asyncTask.execute(params);
        }
    }
}
