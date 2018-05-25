package com.mopub.mraid;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mopub.common.AdReport;
import com.mopub.common.CloseableLayout;
import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.RewardedMraidCountdownRunnable;
import com.mopub.mobileads.VastVideoRadialCountdownWidget;

import static com.mopub.common.IntentActions.ACTION_REWARDED_PLAYABLE_COMPLETE;
import static com.mopub.mobileads.BaseBroadcastReceiver.broadcastAction;

public class RewardedMraidController extends MraidController {

    /**
     * Should reward on click is for when the user should be rewarded when clicking on the
     * creative. This is defaulted to false so the user must wait for the entire countdown
     * before able to leave the app.
     */
    public static final boolean DEFAULT_PLAYABLE_SHOULD_REWARD_ON_CLICK = false;

    /**
     * If a duration is not specified, this duration is used. 30 seconds is also the maximum
     * amount of time that we currently allow rewarded playables to be not closeable.
     */
    public static final int DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS = 30;

    public static final int MILLIS_IN_SECOND = 1000;

    @VisibleForTesting
    static final int DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS =
            DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_SECONDS * MILLIS_IN_SECOND;
    @VisibleForTesting
    static final long PLAYABLE_COUNTDOWN_UPDATE_INTERVAL_MILLIS = 250;

    @NonNull private CloseableLayout mCloseableLayout;
    @NonNull private VastVideoRadialCountdownWidget mRadialCountdownWidget;
    @NonNull private RewardedMraidCountdownRunnable mCountdownRunnable;

    private final int mShowCloseButtonDelay;
    private final long mBroadcastIdentifier;

    private int mCurrentElapsedTimeMillis;
    private boolean mShowCloseButtonEventFired;
    private boolean mIsCalibrationDone;
    private boolean mIsRewarded;

    @VisibleForTesting
    public RewardedMraidController(@NonNull Context context, @Nullable AdReport adReport,
            @NonNull PlacementType placementType, int rewardedDurationInSeconds,
            long broadcastIdentifier) {
        super(context, adReport, placementType);
        final int rewardedDurationInMillis = rewardedDurationInSeconds * MILLIS_IN_SECOND;
        if (rewardedDurationInMillis < 0
                || rewardedDurationInMillis > DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS) {
            mShowCloseButtonDelay = DEFAULT_PLAYABLE_DURATION_FOR_CLOSE_BUTTON_MILLIS;
        } else {
            mShowCloseButtonDelay = rewardedDurationInMillis;
        }
        mBroadcastIdentifier = broadcastIdentifier;
    }

    public void create(@NonNull Context context, CloseableLayout closeableLayout) {
        mCloseableLayout = closeableLayout;
        mCloseableLayout.setCloseAlwaysInteractable(false);
        mCloseableLayout.setCloseVisible(false);

        addRadialCountdownWidget(context, View.INVISIBLE);
        mRadialCountdownWidget.calibrateAndMakeVisible(mShowCloseButtonDelay);
        mIsCalibrationDone = true;

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mCountdownRunnable = new RewardedMraidCountdownRunnable(this, mainHandler);
    }

    public void pause() {
        stopRunnables();
    }

    @Override
    public void resume() {
        startRunnables();
    }

    @Override
    public void destroy() {
        stopRunnables();
    }

    @Override
    protected void handleCustomClose(boolean useCustomClose) {
    }

    @Override
    protected void handleClose() {
        if (mShowCloseButtonEventFired) {
            super.handleClose();
        }
    }

    public boolean backButtonEnabled() {
        return mShowCloseButtonEventFired;
    }

    public boolean isPlayableCloseable() {
        return !mShowCloseButtonEventFired && mCurrentElapsedTimeMillis >= mShowCloseButtonDelay;
    }

    public void showPlayableCloseButton() {
        mShowCloseButtonEventFired = true;

        mRadialCountdownWidget.setVisibility(View.GONE);
        mCloseableLayout.setCloseVisible(true);

        if (!mIsRewarded) {
            broadcastAction(getContext(), mBroadcastIdentifier, ACTION_REWARDED_PLAYABLE_COMPLETE);
            mIsRewarded = true;
        }
    }

    public void updateCountdown(int currentElapsedTimeMillis) {
        mCurrentElapsedTimeMillis = currentElapsedTimeMillis;
        if (mIsCalibrationDone) {
            mRadialCountdownWidget.updateCountdownProgress(mShowCloseButtonDelay,
                    mCurrentElapsedTimeMillis);
        }
    }

    private void startRunnables() {
        mCountdownRunnable.startRepeating(PLAYABLE_COUNTDOWN_UPDATE_INTERVAL_MILLIS);
    }

    private void stopRunnables() {
        mCountdownRunnable.stop();
    }

    private void addRadialCountdownWidget(@NonNull final Context context, int initialVisibility) {
        mRadialCountdownWidget = new VastVideoRadialCountdownWidget(context);
        mRadialCountdownWidget.setVisibility(initialVisibility);

        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) mRadialCountdownWidget.getLayoutParams();
        final int widgetWidth = lp.width + lp.leftMargin + lp.rightMargin;
        final int widgetHeight = lp.height + lp.topMargin + lp.bottomMargin;

        FrameLayout.LayoutParams widgetLayoutParams =
                new FrameLayout.LayoutParams(widgetWidth, widgetHeight);
        widgetLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        mCloseableLayout.addView(mRadialCountdownWidget, widgetLayoutParams);
    }

    @Deprecated
    @VisibleForTesting
    public int getShowCloseButtonDelay() {
        return mShowCloseButtonDelay;
    }

    @Deprecated
    @VisibleForTesting
    public VastVideoRadialCountdownWidget getRadialCountdownWidget() {
        return mRadialCountdownWidget;
    }

    @Deprecated
    @VisibleForTesting
    public RewardedMraidCountdownRunnable getCountdownRunnable() {
        return mCountdownRunnable;
    }

    @Deprecated
    @VisibleForTesting
    public boolean isCalibrationDone() {
        return mIsCalibrationDone;
    }

    @Deprecated
    @VisibleForTesting
    public boolean isShowCloseButtonEventFired() {
        return mShowCloseButtonEventFired;
    }

    @Deprecated
    @VisibleForTesting
    public boolean isRewarded() {
        return mIsRewarded;
    }
}
