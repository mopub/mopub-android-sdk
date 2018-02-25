package com.mopub.mobileads;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import static android.view.ViewTreeObserver.OnPreDrawListener;
import static com.mopub.mobileads.BannerVisibilityTracker.BannerVisibilityChecker;
import static com.mopub.mobileads.BannerVisibilityTracker.BannerVisibilityTrackerListener;
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
public class BannerVisibilityTrackerTest {
    private static final int MIN_VISIBLE_DIPS = 1;
    private static final int MIN_VISIBLE_MILLIS = 0;

    private Activity activity;
    private BannerVisibilityTracker subject;
    private BannerVisibilityChecker visibilityChecker;
    private Handler visibilityHandler;

    private View mockView;
    @Mock
    private BannerVisibilityTrackerListener visibilityTrackerListener;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        mockView = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);
        subject = new BannerVisibilityTracker(activity, mockView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);

        subject.setBannerVisibilityTrackerListener(visibilityTrackerListener);

        visibilityChecker = subject.getBannerVisibilityChecker();
        visibilityHandler = subject.getVisibilityHandler();

        // XXX We need this to ensure that our SystemClock starts
        ShadowSystemClock.uptimeMillis();
    }

    @Test
    public void constructor_shouldSetOnPreDrawListenerForDecorView() throws Exception {
        Activity spyActivity = spy(Robolectric.buildActivity(Activity.class).create().get());
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(spyActivity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.findViewById(anyInt())).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new BannerVisibilityTracker(spyActivity, mockView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);
        assertThat(subject.mOnPreDrawListener).isNotNull();
        verify(viewTreeObserver).addOnPreDrawListener(subject.mOnPreDrawListener);
        assertThat(subject.mWeakViewTreeObserver.get()).isEqualTo(viewTreeObserver);
    }

    @Test
    public void constructor_withNonAliveViewTreeObserver_shouldNotSetOnPreDrawListenerForDecorView() throws Exception {
        Activity mockActivity = mock(Activity.class);
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(mockActivity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(false);

        subject = new BannerVisibilityTracker(mockActivity, mockView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);
        verify(viewTreeObserver, never()).addOnPreDrawListener(subject.mOnPreDrawListener);
        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
    }

    @Test
    public void constructor_withApplicationContext_shouldNotSetOnPreDrawListener() {
        subject = new BannerVisibilityTracker(activity.getApplicationContext(), mockView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);

        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
    }

    @Test
    public void constructor_withViewTreeObserverNotSet_shouldSetViewTreeObserver() {
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);
        View rootView = mock(View.class);

        when(mockView.getContext()).thenReturn(activity.getApplicationContext());
        when(mockView.getRootView()).thenReturn(rootView);
        when(rootView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new BannerVisibilityTracker(activity.getApplicationContext(), rootView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);
        assertThat(subject.mWeakViewTreeObserver.get()).isEqualTo(viewTreeObserver);
    }

    @Test
    public void destroy_shouldRemoveListenerFromDecorView() throws Exception {
        Activity spyActivity = spy(Robolectric.buildActivity(Activity.class).create().get());
        Window window = mock(Window.class);
        View decorView = mock(View.class);
        ViewTreeObserver viewTreeObserver = mock(ViewTreeObserver.class);

        when(spyActivity.getWindow()).thenReturn(window);
        when(window.getDecorView()).thenReturn(decorView);
        when(decorView.findViewById(anyInt())).thenReturn(decorView);
        when(decorView.getViewTreeObserver()).thenReturn(viewTreeObserver);
        when(viewTreeObserver.isAlive()).thenReturn(true);

        subject = new BannerVisibilityTracker(spyActivity, mockView, mockView, MIN_VISIBLE_DIPS, MIN_VISIBLE_MILLIS);
        subject.destroy();

        assertThat(visibilityHandler.hasMessages(0)).isFalse();
        assertThat(subject.isVisibilityScheduled()).isFalse();
        verify(viewTreeObserver).removeOnPreDrawListener(any(OnPreDrawListener.class));
        assertThat(subject.mWeakViewTreeObserver.get()).isNull();
        assertThat(subject.getBannerVisibilityTrackerListener()).isNull();
    }

    // BannerVisibilityRunnable Tests
    @Test
    public void visibilityRunnable_run_withViewVisibleForAtLeastMinDuration_shouldCallOnVisibilityChangedCallback_shouldMarkImpTrackerAsFired_shouldNotScheduleVisibilityCheck() throws Exception {
        subject.new BannerVisibilityRunnable().run();

        verify(visibilityTrackerListener).onVisibilityChanged();
        assertThat(subject.isImpTrackerFired()).isTrue();
        assertThat(subject.isVisibilityScheduled()).isFalse();
    }

    @Test
    public void visibilityRunnable_run_withViewNotVisible_shouldNotCallOnVisibilityChangedCallback_shouldNotMarkImpTrackerAsFired_shouldScheduleVisibilityCheck() throws Exception {
        when(mockView.getVisibility()).thenReturn(View.INVISIBLE);

        subject.new BannerVisibilityRunnable().run();

        verify(visibilityTrackerListener, never()).onVisibilityChanged();
        assertThat(subject.isImpTrackerFired()).isFalse();
        assertThat(subject.isVisibilityScheduled()).isTrue();
    }

    @Test
    public void visibilityRunnable_run_witViewVisibleForLessThanMinDuration_shouldNotCallOnVisibilityChangedCallback_shouldNotMarkImpTrackerAsFired_shouldScheduleVisibilityCheck() throws Exception {
        subject = new BannerVisibilityTracker(activity, mockView, mockView, 1, 1000);
        subject.new BannerVisibilityRunnable().run();

        verify(visibilityTrackerListener, never()).onVisibilityChanged();
        assertThat(subject.isImpTrackerFired()).isFalse();
        assertThat(subject.isVisibilityScheduled()).isTrue();
    }

    // BannerVisibilityChecker Tests
    @Test
    public void hasRequiredTimeElapsed_withStartTimeNotSetYet_shouldReturnFalse() throws Exception {
        assertThat(visibilityChecker.hasRequiredTimeElapsed()).isFalse();
    }

    @Test
    public void hasRequiredTimeElapsed_withStartTimeSet_withElapsedTimeGreaterThanMinTimeViewed_shouldReturnTrue() throws Exception {
        visibilityChecker.setStartTimeMillis();

        // minVisibleMillis is 0 ms as defined by constant MIN_VISIBLE_MILLIS
        assertThat(visibilityChecker.hasRequiredTimeElapsed()).isTrue();
    }

    @Test
    public void hasRequiredTimeElapsed_withStartTimeSet_withElapsedTimeLessThanMinTimeViewed_shouldReturnFalse() throws Exception {
        subject = new BannerVisibilityTracker(activity, mockView, mockView, 1, 1000);
        visibilityChecker = subject.getBannerVisibilityChecker();
        visibilityChecker.setStartTimeMillis();

        // minVisibleMillis is 1 sec, should return false since we are checking immediately before 1 sec elapses
        assertThat(visibilityChecker.hasRequiredTimeElapsed()).isFalse();
    }

    @Test
    public void isVisible_whenParentIsNull_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 100, 100, 100, 100, false, true);
        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenViewIsOffScreen_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, false);
        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenViewIsEntirelyOnScreen_shouldReturnTrue() throws Exception {
        mockView = createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isTrue();
    }

    @Test
    public void isVisible_whenViewHasMoreVisibleDipsThanMinVisibleDips_shouldReturnTrue() throws Exception {
        mockView = createViewMock(View.VISIBLE, 1, 2, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isTrue();
    }

    @Test
    public void isVisible_whenViewHasExactlyMinVisibleDips_shouldReturnTrue() throws Exception {
        mockView = createViewMock(View.VISIBLE, 1, 1, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isTrue();
    }

    @Test
    public void isVisible_whenViewHasLessVisibleDipsThanMinVisibleDips_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 0, 1, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenVisibleAreaIsZero_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 0, 0, 100, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenViewIsInvisibleOrGone_shouldReturnFalse() throws Exception {
        View view = createViewMock(View.INVISIBLE, 100, 100, 100, 100, true, true);
        assertThat(visibilityChecker.isVisible(view, view)).isFalse();

        reset(view);
        view = createViewMock(View.GONE, 100, 100, 100, 100, true, true);
        assertThat(visibilityChecker.isVisible(view, view)).isFalse();
    }

    @Test
    public void isVisible_whenViewHasZeroWidth_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 100, 100, 0, 100, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenViewHasZeroHeight_shouldReturnFalse() throws Exception {
        mockView = createViewMock(View.VISIBLE, 100, 100, 100, 0, true, true);

        assertThat(visibilityChecker.isVisible(mockView, mockView)).isFalse();
    }

    @Test
    public void isVisible_whenViewIsNull_shouldReturnFalse() throws Exception {
        assertThat(visibilityChecker.isVisible(null, null)).isFalse();
    }

    View createViewMock(final int visibility,
            final int visibleWidth,
            final int visibleHeight,
            final int viewWidth,
            final int viewHeight,
            final boolean isParentSet,
            final boolean isOnScreen) {
        View view = mock(View.class);
        when(view.getContext()).thenReturn(activity);
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
