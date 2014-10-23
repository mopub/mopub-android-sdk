package com.mopub.nativeads;

import android.os.Handler;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;

import org.fest.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.HashMap;

import static com.mopub.nativeads.MoPubNative.MoPubNativeListener;
import static com.mopub.nativeads.VisibilityTracker.VisibilityChecker;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ImpressionTrackerTest {
    private ImpressionTracker subject;
    private TimestampWrapper<NativeResponse> timeStampWrapper;
    private HashMap<View, NativeResponse> trackedViews;
    private HashMap<View, TimestampWrapper<NativeResponse>> pollingViews;

    @Mock private NativeResponse nativeResponse;
    @Mock private NativeResponse nativeResponse2;
    @Mock private MoPubNativeListener moPubNativeListener;
    @Mock private VisibilityTracker visibilityTracker;
    @Mock private Handler handler;
    @Mock private View view;
    @Mock private View view2;

    @Before
    public void setUp() {
        view = VisibilityTrackerTest.createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);
        view2 = VisibilityTrackerTest.createViewMock(View.VISIBLE, 100, 100, 100, 100, true, true);

        pollingViews = new HashMap<View, TimestampWrapper<NativeResponse>>(10);
        trackedViews = new HashMap<View, NativeResponse>(10);
        final VisibilityChecker visibilityChecker = new VisibilityChecker();
        subject = new ImpressionTracker(trackedViews, pollingViews, visibilityChecker,
                visibilityTracker, handler);

        timeStampWrapper = new TimestampWrapper<NativeResponse>(nativeResponse);

        when(nativeResponse.getImpressionMinPercentageViewed()).thenReturn(50);
        when(nativeResponse.getImpressionMinTimeViewed()).thenReturn(1000);
        when(nativeResponse2.getImpressionMinPercentageViewed()).thenReturn(50);
        when(nativeResponse2.getImpressionMinTimeViewed()).thenReturn(1000);

        // XXX We need this to ensure that our SystemClock starts
        ShadowSystemClock.uptimeMillis();
    }

    @Test
    public void addView_shouldAddViewToTrackedViews_shouldAddViewToVisibilityTracker() {
        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse);
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void addView_withRecordedImpression_shouldNotAddView() {
        when(nativeResponse.getRecordedImpression()).thenReturn(true);

        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(0);
        verify(visibilityTracker, never())
                .addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void addView_withDestroyedNativeResponse_shouldNotAddView() {
        when(nativeResponse.isDestroyed()).thenReturn(true);

        subject.addView(view, nativeResponse);

        assertThat(trackedViews).isEmpty();
        verify(visibilityTracker, never())
                .addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void addView_withDifferentNativeResponse_shouldRemoveFromPollingViews() {
        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse);
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());

        pollingViews.put(view, timeStampWrapper);

        subject.addView(view, nativeResponse2);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse2);
        assertThat(pollingViews).isEmpty();
        verify(visibilityTracker, times(2))
                .addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void addView_withDifferentAlreadyImpressedNativeResponse_shouldRemoveFromPollingViews_shouldNotTrack() {
        when(nativeResponse2.getRecordedImpression()).thenReturn(true);

        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse);
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());

        pollingViews.put(view, timeStampWrapper);

        subject.addView(view, nativeResponse2);

        assertThat(trackedViews).hasSize(0);
        assertThat(trackedViews.get(view)).isNull();
        assertThat(pollingViews).isEmpty();
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void addView_withSameNativeResponse_shouldNotAddView() {
        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse);
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());

        pollingViews.put(view, timeStampWrapper);

        subject.addView(view, nativeResponse);

        assertThat(trackedViews).hasSize(1);
        assertThat(trackedViews.get(view)).isEqualTo(nativeResponse);
        assertThat(pollingViews.keySet()).containsOnly(view);

        // Still only one call
        verify(visibilityTracker).addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    @Test
    public void removeView_shouldRemoveViewFromViewTrackedViews_shouldRemoveViewFromPollingMap_shouldRemoveViewFromVisibilityTracker() {
        trackedViews.put(view, nativeResponse);
        pollingViews.put(view, new TimestampWrapper<NativeResponse>(nativeResponse));
        visibilityTracker.addView(view, nativeResponse.getImpressionMinPercentageViewed());

        subject.removeView(view);

        assertThat(trackedViews).isEmpty();
        assertThat(pollingViews).isEmpty();
        verify(visibilityTracker).removeView(view);
    }

    @Test
    public void clear_shouldClearViewTrackedViews_shouldClearPollingViews_shouldClearVisibilityTracker_shouldClearPollHandler() {
        trackedViews.put(view, nativeResponse);
        trackedViews.put(view2, nativeResponse);
        pollingViews.put(view, timeStampWrapper);
        pollingViews.put(view2, timeStampWrapper);
        visibilityTracker.addView(view, nativeResponse.getImpressionMinPercentageViewed());
        visibilityTracker.addView(view2, nativeResponse.getImpressionMinPercentageViewed());

        subject.clear();

        assertThat(trackedViews).isEmpty();
        assertThat(pollingViews).isEmpty();
        verify(visibilityTracker).clear();
        verify(handler).removeMessages(0);
    }
    
    @Test
    public void destroy_shouldCallClear_shouldDestroyVisibilityTracker_shouldSetVisibilityTrackerListenerToNull() throws Exception {
        trackedViews.put(view, nativeResponse);
        trackedViews.put(view2, nativeResponse);
        pollingViews.put(view, timeStampWrapper);
        pollingViews.put(view2, timeStampWrapper);
        visibilityTracker.addView(view, nativeResponse.getImpressionMinPercentageViewed());
        visibilityTracker.addView(view2, nativeResponse.getImpressionMinPercentageViewed());
        assertThat(subject.getVisibilityTrackerListener()).isNotNull();

        subject.destroy();

        assertThat(trackedViews).isEmpty();
        assertThat(pollingViews).isEmpty();
        verify(visibilityTracker).clear();
        verify(handler).removeMessages(0);

        verify(visibilityTracker).destroy();
        assertThat(subject.getVisibilityTrackerListener()).isNull();
    }

    @Test
    public void scheduleNextPoll_shouldPostDelayedThePollingRunnable() {
        when(handler.hasMessages(0)).thenReturn(false);

        subject.scheduleNextPoll();

        verify(handler).postDelayed(any(ImpressionTracker.PollingRunnable.class), eq((long) 250));
    }

    @Test
    public void scheduleNextPoll_withMessages_shouldNotPostDelayedThePollingRunnable() {
        when(handler.hasMessages(0)).thenReturn(true);

        subject.scheduleNextPoll();

        verify(handler, never())
                .postDelayed(any(ImpressionTracker.PollingRunnable.class), eq((long) 250));
    }

    @Test
    public void visibilityTrackerListener_onVisibilityChanged_withVisibleViews_shouldAddViewToPollingViews_shouldScheduleNextPoll() {
        subject.addView(view, nativeResponse);

        assertThat(pollingViews).isEmpty();

        subject.getVisibilityTrackerListener()
                .onVisibilityChanged(Lists.newArrayList(view), Lists.<View>newArrayList());

        assertThat(pollingViews.keySet()).containsOnly(view);
        verify(handler).postDelayed(any(ImpressionTracker.PollingRunnable.class), eq((long) 250));
    }

    @Test
    public void visibilityTrackerListener_onVisibilityChanged_withVisibleViews_shouldRemoveViewFromPollingViews() {
        subject.addView(view, nativeResponse);
        subject.getVisibilityTrackerListener()
                .onVisibilityChanged(Lists.newArrayList(view), Lists.<View>newArrayList());


        assertThat(trackedViews.keySet()).containsOnly(view);
        assertThat(pollingViews.keySet()).containsOnly(view);

        subject.getVisibilityTrackerListener()
                .onVisibilityChanged(Lists.<View>newArrayList(), Lists.newArrayList(view));

        assertThat(trackedViews.keySet()).containsOnly(view);
        assertThat(pollingViews).isEmpty();
    }

    @Test
    public void pollingRunnableRun_whenLessThanOneSecondHasElapsed_shouldNotTrackImpression_shouldScheduleNextPoll() {
        // Force the last viewed timestamp to be a known value
        timeStampWrapper.mCreatedTimestamp = 5555;
        pollingViews.put(view, timeStampWrapper);

        // We progress 999 milliseconds
        Robolectric.getUiThreadScheduler().advanceBy(5555 + 999);
        subject.new PollingRunnable().run();

        verify(nativeResponse, never()).recordImpression(view);

        assertThat(pollingViews.keySet()).containsOnly(view);
        verify(handler).postDelayed(any(ImpressionTracker.PollingRunnable.class), eq((long) 250));
    }

    @Test
    public void pollingRunnableRun_whenMoreThanOneSecondHasElapsed_shouldTrackImpression_shouldNotScheduleNextPoll() {
        // Force the last viewed timestamp to be a known value
        timeStampWrapper.mCreatedTimestamp = 5555;
        pollingViews.put(view, timeStampWrapper);

        // We progress 1000 milliseconds
        Robolectric.getUiThreadScheduler().advanceBy(5555 + 1000);
        subject.new PollingRunnable().run();

        verify(nativeResponse).recordImpression(view);

        assertThat(pollingViews).isEmpty();
        verify(handler, never())
                .postDelayed(any(ImpressionTracker.PollingRunnable.class), eq((long) 250));
    }

    @Test(expected = NullPointerException.class)
    public void pollingRunnableRun_whenWrapperIsNull_shouldThrowNPE() {
        pollingViews.put(view, null);
        subject.new PollingRunnable().run();

        verify(nativeResponse, never()).recordImpression(view);
    }

    @Test(expected = NullPointerException.class)
    public void pollingRunnableRun_whenNativeResponseIsNull_shouldThrowNPE() {
        // This doesn't normally happen; perhaps we're being overly defensive
        pollingViews.put(view, new TimestampWrapper<NativeResponse>(null));

        subject.new PollingRunnable().run();

        verify(nativeResponse, never()).recordImpression(view);
    }
}