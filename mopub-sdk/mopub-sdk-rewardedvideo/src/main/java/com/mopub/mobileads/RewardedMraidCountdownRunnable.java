package com.mopub.mobileads;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.mopub.common.Preconditions;
import com.mopub.mraid.RewardedMraidController;

/**
 * A runnable that is used to update a {@link RewardedMraidController}'s countdown display according
 * to rules contained in the {@link RewardedMraidController}
 */
public class RewardedMraidCountdownRunnable extends RepeatingHandlerRunnable {
    @NonNull private final RewardedMraidController mRewardedMraidController;
    private int mCurrentElapsedTimeMillis;

    public RewardedMraidCountdownRunnable(@NonNull RewardedMraidController rewardedMraidController,
            @NonNull Handler handler) {
        super(handler);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(rewardedMraidController);

        mRewardedMraidController = rewardedMraidController;
    }

    @Override
    public void doWork() {
        mCurrentElapsedTimeMillis += mUpdateIntervalMillis;
        mRewardedMraidController.updateCountdown(mCurrentElapsedTimeMillis);

        if (mRewardedMraidController.isPlayableCloseable()) {
            mRewardedMraidController.showPlayableCloseButton();
        }
    }

    @Deprecated
    @VisibleForTesting
    int getCurrentElapsedTimeMillis() {
        return mCurrentElapsedTimeMillis;
    }
}
