package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class VastVideoCtaButtonWidgetTest {
    private Context context;
    private VastVideoCtaButtonWidget subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void constructor_withCompanionAd_shouldNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, true);

        assertThat(subject.getLayoutParams()).isNull();
        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void constructor_withoutCompanionAd_shouldNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, false);

        assertThat(subject.getLayoutParams()).isNull();
        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    // Video is skippable, has companion ad, CTA button initially invisible

    @Test
    public void notifyVideoSkippable_withCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // Video is skippable, no companion ad, CTA button initially invisible

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.INVISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // Video is complete, has companion ad, CTA button already visible

    @Test
    public void notifyVideoComplete_withCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndInLandscape_shouldBeGoneAndNotChangeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // Video is complete, no companion ad, CTA button already visible

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }
}
