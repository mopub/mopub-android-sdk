package com.mopub.common.event;

import android.app.Activity;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class EventDispatcherTest {

    private EventDispatcher subject;
    private List<EventRecorder> recorders;
    @Mock private EventRecorder mockEventRecorder1;
    @Mock private EventRecorder mockEventRecorder2;
    @Mock private HandlerThread mockHandlerThread;

    @Before
    public void setUp() {
        recorders = new ArrayList<EventRecorder>();
        recorders.add(mockEventRecorder1);
        recorders.add(mockEventRecorder2);
    }

    @Test
    public void handler_handleMessage_shouldCallRecordOnAllRecorders() throws Exception {
        Message message = new Message();
        message.obj = mock(Event.class);

        subject = new EventDispatcher(recorders, Looper.getMainLooper());
        subject.getHandlerCallback().handleMessage(message);

        verify(mockEventRecorder1).record(eq((Event) message.obj));
        verify(mockEventRecorder2).record(eq((Event) message.obj));
    }

    @Test
    public void handler_handleMessage_withNonBaseEventTypeMessageShouldNotRecordOnAnyRecorders() throws Exception {
        Message message = new Message();
        message.obj = mock(Activity.class);

        subject = new EventDispatcher(recorders, Looper.getMainLooper());
        subject.getHandlerCallback().handleMessage(message);

        verify(mockEventRecorder1, never()).record(any(BaseEvent.class));
        verify(mockEventRecorder2, never()).record(any(BaseEvent.class));
    }
}
