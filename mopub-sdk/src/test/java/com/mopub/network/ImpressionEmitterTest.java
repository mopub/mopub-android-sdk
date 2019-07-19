// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class ImpressionEmitterTest {

    private static final String mAdUnitId = "ad_unit_id";

    @Mock
    ImpressionListener listener1;
    @Mock
    ImpressionListener listener2;
    @Mock
    ImpressionData impression;

    @After
    public void tearDown() {
        ImpressionsEmitter.clear();
    }

    @Test
    public void send_oneListener_shouldCallListener() {
        ImpressionsEmitter.addListener(listener1);

        ImpressionsEmitter.send(mAdUnitId, impression);
        verify(listener1).onImpression(mAdUnitId, impression);
    }

    @Test
    public void send_twoListeners_shouldCallBoth() {
        ImpressionsEmitter.addListener(listener1);
        ImpressionsEmitter.addListener(listener2);

        ImpressionsEmitter.send(mAdUnitId, impression);

        verify(listener1).onImpression(mAdUnitId, impression);
        verify(listener2).onImpression(mAdUnitId, impression);
    }

    @Test
    public void send_removeListener_shouldNotCallAfterRemove() {
        ImpressionListener l1 = mock(ImpressionListener.class);
        ImpressionsEmitter.addListener(l1);
        ImpressionsEmitter.addListener(listener2);
        ImpressionsEmitter.removeListener(l1);

        ImpressionsEmitter.send(mAdUnitId, impression);

        verify(l1, never()).onImpression(anyString(), any(ImpressionData.class));
        verify(listener2).onImpression(mAdUnitId, impression);
    }

    @Test
    public void send_addListenerTwice_shouldMakeSingleCallback() {
        ImpressionsEmitter.addListener(listener1);
        ImpressionsEmitter.addListener(listener1);

        ImpressionsEmitter.send(mAdUnitId, impression);

        verify(listener1, times(1)).onImpression(mAdUnitId, impression);

        ImpressionsEmitter.removeListener(listener1);
        Mockito.reset(listener1);

        ImpressionsEmitter.send(mAdUnitId, impression);

        verify(listener1, never()).onImpression(anyString(), any(ImpressionData.class));
    }

    @Test
    public void send_nullImpressionData_shouldCallListener() {
        ImpressionsEmitter.addListener(listener1);

        ImpressionsEmitter.send(mAdUnitId, null);

        verify(listener1).onImpression(mAdUnitId, null);
    }

    @Test
    public void multithreading_shouldNotCrash() {
        final int THREAD_COUNT = 10;
        Thread[] testThreads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            testThreads[i] = createTestThread();
        }
        for (int i = 0; i < THREAD_COUNT; i++) {
            testThreads[i].start();
        }

        for (Thread thread : testThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void removeListener_whileEmitterIsBusy_shouldNotWaitForEmitter() throws InterruptedException {
        ImpressionListener listener = createDelayedListener();
        ImpressionsEmitter.addListener(listener);

        new Thread(new Runnable() {
            @Override
            public void run() {
                ImpressionsEmitter.send(mAdUnitId, impression);
            }
        }).start();
        Thread.sleep(100);

        long t1 = System.currentTimeMillis();
        ImpressionsEmitter.removeListener(listener);
        long t2 = System.currentTimeMillis();

        assertTrue(t2 - t1 < 100);
    }

    /*
    Unit test utility functions
     */
    private Thread createTestThread() {

        return new Thread(new Runnable() {
            @Override
            public void run() {
                final int SIZE = 100;
                ImpressionListener[] listeners = new ImpressionListener[SIZE];
                for (int i = 0; i < SIZE; i++) {
                    listeners[i] = mock(ImpressionListener.class);
                    ImpressionsEmitter.addListener(listeners[i]);
                }

                for (int i = 0; i < 150; i++) {
                    ImpressionsEmitter.send(mAdUnitId, mock(ImpressionData.class));
                }

                for (int i = 0; i < SIZE; i++) {
                    ImpressionsEmitter.removeListener(listeners[i]);
                }
            }
        });
    }

    private ImpressionListener createDelayedListener() {
        return new ImpressionListener() {
            @Override
            public void onImpression(@NonNull String adUnitId, @Nullable ImpressionData impressionData) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
