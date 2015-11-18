package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.resource.RadialCountdownDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoRadialCountdownWidgetTest {
    private Context context;
    private VastVideoRadialCountdownWidget subject;
    private RadialCountdownDrawable radialCountdownDrawableSpy;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastVideoRadialCountdownWidget(context);
        radialCountdownDrawableSpy = spy(subject.getImageViewDrawable());
        subject.setImageViewDrawable(radialCountdownDrawableSpy);
    }

    @Test
    public void calibrateAndMakeVisible_shouldSetInitialCountdownAndMakeVisible() throws Exception {
        subject.setVisibility(View.INVISIBLE);

        subject.calibrateAndMakeVisible(10000);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy).setInitialCountdown(10000);
        assertThat(radialCountdownDrawableSpy.getInitialCountdownMilliseconds()).isEqualTo(10000);
    }

    @Test
    public void updateCountdownProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        subject.updateCountdownProgress(10000, 1000);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy).updateCountdownProgress(1000);
    }

    @Test
    public void updateCountdownProgress_whenProgressIsGreaterThanInitialCountdown_shouldHideAndNotUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        subject.updateCountdownProgress(10000, 10001);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        verify(radialCountdownDrawableSpy, never()).updateCountdownProgress(anyInt());
    }

    @Test
    public void updateCountdownProgress_whenCurrentProgressGreaterThanPreviousProgress_shouldUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        // Set mLastProgressMilliseconds to 1000
        subject.updateCountdownProgress(10000, 1000);
        reset(radialCountdownDrawableSpy);

        subject.updateCountdownProgress(10000, 1001);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy).updateCountdownProgress(1001);
    }

    @Test
    public void updateCountdownProgress_whenCurrentProgressLessThanPreviousProgress_shouldNotChangeVisibilityOrUpdateDrawable() throws Exception {
        subject.setVisibility(View.VISIBLE);

        // Set mLastProgressMilliseconds to 1000
        subject.updateCountdownProgress(10000, 1000);
        reset(radialCountdownDrawableSpy);

        subject.updateCountdownProgress(10000, 999);

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownDrawableSpy, never()).updateCountdownProgress(anyInt());
    }
}
