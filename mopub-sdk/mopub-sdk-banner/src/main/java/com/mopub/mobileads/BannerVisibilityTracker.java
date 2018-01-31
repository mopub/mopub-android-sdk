package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Views;

import java.lang.ref.WeakReference;

import static android.view.ViewTreeObserver.OnPreDrawListener;

/**
 * Tracks banner views to determine when they become visible, where visibility is determined by
 * whether a minimum number of dips have been visible for a minimum duration, where both values are
 * configured by the AdServer via headers.
 */
class BannerVisibilityTracker {
    // Time interval to use for throttling visibility checks.
    private static final int VISIBILITY_THROTTLE_MILLIS = 100;

    /**
     * Callback when visibility conditions are satisfied.
     */
    interface BannerVisibilityTrackerListener {
        void onVisibilityChanged();
    }

    @NonNull @VisibleForTesting final OnPreDrawListener mOnPreDrawListener;
    @NonNull @VisibleForTesting WeakReference<ViewTreeObserver> mWeakViewTreeObserver;

    /**
     * Banner view that is being tracked.
     */
    @NonNull private final View mTrackedView;

    /**
     * Root view of banner view being tracked.
     */
    @NonNull private final View mRootView;

    /**
     * Object to check actual visibility.
     */
    @NonNull private final BannerVisibilityChecker mVisibilityChecker;

    /**
     * Callback listener.
     */
    @Nullable private BannerVisibilityTrackerListener mBannerVisibilityTrackerListener;

    /**
     * Runnable to run on each visibility loop.
     */
    @NonNull private final BannerVisibilityRunnable mVisibilityRunnable;

    /**
     * Handler for visibility.
     */
    @NonNull private final Handler mVisibilityHandler;

    /**
     * Whether the visibility runnable is scheduled.
     */
    private boolean mIsVisibilityScheduled;

    /**
     * Whether the imp tracker has been fired already.
     */
    private boolean mIsImpTrackerFired;

