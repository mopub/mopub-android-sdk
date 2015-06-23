package com.mopub.common.event;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Random;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
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
}
