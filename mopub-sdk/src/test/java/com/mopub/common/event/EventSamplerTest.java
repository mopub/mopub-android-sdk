package com.mopub.common.event;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.annotation.Config;

import java.util.Random;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class EventSamplerTest {

    private EventSampler subject;
    @Mock Random mockRandom;
    @Mock BaseEvent mockBaseEvent;

    @Before
    public void setUp() {
        subject = new EventSampler(mockRandom);
        when(mockBaseEvent.getSamplingRate()).thenReturn(0.10);
    }

    @Test
    public void sample_withRandomNumberLessThan10Percent_shouldReturnTrue() throws Exception {
        when(mockRandom.nextDouble()).thenReturn(0.09);

        boolean result = subject.sample(mockBaseEvent);

        assertThat(result).isTrue();
    }

    @Test
    public void sample_withRandomNumberGreaterOrEqualTo10Percent_shouldReturnFalse() throws Exception {
        when(mockRandom.nextDouble()).thenReturn(0.10);

        boolean result = subject.sample(mockBaseEvent);

        assertThat(result).isFalse();
    }

    @Test
    public void sample_withSameRequestId_shouldReturnSameValueRegardlessOfSampleRate() throws Exception {
        when(mockRandom.nextDouble()).thenReturn(0.09).thenReturn(0.999);
        when(mockBaseEvent.getRequestId()).thenReturn("rid");

        boolean firstResult = subject.sample(mockBaseEvent);
        assertThat(firstResult).isTrue();

        boolean secondResult = subject.sample(mockBaseEvent);
        assertThat(secondResult).isTrue();
    }

    @Test
    public void sample_withDifferentRequestId_shouldReturnResultBasedOnSampleRate() throws Exception {
        when(mockRandom.nextDouble()).thenReturn(0.09).thenReturn(0.999);
        when(mockBaseEvent.getRequestId()).thenReturn("rid1").thenReturn("rid2");

        boolean firstResult = subject.sample(mockBaseEvent);
        assertThat(firstResult).isTrue();

        boolean secondResult = subject.sample(mockBaseEvent);
        assertThat(secondResult).isFalse();
    }

    @Test
    public void sample_withTooManyEvents_shouldHoldAMaximumNumberOfRequestIds() {
        when(mockRandom.nextDouble()).thenReturn(0.001);
        OngoingStubbing<String> ongoingStubbing = when(mockBaseEvent.getRequestId()).thenReturn(
                "rid0");
        for (int i = 1; i < EventSampler.MAX_SIZE * 3; i++) {
            ongoingStubbing = ongoingStubbing.thenReturn("rid" + i);
        }

        for (int i = 0; i < EventSampler.MAX_SIZE * 3; i++) {
            subject.sample(mockBaseEvent);
        }

        assertThat(subject.getCacheSize()).isEqualTo(EventSampler.MAX_SIZE);
    }
}
