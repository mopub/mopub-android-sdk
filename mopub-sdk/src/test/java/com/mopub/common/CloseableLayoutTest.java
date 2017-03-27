package com.mopub.common;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.mopub.common.CloseableLayout.ClosePosition;
import com.mopub.common.CloseableLayout.OnCloseListener;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CloseableLayoutTest {
    private CloseableLayout subject;

    @Mock private OnCloseListener mockCloseListener;

    private MotionEvent closeRegionDown;
    private MotionEvent closeRegionUp;
    private MotionEvent closeRegionCancel;
    private MotionEvent contentRegionDown;
    private MotionEvent contentRegionUp;
    private MotionEvent contentRegionCancel;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new CloseableLayout(activity);
        subject.setClosePosition(ClosePosition.TOP_RIGHT);

        // Fake the close bounds, which allows us to set up close regions
        subject.setCloseBounds(new Rect(100, 10, 110, 20));
        closeRegionDown = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_DOWN, 100, 10, 0);
        closeRegionUp = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_UP, 100, 10, 0);
        closeRegionCancel = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_CANCEL, 100, 10, 0);
        contentRegionDown = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_DOWN, 0, 0, 0);
        contentRegionUp = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_UP, 0, 0, 0);
        contentRegionCancel = MotionEvent.obtain(
                100, 200, MotionEvent.ACTION_CANCEL, 0, 0, 0);
    }

    @Test
    public void setOnCloseListener_thenTouchCloseRegion_shouldCallOnClick() {
        subject.setOnCloseListener(mockCloseListener);
        subject.onTouchEvent(closeRegionDown);
        subject.onTouchEvent(closeRegionUp);

        verify(mockCloseListener).onClose();
    }

    @Test
    public void setOnCloseListener_thenTouchContentRegion_shouldNotCallCloseListener() {
        subject.setOnCloseListener(mockCloseListener);
        subject.onTouchEvent(contentRegionDown);
        subject.onTouchEvent(contentRegionUp);

        verify(mockCloseListener, never()).onClose();
    }

    @Test
    public void setCloseVisible_shouldToggleCloseDrawable() {
        subject.setCloseVisible(false);
        assertThat(subject.isCloseVisible()).isFalse();

        subject.setCloseVisible(true);
        assertThat(subject.isCloseVisible()).isTrue();
    }

    @Test
    public void draw_shouldUpdateCloseBounds() {
        subject.setLeft(0);
        subject.setTop(0);
        subject.setRight(100);
        subject.setBottom(200);
        subject.onSizeChanged(100, 200, 0, 0);

        int expectedTop = 0;
        int expectedLeft = (int) (100 - CloseableLayout.CLOSE_REGION_SIZE_DP);
        Canvas canvas = new Canvas();
        subject.draw(canvas);
        Rect closeBounds = subject.getCloseBounds();
        assertThat(closeBounds.top).isEqualTo(expectedTop);
        assertThat(closeBounds.bottom).isEqualTo(
                (int) (expectedTop + CloseableLayout.CLOSE_REGION_SIZE_DP));
        assertThat(closeBounds.left).isEqualTo(expectedLeft);
        assertThat(closeBounds.right).isEqualTo(
                (int) (expectedLeft + CloseableLayout.CLOSE_REGION_SIZE_DP));
    }

    @Test
    public void draw_withoutCloseBoundsChanged_shouldNotUpdateCloseBounds() {
        Canvas canvas = new Canvas();
        subject.draw(canvas);
        Rect originalCloseBounds = subject.getCloseBounds();

        subject.setCloseBounds(new Rect(40, 41, 42, 43));
        subject.setCloseBoundChanged(false);
        subject.draw(canvas);

        assertThat(subject.getCloseBounds()).isEqualTo(originalCloseBounds);
    }

    @Test
    public void onInterceptTouchEvent_closeRegionDown_shouldReturnTrue() {
        boolean intercepted = subject.onInterceptTouchEvent(closeRegionDown);
        assertThat(intercepted).isTrue();
    }

    @Test public void onInterceptTouchEvent_contentRegionDown_returnsTrue() {
        boolean intercepted = subject.onInterceptTouchEvent(contentRegionDown);
        assertThat(intercepted).isFalse();
    }

    @Test
    public void
    onTouchEvent_closeRegionDown_thenCloseRegionUp_shouldTogglePressedStateAfterDelay() {
        assertThat(subject.isClosePressed()).isFalse();

        subject.onTouchEvent(closeRegionDown);
        assertThat(subject.isClosePressed()).isTrue();

        subject.onTouchEvent(closeRegionUp);
        assertThat(subject.isClosePressed()).isTrue();

        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();
        assertThat(subject.isClosePressed()).isFalse();
    }

    @Test
    public void onTouchEvent_closeRegionDown_thenCloseRegionCancel_shouldTogglePressedState() {
        subject.onTouchEvent(closeRegionDown);
        subject.onTouchEvent(closeRegionCancel);
        assertThat(subject.isClosePressed()).isFalse();
    }

    @Test
    public void onTouchEvent_closeRegionDown_thenContentRegionCancel_shouldTogglePressedState() {
        subject.onTouchEvent(closeRegionDown);
        subject.onTouchEvent(contentRegionCancel);
        assertThat(subject.isClosePressed()).isFalse();
    }

    @Test
    public void onTouchEvent_closeRegionDown_withCloseNotVisible_withSetCloseAlwaysInteractableFalse_shouldTogglePressedState() {
        subject.setCloseAlwaysInteractable(false);
        subject.setCloseVisible(false);
        subject.onTouchEvent(closeRegionDown);
        assertThat(subject.isClosePressed()).isFalse();
    }

    @Test
    public void onTouchEvent_closeRegionDown_withCloseNotVisible_withSetCloseAlwaysInteractableDefault_shouldTogglePressedState() {
        // The default of mCloseAlwaysInteractable is true
        subject.setCloseVisible(false);
        subject.onTouchEvent(closeRegionDown);
        assertThat(subject.isClosePressed()).isTrue();
    }

    @Test
    public void pointInCloseBounds_noSlop_shouldReturnValidValues() {
        Rect bounds = new Rect();
        bounds.left = 10;
        bounds.right = 20;
        bounds.top = 100;
        bounds.bottom = 200;
        subject.setCloseBounds(bounds);

        assertThat(subject.pointInCloseBounds(9, 99, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 100, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 199, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 200, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(10, 99, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(10, 100, 0)).isTrue();
        assertThat(subject.pointInCloseBounds(10, 199, 0)).isTrue();
        assertThat(subject.pointInCloseBounds(10, 200, 0)).isFalse();

        assertThat(subject.pointInCloseBounds(19, 99, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(19, 100, 0)).isTrue();
        assertThat(subject.pointInCloseBounds(19, 199, 0)).isTrue();
        assertThat(subject.pointInCloseBounds(19, 200, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 99, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 100, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 199, 0)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 200, 0)).isFalse();

    }

    @Test
    public void pointInCloseBounds_slop_shouldReturnValidValues() {
        int slop = 3;

        // Same as above, but adjust given 3 px slop
        Rect bounds = new Rect();
        bounds.left = 13;
        bounds.right = 17;
        bounds.top = 103;
        bounds.bottom = 197;
        subject.setCloseBounds(bounds);

        assertThat(subject.pointInCloseBounds(9, 99, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 100, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 199, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(9, 200, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(10, 99, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(10, 100, slop)).isTrue();
        assertThat(subject.pointInCloseBounds(10, 199, slop)).isTrue();
        assertThat(subject.pointInCloseBounds(10, 200, slop)).isFalse();

        assertThat(subject.pointInCloseBounds(19, 99, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(19, 100, slop)).isTrue();
        assertThat(subject.pointInCloseBounds(19, 199, slop)).isTrue();
        assertThat(subject.pointInCloseBounds(19, 200, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 99, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 100, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 199, slop)).isFalse();
        assertThat(subject.pointInCloseBounds(20, 200, slop)).isFalse();
    }

    @Test
    public void shouldAllowPress_shouldRespectSetCloseAlwaysInteractable() {
        subject.setCloseVisible(false);
        subject.setCloseAlwaysInteractable(false);
        assertThat(subject.shouldAllowPress()).isFalse();

        subject.setCloseVisible(false);
        subject.setCloseAlwaysInteractable(true);
        assertThat(subject.shouldAllowPress()).isTrue();

        subject.setCloseVisible(true);
        subject.setCloseAlwaysInteractable(false);
        assertThat(subject.shouldAllowPress()).isTrue();

        subject.setCloseVisible(true);
        subject.setCloseAlwaysInteractable(true);
        assertThat(subject.shouldAllowPress()).isTrue();
    }
}
