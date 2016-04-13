package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoViewProgressRunnableTest {
    @Mock VastVideoViewController mockVastVideoViewController;
    @Mock Context mockContext;
    @Mock Handler mockHandler;
    @Mock MoPubRequestQueue mockRequestQueue;
    @Mock VastVideoConfig mockVideoConfig;
    @Captor ArgumentCaptor<TrackingRequest> requestCaptor;
    VastVideoViewProgressRunnable subject;

    @Before
    public void setup() {
        subject = new VastVideoViewProgressRunnable(mockVastVideoViewController, mockVideoConfig,
                mockHandler);

        // Request Queue needed to verify tracking requests made.
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void doWork_whenTrackersReturned_shouldMakeTrackingRequests() {
        List<VastTracker> testTrackers = new ArrayList<VastTracker>();
        testTrackers.add(new VastAbsoluteProgressTracker("https://example.com/", 1999));
        testTrackers.add(new VastAbsoluteProgressTracker("https://example1.com/", 2000));

        when(mockVideoConfig.getUntriggeredTrackersBefore(eq(3000), eq(4000)))
                .thenReturn(testTrackers);
        when(mockVastVideoViewController.getCurrentPosition()).thenReturn(3000);
        when(mockVastVideoViewController.getDuration()).thenReturn(4000);
        when(mockVastVideoViewController.getContext()).thenReturn(mockContext);

        subject.doWork();

        verify(mockVideoConfig).getUntriggeredTrackersBefore(eq(3000), eq(4000));
        verify(mockVastVideoViewController).getCurrentPosition();
        verify(mockVastVideoViewController).getDuration();
        verify(mockVastVideoViewController).getContext();
        verify(mockVastVideoViewController).getNetworkMediaFileUrl();
        verify(mockVastVideoViewController).handleIconDisplay(eq(3000));
        verify(mockVastVideoViewController).updateProgressBar();

        // Capture request queue - should get two different trackers.
        verify(mockRequestQueue, times(2)).add(requestCaptor.capture());
        final List<TrackingRequest> allValues = requestCaptor.getAllValues();
        assertThat(allValues).hasSize(2);
        assertThat(allValues.get(0).getUrl()).isEqualTo("https://example.com/");
        assertThat(allValues.get(1).getUrl()).isEqualTo("https://example1.com/");

        verifyNoMoreInteractions(mockVastVideoViewController, mockRequestQueue);
    }

    @Test
    public void doWork_whenNoTrackersReturned_shouldNotMakeTrackingRequests() {
        List<VastTracker> testTrackers = new ArrayList<VastTracker>();

        when(mockVideoConfig.getUntriggeredTrackersBefore(eq(3000), eq(4000)))
                .thenReturn(testTrackers);
        when(mockVastVideoViewController.getCurrentPosition()).thenReturn(3000);
        when(mockVastVideoViewController.getDuration()).thenReturn(4000);
        when(mockVastVideoViewController.getContext()).thenReturn(mockContext);

        subject.doWork();

        verify(mockVideoConfig).getUntriggeredTrackersBefore(eq(3000), eq(4000));
        verify(mockVastVideoViewController).getCurrentPosition();
        verify(mockVastVideoViewController).getDuration();
        verify(mockVastVideoViewController).handleIconDisplay(eq(3000));
        verify(mockVastVideoViewController).updateProgressBar();

        verifyNoMoreInteractions(mockVastVideoViewController, mockRequestQueue);
    }
}
