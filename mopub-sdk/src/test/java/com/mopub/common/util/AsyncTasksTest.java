// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

import android.os.AsyncTask;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class AsyncTasksTest {

    private AsyncTask<String, ?, ?> asyncTask;

    @Before
    public void setUp() throws Exception {
        asyncTask = spy(new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(String... strings) {
                return null;
            }
        });
    }

    @Test
    public void safeExecuteOnExecutor_withNullParam_shouldCallExecuteWithParamsWithExecutor() throws Exception {
        AsyncTasks.safeExecuteOnExecutor(asyncTask, (String) null);

        verify(asyncTask).executeOnExecutor(any(Executor.class), eq((String) null));
    }


    @Test(expected = NullPointerException.class)
    public void safeExecuteOnExecutor_withNullAsyncTask_shouldThrowNullPointerException() throws Exception {
        AsyncTasks.safeExecuteOnExecutor(null, "hello");
    }

    @Test
    public void safeExecuteOnExecutor_runningOnABackgroundThread_shouldStartAsyncTaskOnUiThread() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                AsyncTasks.safeExecuteOnExecutor(asyncTask, "hello");
                semaphore.release();
            }
        }).start();

        semaphore.acquire();
        ShadowLooper.runUiThreadTasks();
        verify(asyncTask).executeOnExecutor(any(Executor.class), eq("hello"));
    }
}
