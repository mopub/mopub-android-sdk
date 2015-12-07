package com.mopub.common.event;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class EventTest {

    private Event subject;

    @Before
    public void setUp() {
        subject = new Event.Builder(BaseEvent.Name.AD_REQUEST, BaseEvent.Category.REQUESTS, 0.10000123).build();
    }

    @Test
    public void constructor_shouldCorrectlyAssignScribeCategoryFromBuilder() {
        assertThat(subject.getName()).isEqualTo(BaseEvent.Name.AD_REQUEST);
        assertThat(subject.getCategory()).isEqualTo(BaseEvent.Category.REQUESTS);
        assertThat(subject.getSamplingRate()).isEqualTo(0.10000123);
        assertThat(subject.getScribeCategory()).isEqualTo(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_EVENT);
    }
}
