package com.mopub.mobileads;

import android.os.Handler;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class VastVideoViewCountdownRunnableTest {

    @Mock VastVideoViewController mockVideoViewController;
    @Mock Handler mockHandler;

    VastVideoViewCountdownRunnable subject;

    @Before
    public void setup() {
        subject = new VastVideoViewCountdownRunnable(mockVideoViewController, mockHandler);
    }

    @Test
    public void doWork_whenShouldBeInteractable_shouldCallMakeVideoInteractable() {
        when(mockVideoViewController.shouldBeInteractable()).thenReturn(true);

        subject.doWork();

        verify(mockVideoViewController).makeVideoInteractable();
        verify(mockVideoViewController).updateDuration();
    }

    @Test
    public void doWork_whenShouldNotBeInteractable_shouldNotCallMakeVideoInteractable() {
        when(mockVideoViewController.shouldBeInteractable()).thenReturn(false);

        subject.doWork();

        verify(mockVideoViewController, never()).makeVideoInteractable();
        verify(mockVideoViewController).updateDuration();
    }

    @Test
    public void doWork_whenShouldShowCountdown_shouldCallUpdateCountdown() {
        when(mockVideoViewController.shouldShowCountdown()).thenReturn(true);

        subject.doWork();

        verify(mockVideoViewController).updateCountdown();
        verify(mockVideoViewController).updateDuration();
    }

    @Test
    public void run_whenShouldNotShowCountdown_shouldNotCallUpdateCountdown() {
        when(mockVideoViewController.shouldShowCountdown()).thenReturn(false);

        subject.doWork();

        verify(mockVideoViewController, never()).updateCountdown();
        verify(mockVideoViewController).updateDuration();
    }
}
