package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.nativeads.VisibilityTracker.VisibilityChecker;
import static com.mopub.nativeads.VisibilityTracker.VisibilityTrackerListener;

class ImpressionTracker {

    private static final int PERIOD = 250;

    // Object tracking visibility of added views
    private final VisibilityTracker mVisibilityTracker;

    // All views and responses being tracked for impressions
    private final Map<View, NativeResponse> mTrackedViews;

    // Visible views being polled for time on screen before tracking impression
    private final Map<View, TimestampWrapper<NativeResponse>> mPollingViews;

    // Handler for polling visible views
    private final Handler mPollHandler;

    // Runnable to run on each visibility loop
    private final PollingRunnable mPollingRunnable;

    // Object to check actual visibility
    private final VisibilityChecker mVisibilityChecker;

    // Listener for when a view becomes visible or non visible
    private VisibilityTrackerListener mVisibilityTrackerListener;

    ImpressionTracker(final Context context) {
        this(new WeakHashMap<View, NativeResponse>(),
                new WeakHashMap<View, TimestampWrapper<NativeResponse>>(),
                new VisibilityChecker(),
                new VisibilityTracker(context),
                new Handler());
    }

    @VisibleForTesting
    ImpressionTracker(final Map<View, NativeResponse> trackedViews,
                      final Map<View, TimestampWrapper<NativeResponse>> pollingViews,
                      final VisibilityChecker visibilityChecker,
                      final VisibilityTracker visibilityTracker,
                      final Handler handler) {
        mTrackedViews = trackedViews;
        mPollingViews = pollingViews;
        mVisibilityChecker = visibilityChecker;
        mVisibilityTracker = visibilityTracker;

        mVisibilityTrackerListener = new VisibilityTrackerListener() {
            @Override
            public void onVisibilityChanged(final List<View> visibleViews, final List<View> invisibleViews) {
                for (final View view : visibleViews) {
                    // It's possible for native response to be null if the view was GC'd from this class
                    // but not from VisibilityTracker
                    // If it's null then clean up the view from this class
                    final NativeResponse nativeResponse = mTrackedViews.get(view);
                    if (nativeResponse == null) {
                        removeView(view);
                        continue;
                    }

                    // If the native response is already polling, don't recreate it
                    final TimestampWrapper<NativeResponse> polling = mPollingViews.get(view);
                    if (polling != null && nativeResponse.equals(polling.mInstance)) {
                        continue;
                    }

                    // Add a new polling view
                    mPollingViews.put(view, new TimestampWrapper<NativeResponse>(nativeResponse));
                }

                for (final View view : invisibleViews) {
                    mPollingViews.remove(view);
                }
                scheduleNextPoll();
            }
        };
        mVisibilityTracker.setVisibilityTrackerListener(mVisibilityTrackerListener);

        mPollHandler = handler;
        mPollingRunnable = new PollingRunnable();
    }

    /**
     * Tracks the given view for impressions.
     */
    void addView(final View view, final NativeResponse nativeResponse) {
        // View is already associated with same native response
        if (mTrackedViews.get(view) == nativeResponse) {
            return;
        }

        // Clean up state if view is being recycled and associated with a different response
        removeView(view);

        if (nativeResponse.getRecordedImpression() || nativeResponse.isDestroyed()) {
            return;
        }

        mTrackedViews.put(view, nativeResponse);
        mVisibilityTracker.addView(view, nativeResponse.getImpressionMinPercentageViewed());
    }

    void removeView(final View view) {
        mTrackedViews.remove(view);
        removePollingView(view);
        mVisibilityTracker.removeView(view);
    }

    /**
     * Immediately clear all views. Useful for when we re-request ads for an ad placer
     */
    void clear() {
        mTrackedViews.clear();
        mPollingViews.clear();
        mVisibilityTracker.clear();
        mPollHandler.removeMessages(0);
    }

    void destroy() {
        clear();
        mVisibilityTracker.destroy();
        mVisibilityTrackerListener = null;
    }

    @VisibleForTesting
    void scheduleNextPoll() {
        // Only schedule if there are no messages already scheduled.
        if (mPollHandler.hasMessages(0)) {
            return;
        }

        mPollHandler.postDelayed(mPollingRunnable, PERIOD);
    }

    private void removePollingView(final View view) {
        mPollingViews.remove(view);
    }

    @VisibleForTesting
    class PollingRunnable implements Runnable {
        // Create this once to avoid excessive garbage collection observed when calculating
        // these on each pass.
        private final ArrayList<View> mRemovedViews;

        PollingRunnable() {
            mRemovedViews = new ArrayList<View>();
        }

        @Override
        public void run() {
            for (final Map.Entry<View, TimestampWrapper<NativeResponse>> entry : mPollingViews.entrySet()) {
                final View view = entry.getKey();
                final TimestampWrapper<NativeResponse> timestampWrapper = entry.getValue();

                // If it's been visible for the min impression time, trigger the callback
                if (!mVisibilityChecker.hasRequiredTimeElapsed(
                        timestampWrapper.mCreatedTimestamp,
                        timestampWrapper.mInstance.getImpressionMinTimeViewed())) {
                    continue;
                }

                timestampWrapper.mInstance.recordImpression(view);

                // Removed in a separate loop to avoid a ConcurrentModification exception.
                mRemovedViews.add(view);
            }

            for (View view : mRemovedViews) {
              removeView(view);
            }
            mRemovedViews.clear();

            if (!mPollingViews.isEmpty()) {
                scheduleNextPoll();
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    VisibilityTrackerListener getVisibilityTrackerListener() {
        return mVisibilityTrackerListener;
    }
}