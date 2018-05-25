package com.mopub.common.util;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import com.mopub.mobileads.test.support.ThreadUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.Executor;

import static junit.framework.Assert.fail;
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
    public void safeExecuteOnExecutor_runningOnABackgroundThread_shouldThrowIllegalStateException() throws Exception {
        ensureFastFailWhenTaskIsRunOnBackgroundThread();
    }

    private void ensureFastFailWhenTaskIsRunOnBackgroundThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AsyncTasks.safeExecuteOnExecutor(asyncTask, "hello");

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            fail("Should have thrown IllegalStateException");
                        }
                    });
                } catch (IllegalStateException exception) {
                    // pass
                }
            }
        }).start();

        ThreadUtils.pause(10);
        ShadowLooper.runUiThreadTasks();
    }
}
