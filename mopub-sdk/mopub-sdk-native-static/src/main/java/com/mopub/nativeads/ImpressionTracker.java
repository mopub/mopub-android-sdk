package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.mopub.nativeads.VisibilityTracker.VisibilityChecker;
import static com.mopub.nativeads.VisibilityTracker.VisibilityTrackerListener;

/**
 * Impression tracker used to call {@link ImpressionInterface#recordImpression(View)} when a
 * percentage of a native ad has been on screen for a duration of time.
 */
public class ImpressionTracker {

    private static final int PERIOD = 250;

    // Object tracking visibility of added views
    @NonNull private final VisibilityTracker mVisibilityTracker;

    // All views and ads being tracked for impressions
    @NonNull private final Map<View, ImpressionInterface> mTrackedViews;

    // Visible views being polled for time on screen before tracking impression
    @NonNull private final Map<View, TimestampWrapper<ImpressionInterface>> mPollingViews;

    // Handler for polling visible views
    @NonNull private final Handler mPollHandler;

    // Runnable to run on each visibility loop
    @NonNull private final PollingRunnable mPollingRunnable;

    // Object to check actual visibility
    @NonNull private final VisibilityChecker mVisibilityChecker;

    // Listener for when a view becomes visible or non visible
    @Nullable private VisibilityTrackerListener mVisibilityTrackerListener;

    public ImpressionTracker(@NonNull final Context context) {
        this(new WeakHashMap<View, ImpressionInterface>(),
                new WeakHashMap<View, TimestampWrapper<ImpressionInterface>>(),
                new VisibilityChecker(),
                new VisibilityTracker(context),
                new Handler(Looper.getMainLooper()));
    }

    @VisibleForTesting
    ImpressionTracker(@NonNull final Map<View, ImpressionInterface> trackedViews,
            @NonNull final Map<View, TimestampWrapper<ImpressionInterface>> pollingViews,
            @NonNull final VisibilityChecker visibilityChecker,
            @NonNull final VisibilityTracker visibilityTracker,
            @NonNull final Handler handler) {
        mTrackedViews = trackedViews;
        mPollingViews = pollingViews;
        mVisibilityChecker = visibilityChecker;
        mVisibilityTracker = visibilityTracker;

        mVisibilityTrackerListener = new VisibilityTrackerListener() {
            @Override
            public void onVisibilityChanged(@NonNull final List<View> visibleViews, @NonNull final List<View> invisibleViews) {
                for (final View view : visibleViews) {
                    // It's possible for native ad to be null if the view was GC'd from this class
                    // but not from VisibilityTracker
                    // If it's null then clean up the view from this class
                    final ImpressionInterface impressionInterface = mTrackedViews.get(view);
                    if (impressionInterface == null) {
                        removeView(view);
                        continue;
                    }

                    // If the native ad is already polling, don't recreate it
                    final TimestampWrapper<ImpressionInterface> polling = mPollingViews.get(view);
                    if (polling != null && impressionInterface.equals(polling.mInstance)) {
                        continue;
                    }

                    // Add a new polling view
                    mPollingViews.put(view, new TimestampWrapper<ImpressionInterface>(impressionInterface));
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
    public void addView(final View view, @NonNull final ImpressionInterface impressionInterface) {
        // View is already associated with the same native ad
        if (mTrackedViews.get(view) == impressionInterface) {
            return;
        }

        // Clean up state if view is being recycled and associated with a different ad
        removeView(view);

        if (impressionInterface.isImpressionRecorded()) {
            return;
        }

        mTrackedViews.put(view, impressionInterface);
        mVisibilityTracker.addView(view, impressionInterface.getImpressionMinPercentageViewed(),
                impressionInterface.getImpressionMinVisiblePx());
    }

    public void removeView(final View view) {
        mTrackedViews.remove(view);
        removePollingView(view);
        mVisibilityTracker.removeView(view);
    }

    /**
     * Immediately clear all views. Useful for when we re-request ads for an ad placer
     */
    public void clear() {
        mTrackedViews.clear();
        mPollingViews.clear();
        mVisibilityTracker.clear();
        mPollHandler.removeMessages(0);
    }

    public void destroy() {
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
        @NonNull private final ArrayList<View> mRemovedViews;

        PollingRunnable() {
            mRemovedViews = new ArrayList<View>();
        }

        @Override
        public void run() {
            for (final Map.Entry<View, TimestampWrapper<ImpressionInterface>> entry : mPollingViews.entrySet()) {
                final View view = entry.getKey();
                final TimestampWrapper<ImpressionInterface> timestampWrapper = entry.getValue();

                // If it's been visible for the min impression time, trigger the callback
                if (!mVisibilityChecker.hasRequiredTimeElapsed(
                        timestampWrapper.mCreatedTimestamp,
                        timestampWrapper.mInstance.getImpressionMinTimeViewed())) {
                    continue;
                }

                timestampWrapper.mInstance.recordImpression(view);
                timestampWrapper.mInstance.setImpressionRecorded();

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

    @Nullable
    @Deprecated
    @VisibleForTesting
    VisibilityTrackerListener getVisibilityTrackerListener() {
        return mVisibilityTrackerListener;
    }
}