    @VisibleForTesting
    public BannerVisibilityTracker(@NonNull final Context context,
            @NonNull final View rootView,
            @NonNull final View trackedView,
            final int minVisibleDips,
            final int minVisibleMillis) {
        Preconditions.checkNotNull(rootView);
        Preconditions.checkNotNull(trackedView);

        mRootView = rootView;
        mTrackedView = trackedView;

        mVisibilityChecker = new BannerVisibilityChecker(minVisibleDips, minVisibleMillis);
        mVisibilityHandler = new Handler();
        mVisibilityRunnable = new BannerVisibilityRunnable();

        mOnPreDrawListener = new OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                scheduleVisibilityCheck();
                return true;
            }
        };

        mWeakViewTreeObserver = new WeakReference<ViewTreeObserver>(null);
        setViewTreeObserver(context, mTrackedView);
    }

    private void setViewTreeObserver(@Nullable final Context context, @Nullable final View view) {
        final ViewTreeObserver originalViewTreeObserver = mWeakViewTreeObserver.get();
        if (originalViewTreeObserver != null && originalViewTreeObserver.isAlive()) {
            return;
        }

        final View rootView = Views.getTopmostView(context, view);
        if (rootView == null) {
            MoPubLog.d("Unable to set Visibility Tracker due to no available root view.");
            return;
        }

        final ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        if (!viewTreeObserver.isAlive()) {
            MoPubLog.w("Visibility Tracker was unable to track views because the"
                    + " root view tree observer was not alive");
            return;
        }

        mWeakViewTreeObserver = new WeakReference<>(viewTreeObserver);
        viewTreeObserver.addOnPreDrawListener(mOnPreDrawListener);
    }

    @Nullable
    @Deprecated
    @VisibleForTesting
    BannerVisibilityTrackerListener getBannerVisibilityTrackerListener() {
        return mBannerVisibilityTrackerListener;
    }

    void setBannerVisibilityTrackerListener(
            @Nullable final BannerVisibilityTrackerListener bannerVisibilityTrackerListener) {
        mBannerVisibilityTrackerListener = bannerVisibilityTrackerListener;
    }

    /**
     * Destroy the visibility tracker, preventing it from future use.
     */
    void destroy() {
        mVisibilityHandler.removeMessages(0);
        mIsVisibilityScheduled = false;
        final ViewTreeObserver viewTreeObserver = mWeakViewTreeObserver.get();
        if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnPreDrawListener(mOnPreDrawListener);
        }
        mWeakViewTreeObserver.clear();
        mBannerVisibilityTrackerListener = null;
    }

    void scheduleVisibilityCheck() {
        // Tracking this directly instead of calling hasMessages directly because we measured that
        // this led to slightly better performance.
        if (mIsVisibilityScheduled) {
            return;
        }

        mIsVisibilityScheduled = true;
        mVisibilityHandler.postDelayed(mVisibilityRunnable, VISIBILITY_THROTTLE_MILLIS);
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    BannerVisibilityChecker getBannerVisibilityChecker() {
        return mVisibilityChecker;
    }

    @NonNull
    @Deprecated
    @VisibleForTesting
    Handler getVisibilityHandler() {
        return mVisibilityHandler;
    }

    @Deprecated
    @VisibleForTesting
    boolean isVisibilityScheduled() {
        return mIsVisibilityScheduled;
    }

    @Deprecated
    @VisibleForTesting
    boolean isImpTrackerFired() {
        return mIsImpTrackerFired;
    }

    class BannerVisibilityRunnable implements Runnable {
        @Override
        public void run() {
            if (mIsImpTrackerFired) {
                return;
            }

            mIsVisibilityScheduled = false;

            // If the view meets the dips count requirement for visibility, then also check the
            // duration requirement for visibility.
            if (mVisibilityChecker.isVisible(mRootView, mTrackedView)) {
                // Start the timer for duration requirement if it hasn't already.
                if (!mVisibilityChecker.hasBeenVisibleYet()) {
                    mVisibilityChecker.setStartTimeMillis();
                }

                if (mVisibilityChecker.hasRequiredTimeElapsed()) {
                    if (mBannerVisibilityTrackerListener != null) {
                        mBannerVisibilityTrackerListener.onVisibilityChanged();
                        mIsImpTrackerFired = true;
                    }
                }
            }

            // If visibility requirements are not met, check again later.
            if (!mIsImpTrackerFired) {
                scheduleVisibilityCheck();
            }
        }
    }

    static class BannerVisibilityChecker {
        private int mMinVisibleDips;
        private int mMinVisibleMillis;
        private long mStartTimeMillis = Long.MIN_VALUE;

        // A rect to use for hit testing. Create this once to avoid excess garbage collection
        private final Rect mClipRect = new Rect();

        BannerVisibilityChecker(final int minVisibleDips, final int minVisibleMillis) {
            mMinVisibleDips = minVisibleDips;
            mMinVisibleMillis = minVisibleMillis;
        }

        boolean hasBeenVisibleYet() {
            return mStartTimeMillis != Long.MIN_VALUE;
        }

        void setStartTimeMillis() {
            mStartTimeMillis = SystemClock.uptimeMillis();
        }

        /**
         * Whether the visible time has elapsed from the start time.
         */
        boolean hasRequiredTimeElapsed() {
            if (!hasBeenVisibleYet()) {
                return false;
            }

            return SystemClock.uptimeMillis() - mStartTimeMillis >= mMinVisibleMillis;
        }

        /**
         * Whether the visible dips count requirement is met.
         */
        boolean isVisible(@Nullable final View rootView, @Nullable final View view) {
            // ListView & GridView both call detachFromParent() for views that can be recycled for
            // new data. This is one of the rare instances where a view will have a null parent for
            // an extended period of time and will not be the main window.
            // view.getGlobalVisibleRect() doesn't check that case, so if the view has visibility
            // of View.VISIBLE but its group has no parent it is likely in the recycle bin of a
            // ListView / GridView and not on screen.
            if (view == null || view.getVisibility() != View.VISIBLE || rootView.getParent() == null) {
                return false;
            }

            // If either width or height is non-positive, the view cannot be visible.
            if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                return false;
            }

            // View completely clipped by its parents
            if (!view.getGlobalVisibleRect(mClipRect)) {
                return false;
            }

            // Calculate area of view not clipped by any of its parents
            final int widthInDips = Dips.pixelsToIntDips((float) mClipRect.width(),
                    view.getContext());
            final int heightInDips = Dips.pixelsToIntDips((float) mClipRect.height(),
                    view.getContext());
            final long visibleViewAreaInDips = (long) (widthInDips * heightInDips);

            return visibleViewAreaInDips >= mMinVisibleDips;
        }
    }
}

