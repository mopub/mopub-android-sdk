package com.mopub.common;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class DoubleTimeTrackerTest {

    @Mock DoubleTimeTracker.Clock mockClock;
    DoubleTimeTracker subject;

    @Before
    public void setUp() throws Exception {
        subject = new DoubleTimeTracker(mockClock);

        when(mockClock.elapsedRealTime()).thenReturn(100L, 200L, 300L, 400L, 500L, 600L);
    }

    @Test
    public void whenStartThenGetInterval_shouldReturn100() throws Exception {
        subject.start();
        assertThat(subject.getInterval()).isEqualTo(100d);
    }

    @Test
    public void whenPauseBeforeStart_shouldReturn0() throws Exception {
        subject.pause();
        assertThat(subject.getInterval()).isEqualTo(0d);
    }

    @Test
    public void whenStartPauseStart_thenGetInterval_shouldReturn200() throws Exception {
        subject.start();
        subject.pause();
        subject.start();

        assertThat(subject.getInterval()).isEqualTo(200d);
    }

    @Test
    public void whenStartPauseStartPause_thenGetInterval_shouldReturn200() throws Exception {
        subject.start();
        subject.pause();
        subject.start();
        subject.pause();

        assertThat(subject.getInterval()).isEqualTo(200d);
    }

    @Test
    public void whenMultipleStart_shouldNotAffectInterval_shouldReturn100() throws Exception {
        subject.start();
        subject.start();
        subject.start();

        assertThat(subject.getInterval()).isEqualTo(100d);
    }
}
