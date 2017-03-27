package com.mopub.common.util;

import android.os.AsyncTask;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

import java.util.concurrent.Executor;

public class AsyncTasks {
    private static Executor sExecutor;

    static {
        init();
    }

    // This is in a separate method rather than a static block to pass lint.
    private static void init() {
        // Reuse the async task executor
        sExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
    }

    @VisibleForTesting
    public static void setExecutor(Executor executor) {
        sExecutor = executor;
    }

    /**
     * Starting with Honeycomb, default AsyncTask#execute behavior runs the tasks serially. This
     * method attempts to force these AsyncTasks to run in parallel with a ThreadPoolExecutor.
     */
    public static <P> void safeExecuteOnExecutor(AsyncTask<P, ?, ?> asyncTask, P... params) {
        Preconditions.checkNotNull(asyncTask, "Unable to execute null AsyncTask.");
        Preconditions.checkUiThread("AsyncTask must be executed on the main thread");

        asyncTask.executeOnExecutor(sExecutor, params);
    }
}
