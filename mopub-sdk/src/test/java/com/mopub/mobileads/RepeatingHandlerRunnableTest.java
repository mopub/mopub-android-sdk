package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RepeatingHandlerRunnableTest {

    @Mock Handler mockHandler;
    RepeatingHandlerRunnable subject;

    @Before
    public void setup() {
      subject = new NoopRepeatingHandlerRunnable(mockHandler);
    }

    @Test
    public void startTracking_whenRunning_shouldScheduleSelf() {
        subject.startRepeating(100);
        reset(mockHandler);
        subject.run();

        verify(mockHandler).postDelayed(eq(subject), eq(100l));
    }

    @Test
    public void start_whenRunning_shouldNotScheduleAndRunShouldScheduleWithNewInterval() {
        subject.startRepeating(100l);
        reset(mockHandler);

        subject.startRepeating(200l);

        verifyZeroInteractions(mockHandler);

        subject.run();

        verify(mockHandler).postDelayed(eq(subject), eq(200l));
    }

    @Test
    public void run_whenNotRunning_shouldNotSchedule() {
        subject.stop();
        reset(mockHandler);

        subject.run();

        verifyZeroInteractions(mockHandler);
    }

    @Test
    public void stopTracking_whenRunning_shouldPreventNextScheduling() {
        subject.startRepeating(100l);
        verify(mockHandler).post(eq(subject));

        subject.run();
        verify(mockHandler).postDelayed(eq(subject), eq(100l));

        reset(mockHandler);
        subject.stop();

        subject.run();
        verifyZeroInteractions(mockHandler);
    }

    private static class NoopRepeatingHandlerRunnable extends RepeatingHandlerRunnable {

        NoopRepeatingHandlerRunnable(@NonNull final Handler handler) {
            super(handler);
        }

        @Override
        public void doWork() {
            // pass
        }
    }
}
