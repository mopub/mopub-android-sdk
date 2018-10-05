// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util.test.support;

import android.os.AsyncTask;

import com.mopub.common.util.AsyncTasks;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;
import java.util.List;

@Implements(value = AsyncTasks.class, callThroughByDefault = false)
public class ShadowAsyncTasks {
    private static boolean sWasCalled;
    private static AsyncTask<?, ?, ?> sAsyncTask;
    private static List<?> sParams;

    @Implementation
    public static <P> void safeExecuteOnExecutor(AsyncTask<P, ?, ?> asyncTask, P... params)
            throws IllegalArgumentException, IllegalStateException {
        sWasCalled = true;
        sAsyncTask = asyncTask;
        sParams = Arrays.asList(params);
    }

    public static boolean wasCalled() {
        return sWasCalled;
    }

    public static AsyncTask<?, ?, ?> getLatestAsyncTask() {
        return sAsyncTask;
    }

    public static List<?> getLatestParams() {
        return sParams;
    }

    public static void reset() {
        sWasCalled = false;
        sAsyncTask = null;
        sParams = null;
    }
}
