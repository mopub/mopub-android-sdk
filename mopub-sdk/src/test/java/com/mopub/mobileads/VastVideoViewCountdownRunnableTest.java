package com.mopub.mobileads;

import android.os.Handler;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
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

        verify(mockVideoViewController).updateCountdown();
        verify(mockVideoViewController).makeVideoInteractable();
    }

    @Test
    public void doWork_whenShouldNotBeInteractable_shouldNotCallMakeVideoInteractable() {
        when(mockVideoViewController.shouldBeInteractable()).thenReturn(false);

        subject.doWork();

        verify(mockVideoViewController).updateCountdown();
        verify(mockVideoViewController, never()).makeVideoInteractable();
    }
}
