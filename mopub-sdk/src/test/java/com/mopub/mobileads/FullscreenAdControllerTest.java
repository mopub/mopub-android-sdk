// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static com.mopub.mobileads.AdData.DEFAULT_DURATION_FOR_CLOSE_BUTTON_MILLIS;
import static com.mopub.mobileads.AdData.MILLIS_IN_SECOND;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@RunWith(SdkTestRunner.class)
public class FullscreenAdControllerTest {
    private static final String EXPECTED_HTML_DATA = "htmlData";
    private static final int REWARDED_DURATION_IN_SECONDS = 25;
    private static final int SHOW_CLOSE_BUTTON_DELAY = REWARDED_DURATION_IN_SECONDS * MILLIS_IN_SECOND;

    private Activity activity;
    private long broadcastIdentifier;
    private AdData adData;
    private FullscreenAdController subject;
    private VastVideoConfig vastVideoConfig;

    @Mock
    Intent mockIntent;
    @Mock
    Bundle mockBundle;

    @Before
    public void setUp() throws Exception {

        activity = spy(Robolectric.buildActivity(Activity.class).create().get());

        vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        broadcastIdentifier = 112233;
        adData = new AdData.Builder()
                .adPayload(EXPECTED_HTML_DATA)
                .broadcastIdentifier(broadcastIdentifier)
                .rewardedDurationSeconds(REWARDED_DURATION_IN_SECONDS)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

    }

    @Test
    public void constructor_withRewardedTrue_shouldInitializeShowCloseButtonDelay() {
        adData.setRewarded(true);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getShowCloseButtonDelayMillis()).isEqualTo(SHOW_CLOSE_BUTTON_DELAY);
    }

    @Test
    public void constructor_whenRewardedDurationIsNegative_shouldUseDefaultRewardedDuration() {
        adData.setRewarded(true);
        adData.setRewardedDurationSeconds(-3);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getShowCloseButtonDelayMillis())
                .isEqualTo(DEFAULT_DURATION_FOR_CLOSE_BUTTON_MILLIS);
    }

    @Test
    public void constructor_whenRewardedDurationIsLongerThanDefault_shouldUseRewardedDuration() {
        adData.setRewarded(true);
        adData.setRewardedDurationSeconds(90);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getShowCloseButtonDelayMillis()).isEqualTo(90 * 1000);
    }

    @Test
    public void constructor_shouldSetCloseableLayoutToVisible() {
        assertThat(subject.getCloseableLayout().isCloseVisible()).isTrue();
    }

    @Test
    public void constructor_withRewardedTrue_shouldSetCloseableLayoutToInvisible_shouldSetBackButtonEnabledFalse() {
        adData.setRewarded(true);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getCloseableLayout().isCloseVisible()).isFalse();
        assertThat(subject.backButtonEnabled()).isFalse();
    }

    @Test
    public void constructor_withRewardedFalse_shouldSetCloseableLayoutToVisible_shouldSetBackButtonEnabledTrue() {
        adData.setRewarded(false);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        assertThat(subject.getCloseableLayout().isCloseVisible()).isTrue();
        assertThat(subject.backButtonEnabled()).isTrue();
    }

    @Test
    public void constructor_shouldInitializeRadialCountdownWidget_shouldInitializeCountdownRunnable() {
        adData.setRewarded(true);

        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertThat(subject.isCalibrationDone()).isEqualTo(true);
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(radialCountdownWidget.getImageViewDrawable().getInitialCountdownMilliseconds())
                .isEqualTo(SHOW_CLOSE_BUTTON_DELAY);
        assertThat(subject.getCountdownRunnable()).isNotNull();
    }

    @Test
    public void pause_shouldStopRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);
        subject.resume();

        subject.pause();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void resume_shouldStartRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        subject.resume();

        assertThat(subject.getCountdownRunnable().isRunning()).isTrue();
    }

    @Test
    public void destroy_shouldStopRunnables() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);
        subject.resume();

        subject.destroy();

        assertThat(subject.getCountdownRunnable().isRunning()).isFalse();
    }

    @Test
    public void showCloseButton_shouldToggleVisibilityStatesAndFireEvents() {
        adData.setRewarded(true);
        subject = new FullscreenAdController(activity, mockBundle, mockIntent, adData);

        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();

        assertThat(subject.getCloseableLayout().isCloseVisible()).isFalse();
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        assertThat(subject.isRewarded()).isFalse();
        subject.resume();

        subject.showCloseButton();

        assertThat(subject.getCloseableLayout().isCloseVisible()).isTrue();
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
        assertThat(subject.isRewarded()).isTrue();
    }
}
