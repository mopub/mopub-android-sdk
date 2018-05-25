package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mraid.RewardedMraidController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.mraid.RewardedMraidController.MILLIS_IN_SECOND;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedMraidActivityTest {
    private long broadcastIdentifier;
    private boolean shouldRewardOnClick;
    private RewardedMraidActivity subject;

    private static final String HTML_DATA = "TEST HTML DATA";
    private static final int REWARDED_DURATION_IN_SECONDS = 25;

    @Mock
    AdReport mockAdReport;

    @Mock
    RewardedMraidController mockRewardedMraidController;

    @Before
    public void setup() {
        broadcastIdentifier = 3333;
        shouldRewardOnClick = false;

        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = RewardedMraidActivity.createIntent(context, mockAdReport, HTML_DATA,
                broadcastIdentifier, REWARDED_DURATION_IN_SECONDS, shouldRewardOnClick);
        subject = Robolectric.buildActivity(RewardedMraidActivity.class, intent)
                .create().get();
    }

    @Test
    public void onCreate_shouldCreateView() throws Exception {
        View adView = subject.getCloseableLayout().getChildAt(0);
        assertThat(adView).isNotNull();
    }

    @Test
    public void onCreate_shouldCallOnCreateForController() throws Exception {
        // Close button should not be visible
        assertThat(subject.getCloseableLayout().isCloseVisible()).isFalse();

        RewardedMraidController controller = subject.getRewardedMraidController();
        VastVideoRadialCountdownWidget countdownWidget = controller.getRadialCountdownWidget();

        // Radial countdown widget should be calibrated
        assertThat(countdownWidget).isNotNull();
        assertThat(countdownWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(countdownWidget.getImageViewDrawable().getInitialCountdownMilliseconds())
                .isEqualTo(REWARDED_DURATION_IN_SECONDS * MILLIS_IN_SECOND);
        assertThat(controller.isCalibrationDone()).isTrue();

        // Countdown runnable should be created
        assertThat(controller.getCountdownRunnable()).isNotNull();
    }

    @Test
    public void onPause_shouldCallPauseOnController() {
        subject.onResume();
        subject.onPause();

        // Countdown runnable should be stopped
        assertThat(subject.getRewardedMraidController().getCountdownRunnable().isRunning())
                .isFalse();
    }

    @Test
    public void onResume_shouldCallResumeOnController() {
        subject.onPause();
        subject.onResume();

        // Countdown runnable should be resumed
        assertThat(subject.getRewardedMraidController().getCountdownRunnable().isRunning())
                .isTrue();
    }

    @Test
    public void onDestroy_shouldCallDestroyOnController() {
        subject.onResume();
        subject.onDestroy();

        // Countdown runnable should be stopped
        assertThat(subject.getRewardedMraidController().getCountdownRunnable().isRunning())
                .isFalse();
    }
}
