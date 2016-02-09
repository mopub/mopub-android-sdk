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
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoCtaButtonWidgetTest {
    private Context context;
    private VastVideoCtaButtonWidget subject;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Test
    public void constructor_withCompanionAd_shouldBeInvisibleAndNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void constructor_withoutCompanionAd_shouldBeInvisibleAndNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);

        assertThat(subject.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void constructor_withCompanionAd_withNoClickthroughUrl_shouldBeGoneAndNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, true, false);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void constructor_withoutCompanionAd_withNoClickthroughUrl_shouldBeGoneAndNotSetLayoutParams() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, false, false);

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    // Video is skippable, has companion ad, has clickthrough url, CTA button initially invisible

    @Test
    public void notifyVideoSkippable_withCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // Video is skippable, no companion ad, has clickthrough url, CTA button initially invisible

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoSkippable_withoutCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // Video is complete, has companion ad, CTA button already visible

    @Test
    public void notifyVideoComplete_withCompanionAdAndInPortrait_shouldBeGoneAndNotChangeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndInLandscape_shouldBeGoneAndNotChangeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void notifyVideoComplete_withCompanionAdAndOrientationUndefined_shouldBeGoneAndNotChangeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getLayoutParams()).isNull();
    }

    @Test
    public void notifyVideoComplete_withCompanionAd_withSocialActions_shouldBeVisible() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, true, true);
        subject.setHasSocialActions(true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
    }

    // Video is complete, no companion ad, has clickthrough url, CTA button already visible

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInPortrait_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndInLandscape_shouldBeVisibleAndSetLandscapeLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasLandscapeLayoutParams()).isTrue();
    }

    @Test
    public void notifyVideoComplete_withoutCompanionAdAndOrientationUndefined_shouldBeVisibleAndSetPortraitLayoutParams() throws Exception {
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_UNDEFINED;
        subject = new VastVideoCtaButtonWidget(context, 0, false, true);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.hasPortraitLayoutParams()).isTrue();
    }

    // No clickthrough url means never show cta button

    @Test
    public void notifyVideoSkippable_withoutClickthroughUrl_shouldBeGone() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, true, false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoSkippable();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void notifyVideoComplete_withoutClickthroughUrl_shouldBeGone() throws Exception {
        subject = new VastVideoCtaButtonWidget(context, 0, true, false);
        subject.setVisibility(View.VISIBLE);

        subject.notifyVideoComplete();

        assertThat(subject.getVisibility()).isEqualTo(View.GONE);
    }
}
