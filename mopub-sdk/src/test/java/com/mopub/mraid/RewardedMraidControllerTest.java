package com.mopub.mraid;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.CloseableLayout;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.VastVideoRadialCountdownWidget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.mraid.RewardedMraidController.DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS;
import static com.mopub.mraid.RewardedMraidController.MILLIS_IN_SECOND;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedMraidControllerTest {
    private Context context;
    private long testBroadcastIdentifier;
    private RewardedMraidController subject;

    private static final int REWARDED_DURATION_IN_SECONDS = 25;
    private static final int SHOW_CLOSE_BUTTON_DELAY = REWARDED_DURATION_IN_SECONDS * MILLIS_IN_SECOND;

    @Mock
    AdReport mockAdReport;

    @Mock
    CloseableLayout mockCloseableLayout;

    @Before
    public void setUp() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        testBroadcastIdentifier = 1111;

        subject = new RewardedMraidController(context, mockAdReport, PlacementType.INTERSTITIAL,
                REWARDED_DURATION_IN_SECONDS, testBroadcastIdentifier);
    }

    @Test
    public void constructor_shouldInitializeShowCloseButtonDelay() {
        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(SHOW_CLOSE_BUTTON_DELAY);
    }

    @Test
    public void constructor_whenRewardedDurationIsNegative_shouldUseDefaultRewardedDuration() {
        subject = new RewardedMraidController(context, mockAdReport, PlacementType.INTERSTITIAL, -1,
                testBroadcastIdentifier);

        assertThat(subject.getShowCloseButtonDelay())
                .isEqualTo(DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS);
    }

    @Test
    public void constructor_whenRewardedDurationIsLongerThanDefault_shouldUseDefaultRewardedDuration() {
        subject = new RewardedMraidController(context, mockAdReport, PlacementType.INTERSTITIAL,
                DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS+1, testBroadcastIdentifier);

        assertThat(subject.getShowCloseButtonDelay())
                .isEqualTo(DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS);
    }

    @Test
    public void create_shouldSetCloseableLayoutToInvisible() {
        subject.create(context, mockCloseableLayout);

        verify(mockCloseableLayout).setCloseVisible(false);
    }

    @Test
    public void create_shouldInitializeRadialCountdownWidget() {
        subject.create(context, mockCloseableLayout);
        VastVideoRadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();

        assertThat(subject.isCalibrationDone()).isEqualTo(true);
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(radialCountdownWidget.getImageViewDrawable().getInitialCountdownMilliseconds())
                .isEqualTo(SHOW_CLOSE_BUTTON_DELAY);
    }

    @Test
    public void create_shouldInitializeCountdownRunnable() {
        subject.create(context, mockCloseableLayout);

        assertThat(subject.getCountdownRunnable()).isNotNull();
    }

    @Test
    public void pause_shouldStopRunnables() {
        subject.create(context, mockCloseableLayout);
        subject.resume();
        subject.pause();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void resume_shouldStartRunnables() {
        subject.create(context, mockCloseableLayout);
        subject.resume();

        assertThat(subject.getCountdownRunnable().isRunning()).isTrue();
    }

    @Test
    public void destroy_shouldStopRunnables() {
        subject.create(context, mockCloseableLayout);
        subject.resume();
        subject.destroy();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void showPlayableCloseButton_shouldToggleVisibilityStatesAndFireEvents() {
        subject.create(context, mockCloseableLayout);
        VastVideoRadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();

        verify(mockCloseableLayout).setCloseVisible(false);
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        assertThat(subject.isRewarded()).isFalse();

        subject.resume();
        subject.showPlayableCloseButton();

        verify(mockCloseableLayout).setCloseVisible(true);
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
        assertThat(subject.isRewarded()).isTrue();
    }
}
