package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.resource.ProgressBarDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoProgressBarWidgetTest {
    private Context context;
    private VastVideoProgressBarWidget subject;
    private ProgressBarDrawable progressBarDrawableSpy;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastVideoProgressBarWidget(context);
        subject.setAnchorId(0);
        progressBarDrawableSpy = spy(subject.getImageViewDrawable());
        subject.setImageViewDrawable(progressBarDrawableSpy);
    }

    @Test
    public void calibrateAndMakeVisible_shouldSetDurationAndSkipOffsetAndMakeVisible() throws Exception {
        subject.setVisibility(View.INVISIBLE);

        subject.calibrateAndMakeVisible(10000, 5000);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarDrawableSpy).setDurationAndSkipOffset(10000, 5000);
        assertThat(progressBarDrawableSpy.getSkipRatio()).isEqualTo(0.5f);
    }

    @Test
    public void updateProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        subject.updateProgress(1000);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarDrawableSpy).setProgress(1000);
        assertThat(progressBarDrawableSpy.getCurrentProgress()).isEqualTo(1000);
    }

    @Test
    public void updateProgress_whenCurrentProgressGreaterThanPreviousProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        // Set mLastProgress to 1000
        subject.updateProgress(1000);
        reset(progressBarDrawableSpy);

        subject.updateProgress(1001);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarDrawableSpy).setProgress(1001);
        verify(progressBarDrawableSpy, never()).forceCompletion();
        assertThat(progressBarDrawableSpy.getCurrentProgress()).isEqualTo(1001);
    }

    @Test
    public void updateProgress_whenCurrentProgressLessThanPreviousProgressButNotZero_shouldForceProgressCompletionAndNotChangeVisibility() throws Exception {
        subject.setVisibility(View.VISIBLE);
        subject.calibrateAndMakeVisible(10000, 5000);

        // Set mLastProgress to 1000
        subject.updateProgress(1000);
        reset(progressBarDrawableSpy);

        subject.updateProgress(999);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarDrawableSpy).setProgress(999);
        verify(progressBarDrawableSpy).forceCompletion();
        assertThat(progressBarDrawableSpy.getCurrentProgress()).isEqualTo(10000);
    }

    @Test
    public void updateProgress_whenCurrentProgressLessThanPreviousProgressAndIsZero_shouldNotForceProgressCompletionAndNotChangeVisibility() throws Exception {
        subject.setVisibility(View.VISIBLE);
        subject.calibrateAndMakeVisible(10000, 5000);

        // Set mLastProgress to 1000
        subject.updateProgress(1000);
        reset(progressBarDrawableSpy);

        subject.updateProgress(0);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarDrawableSpy).setProgress(0);
        verify(progressBarDrawableSpy, never()).forceCompletion();
        assertThat(progressBarDrawableSpy.getCurrentProgress()).isEqualTo(1000);
    }
}
