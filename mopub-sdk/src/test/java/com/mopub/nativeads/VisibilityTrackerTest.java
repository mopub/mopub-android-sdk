package com.mopub.nativeads;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.VisibilityTracker.TrackingInfo;

import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static android.view.ViewTreeObserver.OnPreDrawListener;
import static com.mopub.nativeads.VisibilityTracker.VisibilityChecker;
import static com.mopub.nativeads.VisibilityTracker.VisibilityTrackerListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VisibilityTrackerTest {
    private static final int MIN_PERCENTAGE_VIEWED = 50;
    private static final Integer DEFAULT_MIN_VISIBLE_PX = 1;

    private Activity activity;
    private VisibilityTracker subject;
    private Map<View, TrackingInfo> trackedViews;
    private VisibilityChecker visibilityChecker;

    @Mock private VisibilityTrackerListener visibilityTrackerListener;
    @Mock private View view;
    @Mock private View view2;
    @Mock private Handler visibilityHandler;

    @Before
    public void setUp() throws Exception {
        trackedViews = new WeakHashMap<View, TrackingInfo>();
        visibilityChecker = new VisibilityChecker();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        view = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);
        view2 = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);

        // Add a proxy listener to that makes a safe copy of the listener args.
        VisibilityTrackerListener proxyListener = new VisibilityTrackerListener() {
            @Override
            public void onVisibilityChanged(List<View> visibleViews, List<View> invisibleViews) {
                ArrayList<View> safeVisibleViews = new ArrayList<View>(visibleViews);
                ArrayList<View> safeInVisibleViews = new ArrayList<View>(invisibleViews);
                visibilityTrackerListener.onVisibilityChanged(safeVisibleViews, safeInVisibleViews);
            }
        };
        subject = new VisibilityTracker(activity, trackedViews, visibilityChecker, visibilityHandler);
        subject.setVisibilityTrackerListener(proxyListener);

        // XXX We need this to ensure that our SystemClock starts
        ShadowSystemClock.uptimeMillis();
    }

    @Test
    public void constructor_shouldSetOnPreDrawListenerForDecorView() throws Exception {
        Activity activity1 = spy(Robolectric.buildActivity(Activity.class).create().get());
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(activity1.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.findViewById(anyInt())).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new VisibilityTracker(activity1, trackedViews, visibilityChecker, visibilityHandler);
        assertThat(subject.mOnPreDrawListener).isNotNull();
        verify(viewTreeObserver).addOnPreDrawListener(subject.mOnPreDrawListener);
        assertThat(subject.mWeakViewTreeObserver.get()).isEqualTo(viewTreeObserver);
    }

    @Test
    public void constructor_withNonAliveViewTreeObserver_shouldNotSetOnPreDrawListenerForDecorView() throws Exception {
        Activity activity1 = mock(Activity.class);
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(activity1.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(false);

        subject = new VisibilityTracker(activity1, trackedViews, visibilityChecker, visibilityHandler);
        verify(viewTreeObserver, never()).addOnPreDrawListener(subject.mOnPreDrawListener);
        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
    }

    @Test
    public void constructor_withApplicationContext_shouldNotSetOnPreDrawListener() {
        subject = new VisibilityTracker(activity.getApplicationContext(), trackedViews,
                visibilityChecker, visibilityHandler);

        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
    }

    @Test
    public void addView_withVisibleView_shouldAddVisibleViewToTrackedViews() throws Exception {
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);

        assertThat(trackedViews).hasSize(1);
    }

    @Test
    public void addView_withViewTreeObserverNotSet_shouldSetViewTreeObserver() {
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);
        View rootView = mock(View.class);

        when(view.getContext()).thenReturn(activity.getApplicationContext());
        when(view.getRootView()).thenReturn(rootView);
        when(rootView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new VisibilityTracker(activity.getApplicationContext(), trackedViews,
                visibilityChecker, visibilityHandler);
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);

        assertThat(subject.mWeakViewTreeObserver.get()).isEqualTo(viewTreeObserver);
    }

    @Test(expected = NullPointerException.class)
    public void addView_whenViewIsNull_shouldThrowNPE() throws Exception {
        subject.addView(null, MIN_PERCENTAGE_VIEWED, null);

        assertThat(trackedViews).isEmpty();
    }

    @Test
    public void removeView_shouldRemoveFromTrackedViews() throws Exception {
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews).containsKey(view);

        subject.removeView(view);

        assertThat(trackedViews).isEmpty();
    }

    @Test
    public void clear_shouldRemoveAllViewsFromTrackedViews_shouldRemoveMessagesFromVisibilityHandler_shouldResetIsVisibilityScheduled() throws Exception {
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);
        subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        assertThat(trackedViews).hasSize(2);

        subject.clear();

        assertThat(trackedViews).isEmpty();
        verify(visibilityHandler).removeMessages(0);
    }

    @Test
    public void destroy_shouldCallClear_shouldRemoveListenerFromDecorView() throws Exception {
        Activity activity1 = spy(Robolectric.buildActivity(Activity.class).create().get());
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(activity1.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.findViewById(anyInt())).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new VisibilityTracker(activity1, trackedViews, visibilityChecker, visibilityHandler);

        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);
        subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        assertThat(trackedViews).hasSize(2);

        subject.destroy();

        assertThat(trackedViews).isEmpty();
        verify(visibilityHandler).removeMessages(0);
        verify(viewTreeObserver).removeOnPreDrawListener(any(OnPreDrawListener.class));
        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
    }

    @Test
    public void visibilityRunnable_run_withVisibleView_shouldCallOnVisibleCallback() throws Exception {
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);

        subject.new VisibilityRunnable().run();

        verify(visibilityTrackerListener).onVisibilityChanged(
                Lists.newArrayList(view), Lists.<View>newArrayList());
    }

    @Test
    public void visibilityRunnable_run_withNonVisibleView_shouldCallOnNonVisibleCallback() throws Exception {
        when(view.getVisibility()).thenReturn(View.INVISIBLE);
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);

        subject.new VisibilityRunnable().run();

        ArgumentCaptor<List> visibleCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> invisibleCaptor = ArgumentCaptor.forClass(List.class);
        // noinspection unchecked
        verify(visibilityTrackerListener).onVisibilityChanged(visibleCaptor.capture(),
                invisibleCaptor.capture());
        assertThat(visibleCaptor.getValue().size()).isEqualTo(0);
        assertThat(invisibleCaptor.getValue().size()).isEqualTo(1);
    }

    // VisibilityChecker tests

    @Test
    public void hasRequiredTimeElapsed_withElapsedTimeGreaterThanMinTimeViewed_shouldReturnTrue() throws Exception {
        assertThat(visibilityChecker.hasRequiredTimeElapsed(SystemClock.uptimeMillis() - 501, 500)).isTrue();
    }

    @Test
    public void hasRequiredTimeElapsed_withElapsedTimeLessThanMinTimeViewed_shouldReturnFalse() throws Exception {
        assertThat(visibilityChecker.hasRequiredTimeElapsed(SystemClock.uptimeMillis() - 499, 500)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenParentIsNull_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 100, 100, 100, 100, false, true);
        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenViewIsOffScreen_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, false);
        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenViewIsEntirelyOnScreen_shouldReturnTrue() throws Exception {
        view = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isTrue();
    }

    @Test
    public void isMostlyVisible_whenViewIs50PercentVisible_shouldReturnTrue() throws Exception {
        view = createViewMock(View.VISIBLE, 50, 100, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isTrue();
    }

    @Test
    public void isMostlyVisible_whenViewIs49PercentVisible_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 49, 100, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenVisibleAreaIsZero_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 0, 0, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenViewIsInvisibleOrGone_shouldReturnFalse() throws Exception {
        View view = createViewMock(View.INVISIBLE, 100, 100, 100, 100, true, true);
        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();

        reset(view);
        view = createViewMock(View.GONE, 100, 100, 100, 100, true, true);
        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenViewHasZeroWidthAndHeight_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 100, 100, 0, 0, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenViewIsNull_shouldReturnFalse() throws Exception {
        assertThat(visibilityChecker.isVisible(null, null, MIN_PERCENTAGE_VIEWED, null)).isFalse();
    }

    @Test
    public void isMostlyVisible_whenVisibleAreaIsCheckedByPixel_shouldReturnTrue() throws Exception {
        view = createViewMock(View.VISIBLE, 90, 90, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED,
                DEFAULT_MIN_VISIBLE_PX)).isTrue();
    }

    @Test
    public void isVisible_whenVisibleAreaIsCheckedByPixel_withExactlyOnePixelVisible_shouldReturnTrue() throws Exception {
        view = createViewMock(View.VISIBLE, 1, 1, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED,
                DEFAULT_MIN_VISIBLE_PX)).isTrue();
    }

    @Test
    public void isVisible_whenVisibleAreaIsCheckedByPixel_withLargeNonDefaultMinimumPixel_shouldReturnFalse() throws Exception {
        view = createViewMock(View.VISIBLE, 3, 3, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED,
                25)).isFalse();
    }

    @Test
    public void isVisible_whenVisibleAreaIsCheckedByPixel_withSmallNonDefaultMinimumPixel_shouldReturnTrue() throws Exception {
        view = createViewMock(View.VISIBLE, 3, 3, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(view, view, MIN_PERCENTAGE_VIEWED,
                5)).isTrue();
    }

    @Test
    public void addView_shouldClearViewAfterNumAccesses() {
        // Access 1 time
        subject.addView(view, MIN_PERCENTAGE_VIEWED, null);
        assertThat(trackedViews).hasSize(1);

        // Access 2-49 times
        for (int i = 0; i < VisibilityTracker.NUM_ACCESSES_BEFORE_TRIMMING - 2; ++i) {
            subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        }
        assertThat(trackedViews).hasSize(2);

        // 50th time
        subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        assertThat(trackedViews).hasSize(2);

        // 51-99
        for (int i = 0; i < VisibilityTracker.NUM_ACCESSES_BEFORE_TRIMMING - 1; ++i) {
            subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        }
        assertThat(trackedViews).hasSize(2);

        // 100
        subject.addView(view2, MIN_PERCENTAGE_VIEWED, null);
        assertThat(trackedViews).hasSize(1);
    }

    static View createViewMock(final int visibility,
            final int visibleWidth,
            final int visibleHeight,
            final int viewWidth,
            final int viewHeight,
            final boolean isParentSet,
            final boolean isOnScreen) {
        View view = mock(View.class);
        when(view.getContext()).thenReturn(new Activity());
        when(view.getVisibility()).thenReturn(visibility);

        when(view.getGlobalVisibleRect(any(Rect.class)))
                .thenAnswer(new Answer<Boolean>() {
                    @Override
                    public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        Rect rect = (Rect) args[0];
                        rect.set(0, 0, visibleWidth, visibleHeight);
                        return isOnScreen;
                    }
                });

        when(view.getWidth()).thenReturn(viewWidth);
        when(view.getHeight()).thenReturn(viewHeight);

        if (isParentSet) {
            when(view.getParent()).thenReturn(mock(ViewParent.class));
        }

        when(view.getViewTreeObserver()).thenCallRealMethod();

        return view;
    }
}
