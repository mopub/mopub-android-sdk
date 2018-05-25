package com.mopub.mraid;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.IntentActions;
import com.mopub.mobileads.BaseBroadcastReceiver;

/**
 * Handles the rewarded playable complete broadcast.
 */
public class RewardedPlayableBroadcastReceiver extends BaseBroadcastReceiver {

    private static IntentFilter sIntentFilter;

    @Nullable
    private RewardedMraidInterstitial.RewardedMraidInterstitialListener mRewardedMraidListener;

    public RewardedPlayableBroadcastReceiver(
            @Nullable RewardedMraidInterstitial.RewardedMraidInterstitialListener rewardedVideoListener,
            final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mRewardedMraidListener = rewardedVideoListener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(IntentActions.ACTION_REWARDED_PLAYABLE_COMPLETE);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        if (mRewardedMraidListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (IntentActions.ACTION_REWARDED_PLAYABLE_COMPLETE.equals(action)) {
            mRewardedMraidListener.onMraidComplete();
            unregister(this);
        }
    }
}
