package com.mopub.mobileads;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowGestureDetector;

import static com.mopub.mobileads.ViewGestureDetector.UserClickListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ViewGestureDetectorTest {
    private Activity context;
    private ViewGestureDetector subject;
    private AdAlertGestureListener adAlertGestureListener;
    private View view;
    @Mock AdReport mockAdReport;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        view = mock(View.class);
        stub(view.getWidth()).toReturn(320);
        stub(view.getHeight()).toReturn(50);

        adAlertGestureListener = mock(AdAlertGestureListener.class);

        subject = new ViewGestureDetector(context, view, mockAdReport);
        subject.setAdAlertGestureListener(adAlertGestureListener);
    }

    @Test
    public void constructor_shouldDisableLongPressAndSetGestureListener() throws Exception {
        subject = new ViewGestureDetector(context, view, mockAdReport);

        ShadowGestureDetector shadowGestureDetector = Shadows.shadowOf(subject);

        assertThat(subject.isLongpressEnabled()).isFalse();
        assertThat(shadowGestureDetector.getListener()).isNotNull();
        assertThat(shadowGestureDetector.getListener()).isInstanceOf(AdAlertGestureListener.class);
    }

    @Test
    public void onTouchEvent_whenActionUpAndClickListener_shouldNotifyClickListenerAndCheckReportAd() throws Exception {
        MotionEvent expectedMotionEvent = createMotionEvent(MotionEvent.ACTION_UP);

        UserClickListener userClickListener = mock(UserClickListener.class);
        subject.setUserClickListener(userClickListener);

        subject.sendTouchEvent(expectedMotionEvent);

        verify(userClickListener).onUserClick();
        verify(adAlertGestureListener).finishGestureDetection();
    }

    @Test
    public void onTouchEvent_whenActionUpButNoClickListener_shouldNotNotifyClickListenerAndCheckReportAd() throws Exception {
        MotionEvent expectedMotionEvent = createMotionEvent(MotionEvent.ACTION_UP);

        UserClickListener userClickListener = mock(UserClickListener.class);

        subject.sendTouchEvent(expectedMotionEvent);

        verify(userClickListener, never()).onUserClick();
        verify(adAlertGestureListener).finishGestureDetection();
    }

    @Test
    public void onTouchEvent_whenActionDown_shouldForwardOnTouchEvent() throws Exception {
        MotionEvent expectedMotionEvent = createMotionEvent(MotionEvent.ACTION_DOWN);

        subject.sendTouchEvent(expectedMotionEvent);

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isEqualTo(expectedMotionEvent);
    }

    @Test
    public void onTouchEvent_whenActionMoveWithinView_shouldForwardOnTouchEvent() throws Exception {
        MotionEvent downEvent = createMotionEvent(MotionEvent.ACTION_DOWN);
        subject.sendTouchEvent(downEvent);

        MotionEvent expectedMotionEvent = createActionMove(160);
        subject.sendTouchEvent(expectedMotionEvent);

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isEqualTo(expectedMotionEvent);
        verify(adAlertGestureListener, never()).reset();
    }

    @Test
    public void sendTouchEvent_whenReceiveTouchEventOutsideOfViewInXDirection_shouldResetAlertState() throws Exception {
        subject.sendTouchEvent(createActionMove(350));

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isNull();
        verify(adAlertGestureListener).reset();
    }

    @Test
    public void sendTouchEvent_whenReceiveTouchEventOutsideOfViewInYDirection_shouldResetAlertState() throws Exception {
        MotionEvent verticalMotion = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 160, 200, 0);
        subject.sendTouchEvent(verticalMotion);

        MotionEvent actualMotionEvent = Shadows.shadowOf(subject).getOnTouchEventMotionEvent();

        assertThat(actualMotionEvent).isNull();
        verify(adAlertGestureListener).reset();
    }

    @Test
    public void resetAdFlaggingGesture_shouldNotifyAdAlertGestureListenerOfReset() throws Exception {
        subject.resetAdFlaggingGesture();

        verify(adAlertGestureListener).reset();
    }

    private MotionEvent createActionMove(float x) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, x, 0, 0);
    }

    private MotionEvent createMotionEvent(int action) {
        return MotionEvent.obtain(0, 0, action, 0, 0, 0);
    }
}
