package com.mopub.common.event;

import android.os.Handler;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.ScribeBackoffPolicy;
import com.mopub.network.ScribeRequest;
import com.mopub.network.ScribeRequestManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.Queue;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ScribeEventRecorderTest {

    private ScribeEventRecorder subject;
    @Mock private EventSampler mockEventSampler;
    @Mock private Queue<BaseEvent> mockQueue;
    @Mock private EventSerializer mockEventSerializer;
    @Mock private ScribeRequestManager mockScribeRequestManager;
    @Mock private Handler mockHandler;
    @Mock private Event mockEvent;

    @Before
    public void setUp() {
        subject = new ScribeEventRecorder(
                mockEventSampler,
                mockQueue,
                mockEventSerializer,
                mockScribeRequestManager,
                mockHandler
        );

        when(mockEventSampler.sample(any(Event.class))).thenReturn(true);
    }

    @Test
    public void record_shouldSampleEvent() throws Exception {
        subject.record(mockEvent);
        verify(mockEventSampler).sample(mockEvent);
    }

    @Test
    public void record_withQueueSizeBelowSendThreshold_shouldQueueEvent_shouldNotSendEvents_shouldScheduleNextPoll() throws Exception {
        when(mockQueue.size()).thenReturn(99);

        subject.record(mockEvent);

        verify(mockQueue).add(mockEvent);
        verify(mockScribeRequestManager, never()).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
        verify(mockHandler).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), eq(Long.valueOf(120000)));
    }

    @Test
    public void record_withQueueSizeAtSendThreshold_shouldQueueEvent_shouldSendEvents_shouldScheduleNextPoll() throws Exception {
        when(mockQueue.size()).thenReturn(100);
        when(mockQueue.peek()).thenReturn(mockEvent);
        when(mockQueue.poll()).thenReturn(mockEvent);

        subject.record(mockEvent);

        verify(mockQueue).add(mockEvent);
        verify(mockScribeRequestManager).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
        verify(mockHandler).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), eq(Long.valueOf(120000)));
    }

    @Test
    public void record_withQueueSizeAtQueueLimit_shouldNotQueueEvent_shouldNotSendEvents_shouldNotScheduleNextPoll() throws Exception {
        when(mockQueue.size()).thenReturn(500);

        subject.record(mockEvent);

        verify(mockQueue, never()).add(mockEvent);
        verify(mockScribeRequestManager, never()).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
        verify(mockHandler, never()).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), anyLong());
    }

    @Test
    public void sendEvents_shouldDequeueEvents_shouldAddRequestToScribeRequestManager() throws Exception {
        when(mockQueue.size()).thenReturn(1);
        when(mockQueue.peek()).thenReturn(mockEvent).thenReturn(null);
        when(mockQueue.poll()).thenReturn(mockEvent).thenReturn(null);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ScribeRequest scribeRequest = ((ScribeRequest.ScribeRequestFactory) invocation.getArguments()[0]).createRequest(null);
                assertThat(scribeRequest.getUrl()).isEqualTo("https://analytics.mopub.com/i/jot/exchange_client_event");
                assertThat(scribeRequest.getEvents()).containsOnly(mockEvent);
                return null;
            }
        }).when(mockScribeRequestManager).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));

        subject.sendEvents();

        verify(mockQueue, times(2)).peek();
        verify(mockQueue, times(1)).poll();
        verify(mockScribeRequestManager).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
    }

    @Test
    public void sendEvents_withRequestInFlightShouldReturnFast() throws Exception {
        when(mockScribeRequestManager.isAtCapacity()).thenReturn(true);

        subject.sendEvents();

        verify(mockQueue, never()).poll();
        verify(mockScribeRequestManager, never()).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
    }
    
    @Test
    public void dequeEvents_withQueueSizeGreaterThanEventSendThreshhold_shouldDequeueUpToEventSendThreshhold() throws Exception {
        when(mockQueue.size()).thenReturn(101);
        when(mockQueue.peek()).thenReturn(mockEvent);
        when(mockQueue.poll()).thenReturn(mockEvent);

        List<BaseEvent> events = subject.dequeueEvents();

        verify(mockQueue, times(101)).peek();
        verify(mockQueue, times(100)).poll();
        assertThat(events.size()).isEqualTo(100);
    }

    @Test
    public void dequeEvents_withQueueSizeLessThanEventSendThreshhold_shouldDequeueQueueSize() throws Exception {
        when(mockQueue.size()).thenReturn(99);

        when(mockQueue.peek()).thenAnswer(new Answer<BaseEvent>() {
            int i;
            @Override
            public BaseEvent answer(InvocationOnMock invocation) throws Throwable {
                return i++ < 99 ? mockEvent : null;
            }
        });

        when(mockQueue.poll()).thenAnswer(new Answer<BaseEvent>() {
            int i;
            @Override
            public BaseEvent answer(InvocationOnMock invocation) throws Throwable {
                return i++ < 99 ? mockEvent : null;
            }
        });

        List<BaseEvent> events = subject.dequeueEvents();

        verify(mockQueue, times(100)).peek();
        verify(mockQueue, times(99)).poll();
        assertThat(events.size()).isEqualTo(99);
    }

    @Test
    public void scheduleNextPoll_shouldPostDelayedPollingRunnable() throws Exception {
        when(mockHandler.hasMessages(0)).thenReturn(false);
        when(mockQueue.isEmpty()).thenReturn(false);

        subject.scheduleNextPoll();

        verify(mockHandler).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), eq(Long.valueOf(120000)));
    }

    @Test
    public void scheduleNextPoll_withPollScheduled_shouldNotPostDelayedPollingRunnable() throws Exception {
        when(mockHandler.hasMessages(0)).thenReturn(true);
        when(mockQueue.isEmpty()).thenReturn(false);

        subject.scheduleNextPoll();

        verify(mockHandler, never()).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), anyLong());
    }

    @Test
    public void scheduleNextPoll_withEmptyRequestQueue_shouldNotPostDelayedPollingRunnable() throws Exception {
        when(mockHandler.hasMessages(0)).thenReturn(false);
        when(mockQueue.isEmpty()).thenReturn(true);

        subject.scheduleNextPoll();

        verify(mockHandler, never()).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), anyLong());
    }

    @Test
    public void PollingRunnable_run_shouldSendEvents_shouldScheduleNextPoll() throws Exception {
        when(mockQueue.size()).thenReturn(100);
        when(mockQueue.peek()).thenReturn(mockEvent);
        when(mockQueue.poll()).thenReturn(mockEvent);

        ScribeEventRecorder.PollingRunnable pollingRunnable = subject.new PollingRunnable();
        pollingRunnable.run();

        verify(mockScribeRequestManager).makeRequest(any(ScribeRequest.ScribeRequestFactory.class), any(ScribeBackoffPolicy.class));
        verify(mockHandler).postDelayed(any(ScribeEventRecorder.PollingRunnable.class), eq(Long.valueOf(120000)));
    }
}

