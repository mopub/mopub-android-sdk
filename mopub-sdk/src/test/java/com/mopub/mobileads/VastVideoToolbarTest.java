package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.mopub.mobileads.resource.TextDrawable;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class VastVideoToolbarTest {
    private Context context;
    private VastVideoToolbar subject;
    private View.OnTouchListener onTouchListener;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        subject = new VastVideoToolbar(context);

        onTouchListener = mock(View.OnTouchListener.class);
    }

    @Test
    public void constructor_shouldSetOnTouchListenerThatConsumesAllTouchEvents() throws Exception {
        final View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        assertThat(onTouchListener).isNotNull();
        assertThat(onTouchListener.onTouch(null, null)).isTrue();
    }

    @Test
    public void constructor_shouldAddWidgetsToToolbar() throws Exception {
        assertThat(subject.getChildCount()).isEqualTo(4);

        assertThat(subject.getDurationWidget().getParent()).isEqualTo(subject);
        assertThat(subject.getLearnMoreWidget().getParent()).isEqualTo(subject);
        assertThat(subject.getCountdownWidget().getParent()).isEqualTo(subject);
        assertThat(subject.getCloseButtonWidget().getParent()).isEqualTo(subject);
    }
    
    @Test
    public void constructor_shouldOnlyStartWithDurationWidgetsVisible() throws Exception {
        assertThat(subject.getDurationWidget().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getLearnMoreWidget().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getCountdownWidget().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void makeInteractable_shouldHideCountdownWidgetAndShowLearnMoreAndCloseButtonWidgets() throws Exception {
        subject.makeInteractable();

        assertThat(subject.getDurationWidget().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getLearnMoreWidget().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getCountdownWidget().getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void setCloseButtonOnTouchListener_shouldPropagateToCloseButtonWidget() throws Exception {
        subject.setCloseButtonOnTouchListener(onTouchListener);

        assertThat(shadowOf(subject.getCloseButtonWidget()).getOnTouchListener()).isEqualTo(onTouchListener);
    }

    @Test
    public void setLearnButtonOnTouchListener_shouldPropagateToLearnButtonWidget() throws Exception {
        subject.setLearnMoreButtonOnTouchListener(onTouchListener);

        assertThat(shadowOf(subject.getLearnMoreWidget()).getOnTouchListener()).isEqualTo(onTouchListener);
    }

    @Test
    public void getDisplaySeconds_shouldReturnLongMillisecondsAsRoundedUpStringSeconds() throws Exception {
        assertThat(subject.getDisplaySeconds(0)).isEqualTo("0");

        assertThat(subject.getDisplaySeconds(1)).isEqualTo("1");
        assertThat(subject.getDisplaySeconds(999)).isEqualTo("1");
        assertThat(subject.getDisplaySeconds(1000)).isEqualTo("1");

        assertThat(subject.getDisplaySeconds(1001)).isEqualTo("2");
        assertThat(subject.getDisplaySeconds(100000)).isEqualTo("100");
    }
    
    @Test
    public void updateCountdownWidget_shouldUpdateTextDrawablesDisplayNumber() throws Exception {
        final TextDrawable countdownImageSpy = spy(subject.getCountdownWidget().getImageViewDrawable());
        subject.getCountdownWidget().setImageViewDrawable(countdownImageSpy);

        subject.updateCountdownWidget(1002);

        verify(countdownImageSpy).updateText("2");
    }

    @Test
    public void updateCountdownWidget_shouldHideCloseButtonAndShowCountdown() throws Exception {
        subject.getCloseButtonWidget().setVisibility(View.INVISIBLE);
        subject.getCountdownWidget().setVisibility(View.INVISIBLE);

        subject.updateCountdownWidget(1);

        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getCountdownWidget().getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void updateCountdownWidget_whenRemainingTimeIsNegative_shouldNotModifyWidgetVisibility() throws Exception {
        subject.getCloseButtonWidget().setVisibility(View.INVISIBLE);
        subject.getCountdownWidget().setVisibility(View.INVISIBLE);

        subject.updateCountdownWidget(-1);

        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getCountdownWidget().getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void updateDurationWidget_shouldShowWhenVideoEnds() throws Exception {
        final TextView durationWidgetTextView = getDurationWidgetTextView();

        assertThat(durationWidgetTextView.getText()).isEqualTo("");

        subject.updateDurationWidget(100000);

        assertThat(durationWidgetTextView.getText()).isEqualTo("Ends in 100 seconds");

        subject.updateDurationWidget(99000);

        assertThat(durationWidgetTextView.getText()).isEqualTo("Ends in 99 seconds");
    }

    @Test
    public void updateDurationWidget_whenBelowThresholdForHidingVideoDuration_shouldShowThanksForWatching() throws Exception {
        final TextView durationWidgetTextView = getDurationWidgetTextView();

        assertThat(durationWidgetTextView.getText()).isEqualTo("");

        subject.updateDurationWidget(50000);

        assertThat(durationWidgetTextView.getText()).isEqualTo("Ends in 50 seconds");

        // 200ms of remaining video is the cut off for switching to "Thanks for watching"
        subject.updateDurationWidget(200);

        assertThat(durationWidgetTextView.getText()).isEqualTo("Ends in 1 seconds");

        subject.updateDurationWidget(199);

        assertThat(durationWidgetTextView.getText()).isEqualTo("Thanks for watching");
    }

    @Test
    public void updateDurationWidget_whenRemainingTimeIsNegative_shouldDoNothing() throws Exception {
        final TextView durationWidgetTextView = getDurationWidgetTextView();

        assertThat(durationWidgetTextView.getText()).isEqualTo("");

        subject.updateDurationWidget(-1);

        assertThat(durationWidgetTextView.getText()).isEqualTo("");
    }

    private TextView getDurationWidgetTextView() {
        final ToolbarWidget durationWidget = subject.getDurationWidget();
        final int childCount = durationWidget.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = durationWidget.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            }
        }
        return null;
    }
}
