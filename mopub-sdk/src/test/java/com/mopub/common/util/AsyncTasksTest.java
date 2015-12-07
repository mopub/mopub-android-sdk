package com.mopub.common.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.Looper;

import com.mopub.TestSdkHelper;
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
    public void safeExecuteOnExecutor_beforeHoneycomb_shouldCallExecuteWithParams() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.GINGERBREAD_MR1);
        AsyncTasks.safeExecuteOnExecutor(asyncTask, "hello");

        verify(asyncTask).execute(eq("hello"));
    }


    @Test
    public void safeExecutorOnExecutor_beforeHoneycomb_withNullParam_shouldCallExecute() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.GINGERBREAD_MR1);

        AsyncTasks.safeExecuteOnExecutor(asyncTask, (String) null);

        verify(asyncTask).execute(eq((String) null));
    }


    @Test
    public void safeExecutorOnExecutor_beforeHoneycomb_withNullAsyncTask_shouldThrowIllegalArgumentException() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.GINGERBREAD_MR1);
        try {
            AsyncTasks.safeExecuteOnExecutor(null, "hello");
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException exception) {
            // pass
        }
    }


    @Test
    public void safeExecutorOnExecutor_beforeHoneycomb_runningOnABackgroundThread_shouldThrowIllegalStateException() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.GINGERBREAD_MR1);
        ensureFastFailWhenTaskIsRunOnBackgroundThread();
    }


    @TargetApi(VERSION_CODES.HONEYCOMB)
    @Test
    public void safeExecuteOnExecutor_atLeastHoneycomb_shouldCallExecuteWithParamsWithExecutor() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.HONEYCOMB);
        AsyncTasks.safeExecuteOnExecutor(asyncTask, "goodbye");

        verify(asyncTask).executeOnExecutor(any(Executor.class), eq("goodbye"));
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    @Test
    public void safeExecutorOnExecutor_atLeastHoneycomb_withNullParam_shouldCallExecuteWithParamsWithExecutor() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.HONEYCOMB);
        AsyncTasks.safeExecuteOnExecutor(asyncTask, (String) null);

        verify(asyncTask).executeOnExecutor(any(Executor.class), eq((String) null));
    }


    @Test
    public void safeExecutorOnExecutor_atLeastHoneycomb_withNullAsyncTask_shouldThrowIllegalArgumentException() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.HONEYCOMB);
        try {
            AsyncTasks.safeExecuteOnExecutor(null, "hello");
            fail("Should have thrown NullPointerException");
        } catch (NullPointerException exception) {
            // pass
        }
    }


    @Test
    public void safeExecutorOnExecutor_atLeastHoneycomb_runningOnABackgroundThread_shouldThrowIllegalStateException() throws Exception {
        TestSdkHelper.setReportedSdkLevel(VERSION_CODES.HONEYCOMB);
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
