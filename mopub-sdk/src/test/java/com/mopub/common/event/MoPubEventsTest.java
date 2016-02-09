package com.mopub.common.event;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubEventsTest {

    @Before
    public void setUp() {
        MoPubEvents.setEventDispatcher(null);
    }

    @Test
    public void getDispatcher_shouldReturnSingletonEventDispatcherWithScribeEventRecorder() throws Exception {
        EventDispatcher eventDispatcher = MoPubEvents.getDispatcher();
        EventDispatcher eventDispatcher2 = MoPubEvents.getDispatcher();

        assertThat(eventDispatcher).isEqualTo(eventDispatcher2);

        Iterable<EventRecorder> eventRecorderIterable = eventDispatcher.getEventRecorders();
        ArrayList<EventRecorder> eventRecorders = new ArrayList<EventRecorder>();
        for (EventRecorder recorder : eventRecorderIterable) {
            eventRecorders.add(recorder);
        }

        assertThat(eventRecorders.size()).isEqualTo(1);
        assertThat(eventRecorders.get(0)).isInstanceOf(ScribeEventRecorder.class);
    }

    @Test
    public void log_shouldDispatchEvent() throws Exception {
        EventDispatcher mockEventDispatcher = mock(EventDispatcher.class);
        MoPubEvents.setEventDispatcher(mockEventDispatcher);

        Event mockEvent = mock(Event.class);
        MoPubEvents.log(mockEvent);

        verify(mockEventDispatcher).dispatch(mockEvent);
    }
}
