package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.IntentActions;
import com.mopub.common.Preconditions;

public class RewardedVideoBroadcastReceiver extends BaseBroadcastReceiver {

    private static IntentFilter sIntentFilter;

    @Nullable
    private RewardedVastVideoInterstitial.RewardedVideoInterstitialListener mRewardedVideoListener;

    public RewardedVideoBroadcastReceiver(
            @Nullable RewardedVastVideoInterstitial.RewardedVideoInterstitialListener rewardedVideoListener,
            final long broadcastIdentifier) {
        super(broadcastIdentifier);
        mRewardedVideoListener = rewardedVideoListener;
        getIntentFilter();
    }

    @NonNull
    public IntentFilter getIntentFilter() {
        if (sIntentFilter == null) {
            sIntentFilter = new IntentFilter();
            sIntentFilter.addAction(IntentActions.ACTION_REWARDED_VIDEO_COMPLETE);
        }
        return sIntentFilter;
    }

    @Override
    public void onReceive(@NonNull final Context context, @NonNull final Intent intent) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(intent);

        if (mRewardedVideoListener == null) {
            return;
        }

        if (!shouldConsumeBroadcast(intent)) {
            return;
        }

        final String action = intent.getAction();
        if (IntentActions.ACTION_REWARDED_VIDEO_COMPLETE.equals(action)) {
            mRewardedVideoListener.onVideoComplete();
            unregister(this);
        }
    }
}
